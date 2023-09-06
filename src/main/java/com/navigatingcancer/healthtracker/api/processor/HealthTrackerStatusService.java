package com.navigatingcancer.healthtracker.api.processor;

import com.navigatingcancer.healthtracker.api.data.auth.Identity;
import com.navigatingcancer.healthtracker.api.data.model.*;
import com.navigatingcancer.healthtracker.api.data.model.survey.SurveyPayload;
import com.navigatingcancer.healthtracker.api.data.repo.CheckInRepository;
import com.navigatingcancer.healthtracker.api.data.repo.EnrollmentRepository;
import com.navigatingcancer.healthtracker.api.data.repo.HealthTrackerStatusRepository;
import com.navigatingcancer.healthtracker.api.data.repo.proReview.ProReviewRepository;
import com.navigatingcancer.healthtracker.api.data.service.EnrollmentService;
import com.navigatingcancer.healthtracker.api.data.service.ProReviewService;
import com.navigatingcancer.healthtracker.api.data.service.impl.PatientRecordService;
import com.navigatingcancer.healthtracker.api.events.HealthTrackerEventsPublisher;
import com.navigatingcancer.healthtracker.api.metrics.HealthTrackerCounterMetric;
import com.navigatingcancer.healthtracker.api.metrics.MetersService;
import com.navigatingcancer.healthtracker.api.processor.model.HealthTracker;
import com.navigatingcancer.healthtracker.api.processor.model.HealthTrackerStatusCommand;
import com.navigatingcancer.healthtracker.api.processor.model.ProFormatManager;
import com.navigatingcancer.healthtracker.api.processor.model.SurveyPayloadParser;
import com.navigatingcancer.healthtracker.api.processor.model.TriagePayload;
import com.navigatingcancer.healthtracker.api.rest.QueryParameters.EnrollmentQuery;
import com.navigatingcancer.healthtracker.api.rest.exception.UnknownEnrollmentException;
import com.navigatingcancer.json.utils.JsonUtils;
import com.navigatingcancer.patientinfo.PatientInfoClient;
import com.navigatingcancer.patientinfo.domain.PatientInfo;
import com.navigatingcancer.sqs.SqsHelper;
import com.navigatingcancer.sqs.SqsListener;
import com.navigatingcancer.sqs.SqsProducer;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
@SqsListener(queueName = HealthTrackerStatusService.HT_STATUS_QUEUE_NAME)
@Slf4j
public class HealthTrackerStatusService implements Consumer<HealthTrackerStatusCommand> {

  @Value(value = "${triage.saga.enabled:false}")
  private boolean triageSagaEnabled = false;

  static final String HT_STATUS_QUEUE_NAME = "${ht-status-queue}";
  public static final String API_STATUS_CHANGE_REASON = "HT status change with API call";
  public static final String TRIAGE_STATUS_CHANGE_REASON = "Triage status change";
  public static final String TRIAGE_MARKED_AS_ERROR_REASON = "Triage marked as error";

  @Autowired private ProFormatManager proFormatManager;
  @Autowired private SurveyPayloadParser surveyPayloadParser;
  @Autowired private EnrollmentRepository enrollmentRepository;
  @Autowired private HealthTrackerStatusRepository healthTrackerStatusRepository;
  // TODO: remove lazy annoation and refactor to avoid circular dependancy
  @Lazy @Autowired private EnrollmentService enrollmentService;
  @Autowired private CheckInRepository checkInRepository;
  @Autowired private PatientInfoClient patientInfoClient;
  @Autowired private DefaultDroolsService ruleEngine;
  @Autowired private SqsHelper sqsHelper;
  @Autowired private Identity identity;
  @Autowired private HealthTrackerEventsPublisher eventsPublisher;
  @Autowired private ProReviewRepository proReviewRepository;
  @Lazy @Autowired private ProReviewService proReviewService;

  private SqsProducer<HealthTrackerStatusCommand> sqsProducer;

  @Autowired RabbitTemplate rabbitTemplate;

  @Autowired PatientRecordService patientRecordService;

  @Autowired MetersService metersService;

  @PostConstruct
  public void init() {
    sqsProducer = sqsHelper.createProducer(HealthTrackerStatusCommand.class, HT_STATUS_QUEUE_NAME);
  }

  @Override
  public void accept(HealthTrackerStatusCommand command) {
    log.debug("got HealthTrackerStatusCommand {}", command);
    String enrollmentId = command.enrollmentId;
    SurveyPayload surveyPayload = command.surveyPayload;
    processStatus(enrollmentId, null, surveyPayload);
  }

