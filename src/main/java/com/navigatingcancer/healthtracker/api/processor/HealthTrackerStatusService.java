package com.navigatingcancer.healthtracker.api.processor;

import com.google.common.base.Preconditions;
import com.navigatingcancer.healthtracker.api.data.auth.Identity;
import com.navigatingcancer.healthtracker.api.data.client.PatientInfoServiceClient;
import com.navigatingcancer.healthtracker.api.data.model.*;
import com.navigatingcancer.healthtracker.api.data.model.patientInfo.PatientInfo;
import com.navigatingcancer.healthtracker.api.data.model.survey.SurveyPayload;
import com.navigatingcancer.healthtracker.api.data.repo.CheckInRepository;
import com.navigatingcancer.healthtracker.api.data.repo.EnrollmentRepository;
import com.navigatingcancer.healthtracker.api.data.repo.HealthTrackerStatusRepository;
import com.navigatingcancer.healthtracker.api.data.repo.proReview.ProReviewRepository;
import com.navigatingcancer.healthtracker.api.data.service.EnrollmentService;
import com.navigatingcancer.healthtracker.api.data.service.impl.PatientRecordService;
import com.navigatingcancer.healthtracker.api.events.HealthTrackerEventsPublisher;
import com.navigatingcancer.healthtracker.api.metrics.HealthTrackerCounterMetric;
import com.navigatingcancer.healthtracker.api.metrics.MetersService;
import com.navigatingcancer.healthtracker.api.processor.model.*;
import com.navigatingcancer.healthtracker.api.rest.exception.UnknownEnrollmentException;
import com.navigatingcancer.json.utils.JsonUtils;
import com.navigatingcancer.sqs.SqsHelper;
import com.navigatingcancer.sqs.SqsListener;
import com.navigatingcancer.sqs.SqsProducer;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Consumer;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
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

  @Autowired private ProFormatManager proFormatManager;
  @Autowired private SurveyPayloadParser surveyPayloadParser;
  @Autowired private EnrollmentRepository enrollmentRepository;
  @Autowired private HealthTrackerStatusRepository healthTrackerStatusRepository;
  // TODO: remove lazy annoation and refactor to avoid circular dependancy
  @Lazy @Autowired private EnrollmentService enrollmentService;
  @Autowired private CheckInRepository checkInRepository;
  @Autowired private PatientInfoServiceClient patientInfoClient;
  @Autowired private DefaultDroolsService ruleEngine;
  @Autowired private SqsHelper sqsHelper;
  @Autowired private Identity identity;
  @Autowired private HealthTrackerEventsPublisher eventsPublisher;
  @Autowired private ProReviewRepository proReviewRepository;

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
    Preconditions.checkNotNull(command, "command is required");

    processStatus(command.getEnrollmentId(), command.getSurveyPayload(), command.getCheckInIds());
  }

  public void push(String enrollmentId, SurveyPayload surveyPayload, List<CheckIn> checkIns) {
    Preconditions.checkNotNull(enrollmentId, "enrollmentId is required");
    Preconditions.checkNotNull(checkIns, "0 or more checkIns required");

    sqsProducer.send(
        new HealthTrackerStatusCommand(
            enrollmentId, surveyPayload, checkIns.stream().map(AbstractDocument::getId).toList()));
  }

  /**
   * Process the HealthTrackerStatus for a given enrollment.
   *
   * @param enrollmentId
   * @param surveyPayload
   * @param triggeringCheckInIds
   */
  public void processStatus(
      String enrollmentId, SurveyPayload surveyPayload, List<String> triggeringCheckInIds) {
    Preconditions.checkNotNull(enrollmentId, "enrollmentId is required");

    var enrollment = enrollmentRepository.findById(enrollmentId).orElseThrow();
    var status = getOrCreateNewStatus(enrollment);
    CheckIn firstTriggeringCheckIn =
        !triggeringCheckInIds.isEmpty()
            ? checkInRepository.findById(triggeringCheckInIds.get(0)).orElse(null)
            : null;

    // Save current category (status). This is used to check if we need to act on a status change
    var originalStatus = status.getCategory();
    var checkIns =
        checkInRepository.findByStatusNotAndEnrollmentIdOrderByScheduleDateDesc(
            CheckInStatus.PENDING, enrollmentId);

    var healthTracker =
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
      LocalDate scheduledDate =
          firstTriggeringCheckIn != null ? firstTriggeringCheckIn.getScheduleDate() : null;

      ProReview newProReview =
          createNewOrUpdateProReview(
              enrollment, triggeringCheckInIds, surveyPayload, status, scheduledDate);

      status.setProReviewId(newProReview.getId());
      status.setSurveyPayload(newProReview.getSurveyPayload());
      status.setLastCompletedCheckInIds(new HashSet<String>(newProReview.getCheckInIds()));
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
      status.setSymptomDetails(healthTracker.getSymptomDetailsAggregator().getSymptomDetails());
    }

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

    String completedBy = null;
    if (isPatientReportedCheckIn(surveyPayload) && checkIns != null && checkIns.size() > 0) {
      CheckIn mostRecentCheckIn = checkIns.get(0);
      if (mostRecentCheckIn != null) {
        status.setLastPatientCheckInAt(mostRecentCheckIn.getUpdatedDate());
        completedBy = mostRecentCheckIn.getCompletedBy();
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

    // find and replace avoids optimistic locking exception
    healthTrackerStatusRepository.findAndReplaceStatus(status);
    eventsPublisher.publishStatusChangeByRules(
        enrollment, originalStatus, status.getCategory(), surveyPayload);

    if (triagePayload != null) sendTriagePayload(triagePayload, enrollment);

    if (isPatientReportedCheckIn(surveyPayload)) {
      // if there was actual patient report, send it over
      patientRecordService.publishProData(enrollment, status, completedBy);
    } else {
      // if this is just status check, report missed checkins only
      patientRecordService.publishMissedCheckIn(enrollment, status.getMissedCheckIns());
    }
  }

  private boolean isPatientReportedCheckIn(SurveyPayload surveyPayload) {
    // FIXME: huh?
    return surveyPayload != null;
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

  private void sendTriagePayload(TriagePayload triagePayload, Enrollment enrollment) {
    log.debug("sending enrollment {} triage payload {}", enrollment.getId(), triagePayload);
    sendTriagePayloadViaRabbit(triagePayload);
    eventsPublisher.publishTriageTicketCreated(
        enrollment.getId(), enrollment.getClinicId(), enrollment.getPatientId(), null);
    metersService.incrementCounter(
        enrollment.getClinicId(), HealthTrackerCounterMetric.TRIAGE_TICKET_SUBMITTED);
  }

  private ProReview createNewOrUpdateProReview(
      Enrollment enrollment,
      List<String> checkInIds,
      SurveyPayload surveyPayload,
      HealthTrackerStatus status,
      LocalDate mostRecentCheckInDate) {

    var oralAdherence = surveyPayloadParser.parseOralAdherence(enrollment, surveyPayload);
    var sideEffects = surveyPayloadParser.parseSideEffects(enrollment, surveyPayload);

    return proReviewRepository.upsertByLatestCheckInDate(
        new ProReview(
            enrollment.getClinicId(),
            enrollment.getId(),
            checkInIds,
            surveyPayload,
            sideEffects,
            oralAdherence,
            status,
            mostRecentCheckInDate));
  }
}
