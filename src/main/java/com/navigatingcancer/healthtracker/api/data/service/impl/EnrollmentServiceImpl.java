package com.navigatingcancer.healthtracker.api.data.service.impl;

import com.navigatingcancer.healthtracker.api.data.auth.AuthInterceptor;
import com.navigatingcancer.healthtracker.api.data.auth.Identity;
import com.navigatingcancer.healthtracker.api.data.model.CheckInSchedule;
import com.navigatingcancer.healthtracker.api.data.model.Enrollment;
import com.navigatingcancer.healthtracker.api.data.model.EnrollmentStatus;
import com.navigatingcancer.healthtracker.api.data.model.EnrollmentStatusLog;
import com.navigatingcancer.healthtracker.api.data.model.HealthTrackerStatus;
import com.navigatingcancer.healthtracker.api.data.model.HealthTrackerStatusCategory;
import com.navigatingcancer.healthtracker.api.data.model.TherapyType;
import com.navigatingcancer.healthtracker.api.data.model.surveyConfig.ClinicConfig;
import com.navigatingcancer.healthtracker.api.data.model.surveyConfig.ProgramConfig;
import com.navigatingcancer.healthtracker.api.data.repo.CustomCheckInRepository;
import com.navigatingcancer.healthtracker.api.data.repo.CustomEnrollmentRepositoryImpl;
import com.navigatingcancer.healthtracker.api.data.repo.EnrollmentRepository;
import com.navigatingcancer.healthtracker.api.data.repo.HealthTrackerStatusRepository;
import com.navigatingcancer.healthtracker.api.data.service.ConsentService;
import com.navigatingcancer.healthtracker.api.data.service.EnrollmentService;
import com.navigatingcancer.healthtracker.api.data.service.SurveyConfigService;
import com.navigatingcancer.healthtracker.api.events.HealthTrackerEventsPublisher;
import com.navigatingcancer.healthtracker.api.metrics.HealthTrackerCounterMetric;
import com.navigatingcancer.healthtracker.api.metrics.MetersService;
import com.navigatingcancer.healthtracker.api.processor.HealthTrackerStatusService;
import com.navigatingcancer.healthtracker.api.rest.QueryParameters.EnrollmentQuery;
import com.navigatingcancer.healthtracker.api.rest.exception.DuplicateEnrollmentException;
import com.navigatingcancer.healthtracker.api.rest.exception.UnknownEnrollmentException;
import com.navigatingcancer.healthtracker.api.rest.representation.EnrollmentIdentifiers;
import java.security.InvalidParameterException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EnrollmentServiceImpl implements EnrollmentService {

  @Autowired private EnrollmentRepository enrollmentRepository;

  @Autowired private CustomEnrollmentRepositoryImpl customEnrollmentRepository;

  @Autowired private HealthTrackerStatusRepository statusRepository;

  @Lazy @Autowired private SchedulingServiceImpl schedulingService;

  @Autowired private NotificationService notificationService;

  @Autowired private PxTokenService pxTokenService;

  @Lazy @Autowired private HealthTrackerStatusService healthTrackerStatusService;

  @Autowired private HealthTrackerStatusRepository healthTrackerStatusRepository;

  @Autowired private PatientRecordService patientRecordService;

  @Autowired private CustomCheckInRepository customCheckInRepository;

  @Autowired private SurveyConfigService surveyConfigService;

  @Autowired private ConsentService consentService;

  @Autowired private HealthTrackerEventsPublisher eventsPublisher;

  @Autowired private MetersService metersService;

  private static class InvalidStatusUpdateException extends Exception {
    public InvalidStatusUpdateException(String errorMessage) {
      super(errorMessage);
    }
  }

  @Autowired private Identity identity;

  @Override
  public List<Enrollment> setProgramIds(List<Enrollment> enrollments) {
    if (enrollments == null || enrollments.isEmpty()) {
      return enrollments;
    }

    // get config and process consent, if necessary
    ClinicConfig clinicConfig =
        this.surveyConfigService.getClinicConfig(enrollments.get(0).getClinicId());
    Collection<String> programIds = new ArrayList<>();
    if (clinicConfig != null && clinicConfig.getPrograms() != null) {
      programIds.addAll(clinicConfig.getPrograms().values());
    }

    List<ProgramConfig> programConfigs = this.surveyConfigService.getProgramConfigs(programIds);

    for (Enrollment e : enrollments) {
      // check if the clinic is setup for pro-ctcae
      if (clinicConfig != null && clinicConfig.getPrograms().containsKey("PRO-CTCAE")) {
        if (e.getTherapyTypes() != null && e.getTherapyTypes().contains(TherapyType.IV)) {
          e.setProgramId(this.surveyConfigService.getProgramId(programConfigs, "pro-ctcae"));
          continue;
        } else {
          e.setProgramId(this.surveyConfigService.getProgramId(programConfigs, "healthtracker"));
        }
        continue;
      } else {
        // only other option at the moment is healthtracker
        e.setProgramId(this.surveyConfigService.getProgramId(programConfigs, "healthtracker"));
        continue;
      }
    }

    return enrollments;
  }

  @Override
  public Enrollment createEnrollment(Enrollment enrollment) {
    log.debug("EnrollmentService::createEnrollment");
    if (enrollmentRepository.activeEnrollmentExists(
        enrollment.getPatientId(), enrollment.getClinicId())) {
      throw new DuplicateEnrollmentException(
          "An active enrollment exists for this patient in this clinic.");
    }

    // statusLogs is null on initial create
    if (enrollment.getStatusLogs() == null) {
      enrollment.setStatusLogs(new ArrayList<>());
    }

    // get config and process consent, if necessary
    ProgramConfig programConfig =
        this.surveyConfigService.getProgramConfig(enrollment.getProgramId());

    enrollment
        .getStatusLogs()
        .add(
            new EnrollmentStatusLog(
                EnrollmentStatus.ACTIVE,
                null,
                null,
                Instant.now(),
                identity.getClinicianId(),
                identity.getClinicianName()));

    // HT-362 - send status AFTER saving to db so id isn't null
    Enrollment persisted = scheduleFirstTime(enrollment, programConfig);

    // publish notofication
    eventsPublisher.publishEnrollmentCreated(enrollment, identity);

    try {
      // TODO. Make it event listener action?
      patientRecordService.publishEnrollmentCreated(persisted, identity);
    } catch (Exception e) {
      log.error("Unable to publish to RabbitMQ : ", e);
      // FIXME better way to handle this error ?
      // enrollmentRepository.delete(persisted);
      throw e;
    }

    metersService.incrementCounter(
        persisted.getClinicId(), HealthTrackerCounterMetric.ENROLLMENT_CREATED);

    return persisted;
  }

  @Override
  public Enrollment updateEnrollment(Enrollment enrollment) {
    log.debug("EnrollmentService::updateEnrollment");
    String id = enrollment.getId();
    Enrollment persisted = enrollmentRepository.findById(id).get();
    enrollment.setCreatedDate(persisted.getCreatedDate());
    enrollment.setCreatedBy(persisted.getCreatedBy());
    enrollment.setUrl(persisted.getUrl());
    enrollment.setStatusLogs(persisted.getStatusLogs());
    enrollment.setConsentRequestId(persisted.getConsentRequestId());
    enrollment.setProgramId(persisted.getProgramId());

    // If the schedule has started then certain fields are immutable
    if (persisted.getStartDate().isBefore(LocalDate.now())
        || persisted.getStartDate().isEqual(LocalDate.now())) {
      // schedule size should match
      if (persisted.getSchedules() == null
          || enrollment.getSchedules() == null
          || (persisted.getSchedules().size() != enrollment.getSchedules().size())) {
        throw new InvalidParameterException("check in schedule count does not match");
      }

      for (CheckInSchedule p : persisted.getSchedules()) {

        CheckInSchedule schedule = null;

        for (CheckInSchedule e : enrollment.getSchedules()) {
          if (e.matchesTypeAndMedication(p)) {
            if (schedule != null) {
              log.debug(" found multiple matching schedules for resultType %s med %s");
              throw new InvalidParameterException("check in schedules do not match");
            }
            schedule = e;
          }
        }

        if (schedule == null) {
          throw new InvalidParameterException(
              String.format(
                  "cannot remove enrollment resultType %s for medication %s",
                  p.getCheckInType().toString(), p.getMedication()));
        }
      }
    }

    List<String> diffList = persisted.diffDescr(enrollment);
    if (diffList.size() > 0) {
      eventsPublisher.publishEnrollmentUpdated(enrollment, diffList, identity);
    }

    updateSchedule(enrollment);
    try {
      this.enrollmentRepository.save(enrollment);
    } catch (OptimisticLockingFailureException ex) {
      log.error("Failed to update. Concurrent update of the enrollment " + id, ex);
      throw new RuntimeException(
          "Changes to the underlying enrollment data detected. Has it been modified meanwhile? Please refresh enrollment and try again.");
    }
    patientRecordService.publishEnrollmentUpdated(enrollment, identity);

    return enrollment;
  }

  @Override
  public List<Enrollment> getEnrollmentsByIds(List<String> enrollmentIds) {
    log.debug("EnrollmentService::getEnrollmentsByIds");
    Iterable<Enrollment> enrollmentIterable = enrollmentRepository.findAllById(enrollmentIds);
    List<Enrollment> enrollmentList = new ArrayList<>();
    for (Enrollment enrollment : enrollmentIterable) {
      enrollmentList.add(enrollment);
    }
    return setProgramIds(enrollmentList);
  }

  @Override
  public Enrollment getEnrollment(String id) {
    log.debug("EnrollmentService::getEnrollment");
    List<Enrollment> enrollments = new ArrayList<>();
    Enrollment e = enrollmentRepository.findById(id).get();
    enrollments.add(e);
    setProgramIds(enrollments);
    return enrollments.get(0);
  }

  @Override
  public List<Enrollment> getEnrollments(EnrollmentQuery params) {
    log.debug("EnrollmentService::getEnrollments");
    return setProgramIds(enrollmentRepository.findEnrollments(params));
  }

  @Override
  public List<EnrollmentIdentifiers> getCurrentEnrollments(
      List<Long> clinicIds,
      List<Long> locationIds,
      List<Long> providerIds,
      Boolean isManualCollect) {
    log.debug("EnrollmentService::getCurrentEnrollments");
    return enrollmentRepository
        .getCurrentEnrollments(clinicIds, locationIds, providerIds, isManualCollect)
        .stream()
        .map(
            enrollment -> {
              return new EnrollmentIdentifiers(enrollment.getId(), enrollment.getPatientId());
            })
        .collect(Collectors.toList());
  }

  @Override
  public Enrollment changeStatus(String id, EnrollmentStatus status, String reason, String note)
      throws Exception {
    log.debug("EnrollmentService::changeStatus");
    final Enrollment enrollment = enrollmentRepository.findById(id).get();

    // do nothing if no changes
    if (enrollment.getStatus() == status) {
      return enrollment;
    }

    enrollment.setStatus(status);
    updateSchedule(enrollment);

    NotificationService.Event event = null;
    HealthTrackerStatusCategory category = null;
    HealthTrackerStatus htStatus = statusRepository.getById(id);
    // must handle updating ht status and schedule if needed
    switch (status) {
      case PAUSED:
        event = NotificationService.Event.PAUSED;
        break;
      case STOPPED:
        event = NotificationService.Event.STOPPED;
        category = HealthTrackerStatusCategory.COMPLETED;
        break;
      case ACTIVE:
        event = NotificationService.Event.RESUMED;
        break;
      default:
        throw new InvalidStatusUpdateException(
            String.format(
                "Invalid status update '%s' for enrollment id '%s' in status '%s'",
                status, enrollment.getId(), enrollment.getStatus()));
    }
    if (htStatus != null && category != null) {
      htStatus.setCategory(category);
      statusRepository.save(htStatus);
    }

    // FIXME we need to handle situations where one or more of these operations
    // fails !
    Instant statusChangeTime = Instant.now();
    enrollment
        .getStatusLogs()
        .add(
            new EnrollmentStatusLog(
                status,
                reason,
                note,
                statusChangeTime,
                identity.getClinicianId(),
                identity.getClinicianName()));
    enrollmentRepository.save(enrollment);
    notificationService.sendNotification(
        id, enrollment, event, NotificationService.Category.STATUS_CHANGED.toString());

    patientRecordService.publishEnrollmentStatusUpdated(enrollment, event, reason, note, identity);

    if (status == EnrollmentStatus.STOPPED) {
      eventsPublisher.publishEnrollmentStopped(
          enrollment, reason, note, identity, statusChangeTime);
    }

    if (status == EnrollmentStatus.STOPPED) {
      // If there are pending checkins - stop them
      customCheckInRepository.stopCheckins(id);
      // Report the number
      metersService.incrementCounter(
          enrollment.getClinicId(), HealthTrackerCounterMetric.ENROLLMENT_STOPPED);
    }

    return enrollment;
  }

  @Override
  public void resendConsentRequest(String id) throws Exception {
    Enrollment enrollment = this.enrollmentRepository.findById(id).orElse(null);
    if (enrollment == null) {
      throw new Exception("enrollment not found");
    }

    if (enrollment.getConsentRequestId() == null) {
      throw new Exception("no consent required");
    }
    // TODO: handle something other than docusign
    this.consentService.resendRequest(enrollment.getConsentRequestId());
  }

  private Enrollment scheduleFirstTime(Enrollment enrollment, ProgramConfig programConfig) {
    // retain reference in case of rollback need
    HealthTrackerStatus status = null;
    try {

      String url = pxTokenService.getUrl(enrollment);
      enrollment.setUrl(url);

      if (programConfig != null) {
        if (programConfig.getConsent() != null) {
          // handle consent
          enrollment.setConsentRequestId(
              this.consentService.processSurveyConsentForEnrollment(
                  programConfig.getConsent(), enrollment));
        }
      }

      enrollment = enrollmentRepository.save(enrollment);
      log.debug("enrollment is {}", enrollment);

      // persist status prior to scheduling service call to prevent race condition
      status = healthTrackerStatusService.getOrCreateNewStatus(enrollment);

      schedulingService.schedule(enrollment, true);

      // Send first time enrollment notification
      notificationService.sendNotification(
          enrollment.getId(),
          enrollment,
          NotificationService.Event.ENROLLED,
          NotificationService.Category.FIRST_ENROLLMENT.toString());

    } catch (Exception e) {
      log.error("Unable to create enrollment", e);
      if (enrollment.getId() != null) {
        try {
          enrollmentRepository.deleteById(enrollment.getId());
        } catch (Exception ex) {
          log.error("unable to delete enrollment ", ex);
        }
        if (status != null) {
          try {
            statusRepository.deleteById(enrollment.getId());
          } catch (Exception ex) {
            log.error("unable to delete htstatus", ex);
          }
        }
      }
      throw e;
    }

    return enrollment;
  }

  private void updateSchedule(Enrollment result) {
    schedulingService.schedule(result, false);
  }

  @Override
  public Enrollment appendEventsLog(
      String id,
      EnrollmentStatus status,
      String reason,
      String note,
      String clinicianId,
      String clinicianName) {
    EnrollmentStatusLog logEntry =
        new EnrollmentStatusLog(status, reason, note, Instant.now(), clinicianId, clinicianName);
    return customEnrollmentRepository.appendStatusLog(id, logEntry);
  }

  @Override
  public Enrollment completeEnrollment(Enrollment enrollment) {
    String eid = enrollment.getId();
    enrollmentRepository.setStatus(eid, EnrollmentStatus.COMPLETED);
    healthTrackerStatusRepository.updateCategory(eid, HealthTrackerStatusCategory.COMPLETED);
    metersService.incrementCounter(
        enrollment.getClinicId(), HealthTrackerCounterMetric.ENROLLMENT_COMPLETED);
    return appendEventsLog(
        eid,
        EnrollmentStatus.COMPLETED,
        "Enrollment completed",
        null,
        null,
        AuthInterceptor.HEALTH_TRACKER_NAME);
  }

  @Override
  public void rebuildSchedule(String id) {
    Optional<Enrollment> e = enrollmentRepository.findById(id);
    if (e.isPresent()) {
      schedulingService.schedule(e.get(), false);
    } else {
      throw new UnknownEnrollmentException("Unknown enrollment ID " + id);
    }
  }
}