  // TODO. Deprecate status paramater, few tests need to change
  public void processStatus(
      String enrollmentId, HealthTrackerStatus status, SurveyPayload surveyPayload) {

    Enrollment enrollment = enrollmentRepository.findById(enrollmentId).get();
    List<CheckIn> checkIns =
        checkInRepository.findByStatusNotAndEnrollmentIdOrderByScheduleDateDesc(
            CheckInStatus.PENDING, enrollmentId);
    String completedBy = null;
    if (status == null) {
      status = getOrCreateNewStatus(enrollment);
    }
    // Save current category (status). This is used to check if we need to act on a status change
    HealthTrackerStatusCategory originalStatus = status.getCategory();
    status.setSurveyPayload(surveyPayload);
    HealthTracker healthTracker =
        new HealthTracker(enrollment, checkIns, status, proFormatManager, surveyPayload);
    if (healthTracker.getPatientInfo() == null) {
      log.error(
          "Unable to process payload without patient info. id: {} payload: {}",
          enrollmentId,
          surveyPayload);
      return;
    }

    if (enrollment.getStatus() == EnrollmentStatus.PAUSED) {
      status.setCategory(HealthTrackerStatusCategory.PENDING);
    } else if (enrollment.getStatus() == EnrollmentStatus.COMPLETED
        || enrollment.getStatus() == EnrollmentStatus.STOPPED) {
      status.setCategory(HealthTrackerStatusCategory.COMPLETED);
      if (status.getCompletedAt() == null) {
        status.setCompletedAt(LocalDate.now());
      }
    } else {
      boolean wasAutoClosed = status.getEndCurrentCycle();
      status = ruleEngine.process(healthTracker, surveyPayload != null);

      ProReview proReview = createNewProReview(enrollment, checkIns, surveyPayload, status);
      status.setProReviewId(proReview.getId());

      if (!wasAutoClosed && status.getEndCurrentCycle()) {
        // auto-close kicked in
        // auto-close is caused by completed symptoms checkin - find it
        Optional<CheckIn> cin =
            checkIns.stream()
                .filter(
                    ci ->
                        ci.getCheckInType() == CheckInType.SYMPTOM
                            && ci.getStatus() == CheckInStatus.COMPLETED)
                .findFirst();
        if (!cin.isPresent()) {
          log.error("failed to find last completed symptoms checkin {}", checkIns);
          eventsPublisher.publishCycleAutoClosed(enrollment, null);
        } else {
          eventsPublisher.publishCycleAutoClosed(enrollment, cin.get());
        }
      }
      status.setAdherencePercent(healthTracker.getCheckInAggregator().adherencePercent());
      log.debug("calling getSymptomDetails with ht status : {}", status);
      status.setSymptomDetails(healthTracker.getSymptomDetailsAggregator().getSymptomDetails());
      log.debug("getSymptomDetails computed : {}", status.getSymptomDetails());
    }

    status.setSurveyPayload(surveyPayload);
    boolean declineACall =
        surveyPayload != null
            && surveyPayload.getSymptoms() != null
            && surveyPayload.getSymptoms().size() > 0
            && surveyPayload.getSymptoms().get(0).isDeclineACall();

    String declineACallComment =
        declineACall ? surveyPayload.getSymptoms().get(0).getDeclineACallComment() : "";
    status.setDeclineACall(declineACall);
    status.setDeclineACallComment(declineACallComment);
    status.setMedication(enrollment.getMedication());

    if (isPatientReportedCheckIn(surveyPayload)) {
      if (checkIns != null && checkIns.size() > 0) {
        // Pick any one checkIn out of multiple since they will be submitted by the patient at the
        // same time.
        CheckIn checkIn = checkIns.get(0);
        if (checkIn != null) {
          status.setLastPatientCheckInAt(checkIn.getUpdatedDate());
          completedBy = checkIn.getCompletedBy();
        }
      }
    }

    TriagePayload triagePayload = TriagePayload.createTriageIfNeeded(status, enrollment);
    if (triagePayload != null && originalStatus != HealthTrackerStatusCategory.TRIAGE) {
      if (triageSagaEnabled) {
        status.setCategory(HealthTrackerStatusCategory.ACTION_NEEDED);
        log.info(
            "triage saga enabled, setting  HT status to action needed until GC ack arrives {}",
            status);
      }
    }

    // Find last completed or missed checkin ids and save in the status
    status.setLastCompletedCheckInIds(getLastCheckins(checkIns));

    persistState(status, healthTracker);
    eventsPublisher.publishStatusChangeByRules(
        enrollment, originalStatus, status.getCategory(), surveyPayload);

    if (triagePayload != null) {
      log.debug("sending enrollment {} triage payload {}", enrollment.getId(), triagePayload);
      sendTriagePayloadViaRabbit(triagePayload);
      eventsPublisher.publishTriageTicketCreated(
          enrollment.getId(), enrollment.getClinicId(), enrollment.getPatientId(), null);
      metersService.incrementCounter(
          enrollment.getClinicId(), HealthTrackerCounterMetric.TRIAGE_TICKET_SUBMITTED);
    }

    if (isPatientReportedCheckIn(surveyPayload)) {
      // if there was actual patient report, send it over
      patientRecordService.publishProData(enrollment, status, completedBy);
    } else {
      // if this is just status check, report missed checkins only
      patientRecordService.publishMissedCheckIn(enrollment, status.getMissedCheckIns());
    }
  }

