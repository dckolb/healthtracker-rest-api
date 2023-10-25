package com.navigatingcancer.healthtracker.api.data.service.impl;

import com.navigatingcancer.healthtracker.api.data.auth.AuthInterceptor;
import com.navigatingcancer.healthtracker.api.data.auth.Identity;
import com.navigatingcancer.healthtracker.api.data.migrate.CheckInScheduleMigrator;
import com.navigatingcancer.healthtracker.api.data.model.*;
import com.navigatingcancer.healthtracker.api.data.model.surveyConfig.ClinicConfig;
import com.navigatingcancer.healthtracker.api.data.model.surveyConfig.ProgramConfig;
import com.navigatingcancer.healthtracker.api.data.repo.*;
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
import java.util.*;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EnrollmentServiceImpl implements EnrollmentService {

  @Autowired private EnrollmentRepository enrollmentRepository;

  @Autowired private HealthTrackerStatusRepository statusRepository;

  @Lazy @Autowired private SchedulingServiceImpl schedulingService;

  @Autowired private NotificationService notificationService;

  @Autowired private PxTokenService pxTokenService;

  @Lazy @Autowired private HealthTrackerStatusService healthTrackerStatusService;

  @Autowired private HealthTrackerStatusRepository healthTrackerStatusRepository;

  @Autowired private PatientRecordService patientRecordService;

  @Autowired private CheckInRepository checkInRepository;

  @Autowired private SurveyConfigService surveyConfigService;

  @Autowired private ConsentService consentService;

  @Autowired private HealthTrackerEventsPublisher eventsPublisher;

  @Autowired private MetersService metersService;

  @Autowired private CheckInScheduleMigrator checkInScheduleMigrator;

  private static class InvalidStatusUpdateException extends Exception {
    public InvalidStatusUpdateException(String errorMessage) {
      super(errorMessage);
    }
  }

  @Autowired private Identity identity;

  /**
   * @deprecated This method is used to set a transient program id on in-memory enrollments Use
   *     explicitly configured survey instances instead.
   * @param enrollments
   * @return
   */
  @Deprecated
  @Override
  public List<Enrollment> setProgramIds(List<Enrollment> enrollments) {
    if (enrollments == null || enrollments.isEmpty()) {
      return enrollments;
    }

    // get config and process consent, if necessary
    ClinicConfig clinicConfig =
        this.surveyConfigService.getClinicConfig(enrollments.get(0).getClinicId());

    var programConfigs = this.surveyConfigService.getProgramConfigs(clinicConfig);

    var clinicHasProCtcaeProgram =
        programConfigs.stream()
            .anyMatch(p -> ProgramType.PRO_CTCAE.getProgramName().equalsIgnoreCase(p.getType()));

    for (Enrollment e : enrollments) {
      // check if the clinic is setup for pro-ctcae
      if (clinicHasProCtcaeProgram
          && e.getTherapyTypes() != null
          && e.getTherapyTypes().contains(TherapyType.IV)) {
        e.setProgramId(ProgramConfig.getProgramId(programConfigs, ProgramType.PRO_CTCAE));
      } else {
        e.setProgramId(ProgramConfig.getProgramId(programConfigs, ProgramType.HEALTH_TRACKER));
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
    Enrollment savedEnrollment = scheduleFirstTime(enrollment);

    // publish notification
    eventsPublisher.publishEnrollmentCreated(savedEnrollment, identity);

    try {
      // TODO. Make it event listener action?
      patientRecordService.publishEnrollmentCreated(savedEnrollment, identity);
    } catch (Exception e) {
      log.error("Unable to publish to RabbitMQ : ", e);
      // FIXME better way to handle this error ?
      // enrollmentRepository.delete(persisted);
      throw e;
    }

    metersService.incrementCounter(
        savedEnrollment.getClinicId(), HealthTrackerCounterMetric.ENROLLMENT_CREATED);

    return savedEnrollment;
  }

  @Override
  public Enrollment updateEnrollment(Enrollment newEnrollment) {
    String id = newEnrollment.getId();
    Enrollment current = enrollmentRepository.findById(id).get();
    retainImmutableFields(current, newEnrollment);
    if (!current.getSchedules().isEmpty()) {
      // If the schedule has started then certain fields are immutable
      if (current.hasStarted()) {
        validateScheduleUpdates(current, newEnrollment);
      }

      retainScheduleIds(current, newEnrollment);
      this.enrollmentRepository.save(newEnrollment);
      updateSchedule(newEnrollment);
    } else if (!newEnrollment.getSchedules().isEmpty()) {
      scheduleFirstTime(newEnrollment);
    } else {
      this.enrollmentRepository.save(newEnrollment);
    }

    List<String> diffList = current.diffDescr(newEnrollment);
    if (diffList.size() > 0) {
      eventsPublisher.publishEnrollmentUpdated(newEnrollment, diffList, identity);
    }

    patientRecordService.publishEnrollmentUpdated(newEnrollment, identity);

    return newEnrollment;
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

    if (!enrollment.isManualCollect()) {
      notificationService.sendNotification(
          id, enrollment, event, NotificationService.Category.STATUS_CHANGED);
    } else {
      log.info("Not sending notification for clinic collect enrollment {} {}", id, enrollment);
    }

    patientRecordService.publishEnrollmentStatusUpdated(enrollment, event, reason, note, identity);

    if (status == EnrollmentStatus.STOPPED) {
      eventsPublisher.publishEnrollmentStopped(
          enrollment, reason, note, identity, statusChangeTime);
    }

    if (status == EnrollmentStatus.STOPPED) {
      // If there are pending checkins - stop them
      checkInRepository.stopCheckins(id);
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
    return enrollmentRepository.appendStatusLog(id, logEntry);
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

  private Enrollment scheduleFirstTime(Enrollment enrollment) {
    ProgramConfig programConfig =
        this.surveyConfigService.getProgramConfig(enrollment.getProgramId());
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

      for (var schedule : enrollment.getSchedules()) {
        checkInScheduleMigrator.migrateIfNecessary(enrollment, schedule);
      }
      enrollment = enrollmentRepository.findById(enrollment.getId()).orElseThrow();

      // persist status prior to scheduling service call to prevent race condition
      status = healthTrackerStatusService.getOrCreateNewStatus(enrollment);

      if (!enrollment.getSchedules().isEmpty()) {
        schedulingService.schedule(enrollment, true);

        if (!enrollment.isManualCollect()) {
          // Send first time enrollment notification
          notificationService.sendNotification(
              enrollment.getId(),
              enrollment,
              NotificationService.Event.ENROLLED,
              NotificationService.Category.FIRST_ENROLLMENT);
        } else {
          log.info(
              "Not sending notification for clinic collect enrollment {} {}",
              enrollment.getId(),
              enrollment);
        }
      }

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

  private void validateScheduleUpdates(Enrollment current, Enrollment newEnrollment) {
    // Schedule size should match
    if (current.getSchedules() == null
        || newEnrollment.getSchedules() == null
        || (current.getSchedules().size() != newEnrollment.getSchedules().size())) {
      throw new InvalidParameterException("check in schedule count does not match");
    }

    // Ensure types and medication pair are unique
    current
        .getSchedules()
        .forEach(
            ps -> {
              long matchingSchedules =
                  newEnrollment.getSchedules().stream()
                      .filter(es -> es.matchesTypeAndMedication(ps))
                      .count();

              if (matchingSchedules > 1L) {
                log.debug(" found multiple matching schedules for resultType %s med %s");
                throw new InvalidParameterException("check in schedules do not match");
              }

              // disallows updating a schedule's medication once it has started
              // FIXME: there is no medication being set on schedules.
              if (matchingSchedules == 0L) {
                throw new InvalidParameterException(
                    String.format(
                        "cannot remove enrollment resultType %s for medication %s",
                        ps.getCheckInType().toString(), ps.getMedication()));
              }
            });
  }

  private void retainScheduleIds(Enrollment persisted, Enrollment enrollment) {
    enrollment
        .getSchedules()
        .forEach(
            es -> {
              // find persisted schedule matching incoming schedule
              var matchingPersistedSchedules =
                  persisted.getSchedules().stream()
                      .filter(
                          ps -> {
                            if (es.getId() != null) {
                              return Objects.equals(ps.getId(), es.getId());
                            }

                            // TODO: don't use check in type here, use schedule id only when
                            // expected in API
                            return es.getCheckInType() == ps.getCheckInType();
                          })
                      .toList();

              if (matchingPersistedSchedules.size() > 1) {
                throw new InvalidParameterException("check in schedules do not match");
              }

              if (matchingPersistedSchedules.isEmpty()) {
                checkInScheduleMigrator.setScheduleDefaults(enrollment, es);
                return;
              }

              var persistedSchedule = matchingPersistedSchedules.get(0);
              es.setId(persistedSchedule.getId());
              checkInScheduleMigrator.updateIfNecessary(enrollment, persistedSchedule, es);
            });
  }

  private void retainImmutableFields(Enrollment persisted, Enrollment enrollment) {
    enrollment.setCreatedDate(persisted.getCreatedDate());
    enrollment.setCreatedBy(persisted.getCreatedBy());
    enrollment.setUrl(persisted.getUrl());
    enrollment.setStatusLogs(persisted.getStatusLogs());
    enrollment.setConsentRequestId(persisted.getConsentRequestId());
    enrollment.setProgramId(persisted.getProgramId());
  }
}