  private boolean isPatientReportedCheckIn(SurveyPayload surveyPayload) {
    return surveyPayload != null;
  }

  private void persistState(HealthTrackerStatus status, HealthTracker ht) {
    healthTrackerStatusRepository.save(status);
  }

  private void sendTriagePayloadViaRabbit(TriagePayload triagePayload) {
    if (triagePayload != null) {
      // feed a rabbit here
      log.info("Pushing triage request in JMS queue {}", triagePayload);
      rabbitTemplate.convertAndSend(
          "app/health_tracker/patient/create_incident", JsonUtils.toJson(triagePayload));
    }
  }

  public List<HealthTrackerStatus> getOrCreateNewStatus(
      List<Long> clinicId, List<Long> locationIds, List<Long> patientIds) {
    return healthTrackerStatusRepository.findStatuses(clinicId, locationIds, patientIds);
  }

  public HealthTrackerStatus getById(String enrollmentId) {
    return healthTrackerStatusRepository.getById(enrollmentId);
  }

  public HealthTrackerStatus getOrCreateNewStatus(Enrollment enrollment) {

    HealthTrackerStatus status = healthTrackerStatusRepository.getById(enrollment.getId());

    if (status != null) {
      return status;
    }

    status = new HealthTrackerStatus();
    status.setCategory(HealthTrackerStatusCategory.PENDING);
    status.setId(enrollment.getId());
    status.setPatientInfo(getPatientInfo(enrollment.getClinicId(), enrollment.getPatientId()));
    status.setLocationId(enrollment.getLocationId());
    status.setClinicId(enrollment.getClinicId());
    status.setFollowsProCtcaeFormat(proFormatManager.followsCtcaeStandard(enrollment));
    status.setTherapyTypes(enrollment.getTherapyTypes());

    healthTrackerStatusRepository.save(status);

    return status;
  }

  protected static Set<String> getLastCheckins(List<CheckIn> checkIns) {
    if (checkIns != null && !checkIns.isEmpty()) {
      // Find last completed or missed checkin ids and save in the status
      final LocalDate lastDate = checkIns.get(0).getScheduleDate();
      Set<String> ciIds =
          checkIns.stream()
              .filter(ci -> ci.getScheduleDate().equals(lastDate))
              .map(ci -> ci.getId())
              .collect(Collectors.toSet());
      return ciIds;
    }
    return Collections.emptySet();
  }

  @SuppressWarnings("unchecked")
  @RabbitListener(queues = "gc-ht2")
  public void triageTicketListener(Message message) {
    log.info("Processing triage status update {}", message);
    byte[] bytes = message.getBody();
    InputStream is = new ByteArrayInputStream(bytes);
    Map<String, Object> json = JsonUtils.fromJson(is, Map.class);
    Long clinicId = Long.parseLong(json.get("clinic_id").toString());
    Long patientId = Long.parseLong(json.get("patient_id").toString());
    String updatedByName = (String) json.get("updated_by_name");
    Object updatedByIdObj = json.get("updated_by_security_identity_id");
    Boolean markedAsError = (Boolean) json.get("mark_as_error");
    String status = (String) json.get("status");
    if (clinicId == null || patientId == null) {
      log.error("clinic and patient ID must be defined {}", json);
      return;
    }

    String updatedById;
    if (updatedByIdObj == null) {
      updatedById = PatientRecordService.HEALTH_TRACKER_NAME;
    } else {
      updatedById = updatedByIdObj.toString();
    }

    log.debug("looking for enrollment with clinic_id {} and patientId {}", clinicId, patientId);
    EnrollmentQuery query = new EnrollmentQuery();
    query.setClinicId(Arrays.asList(clinicId));
    query.setPatientId(Arrays.asList(patientId));
    query.setStatus(List.of(EnrollmentStatus.ACTIVE));
    List<Enrollment> enrollments = enrollmentRepository.findEnrollments(query);
    log.debug("found {} enrollments", enrollments.size());
    for (Enrollment enrollment : enrollments) {
      processEnrollment(enrollment, updatedById, updatedByName, status, markedAsError);
    }
  }

  private void processEnrollment(
      Enrollment enrollment,
      String updatedById,
      String updatedByName,
      String status,
      Boolean markedAsError) {
    HealthTrackerStatus healthTrackerStatus =
        healthTrackerStatusRepository.getById(enrollment.getId());

    if (healthTrackerStatus == null) {
      log.error("failed to find ht_status for enrollment {}", enrollment);
      return;
    }

    healthTrackerStatus.setCategory(HealthTrackerStatusCategory.valueOf(status.toUpperCase()));

    metersService.incrementCounter(
        enrollment.getClinicId(), HealthTrackerCounterMetric.TRIAGE_TICKET_CLOSED);

    healthTrackerStatus.setActionPerformedBy(updatedByName);
    healthTrackerStatusRepository.save(healthTrackerStatus);
    if (healthTrackerStatus.getProReviewId() != null
        && ObjectId.isValid(healthTrackerStatus.getProReviewId())) {
      proReviewService.markEhrDelivered(healthTrackerStatus.getProReviewId(), updatedByName, null);
    }

    if (markedAsError != null && markedAsError) {
      enrollmentService.appendEventsLog(
          healthTrackerStatus.getId(),
          EnrollmentStatus.STATUS_CHANGE,
          TRIAGE_MARKED_AS_ERROR_REASON,
          null,
          updatedById,
          updatedByName);

      eventsPublisher.publishTriageTicketMarkedAsError(
          enrollment.getId(),
          healthTrackerStatus.getCategory(),
          enrollment.getClinicId(),
          enrollment.getPatientId(),
          updatedByName);
    } else {
      enrollmentService.appendEventsLog(
          healthTrackerStatus.getId(),
          EnrollmentStatus.STATUS_CHANGE,
          TRIAGE_STATUS_CHANGE_REASON,
          null,
          updatedById,
          updatedByName);

      eventsPublisher.publishTriageTicketClosed(
          enrollment.getId(),
          healthTrackerStatus.getCategory(),
          enrollment.getClinicId(),
          enrollment.getPatientId(),
          updatedByName);
    }
  }

  public void push(String enrollmentId, SurveyPayload surveyPayload) {
    HealthTrackerStatusCommand htStatus = new HealthTrackerStatusCommand();
    htStatus.enrollmentId = enrollmentId;
    htStatus.surveyPayload = surveyPayload;
    if (htStatus.enrollmentId == null) throw new Error("null enrollmentId");
    sqsProducer.send(htStatus);
  }

  private PatientInfo getPatientInfo(Long clinicId, Long patientId) {
    List<PatientInfo> patients = patientInfoClient.getApi().getPatients(clinicId, patientId);
    return patients != null && !patients.isEmpty() ? patients.get(0) : null;
  }

  public HealthTrackerStatus setCategory(
      String id, HealthTrackerStatusCategory cat, List<String> checkinIds) {
    HealthTrackerStatus status = healthTrackerStatusRepository.getById(id);

    if (status == null) {
      throw new UnknownEnrollmentException("not found enrollment status for id " + id);
    }

    status.setLastStatusChangedByClinicianAt(new Date());

    HealthTrackerStatus updatedStatus = healthTrackerStatusRepository.updateCategory(id, cat);

    eventsPublisher.publishStatusChange(
        id,
        status.getClinicId(),
        status.getPatientInfo().getId(),
        "HT status change with API call",
        status.getCategory(),
        cat,
        checkinIds,
        identity);

    patientRecordService.publishEnrollmentStatusChange(status, status.getCategory(), cat, identity);

    // TODO. Consider moving to async events listener
    enrollmentService.appendEventsLog(
        id,
        EnrollmentStatus.STATUS_CHANGE,
        "HT status change with API call",
        "from "
            + HealthTrackerEventsPublisher.catName(status.getCategory())
            + " to "
            + HealthTrackerEventsPublisher.catName(cat),
        identity.getClinicianId(),
        identity.getClinicianName());

    return updatedStatus;
  }

  private ProReview createNewProReview(
      Enrollment enrollment,
      List<CheckIn> checkIns,
      SurveyPayload surveyPayload,
      HealthTrackerStatus status) {
    List<String> checkInIds = checkIns.stream().map(ci -> ci.getId()).collect(Collectors.toList());

    List<Adherence> oralAdherence =
        surveyPayloadParser.parseOralAdherence(enrollment, surveyPayload);
    List<SideEffect> sideEffects = surveyPayloadParser.parseSideEffects(enrollment, surveyPayload);
    ProReview proReview =
        new ProReview(
            enrollment.getClinicId(),
            enrollment.getId(),
            checkInIds,
            surveyPayload,
            sideEffects,
            oralAdherence,
            status);
    proReviewRepository.save(proReview);

    return proReview;
  }
}
