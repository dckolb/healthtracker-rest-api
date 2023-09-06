package com.navigatingcancer.healthtracker.api.data.service.impl;

import com.navigatingcancer.healthtracker.api.data.auth.Identity;
import com.navigatingcancer.healthtracker.api.data.model.Enrollment;
import com.navigatingcancer.healthtracker.api.data.model.HealthTrackerStatus;
import com.navigatingcancer.healthtracker.api.data.model.HealthTrackerStatusCategory;
import com.navigatingcancer.healthtracker.api.data.model.patientrecord.EnrollmentUpdatePayload;
import com.navigatingcancer.healthtracker.api.data.model.patientrecord.ProPayload;
import com.navigatingcancer.healthtracker.api.data.model.patientrecord.ProSentToEhrPayload;
import com.navigatingcancer.healthtracker.api.processor.model.AdherenceParser;
import com.navigatingcancer.healthtracker.api.processor.model.ProFormatManager;
import com.navigatingcancer.healthtracker.api.processor.model.SymptomParser;
import com.navigatingcancer.json.utils.JsonUtils;

import lombok.extern.slf4j.Slf4j;
import org.codehaus.plexus.util.StringUtils;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class PatientRecordService {

  private static final String PRO_SENT_TO_EHR_PAYLOAD_ROUTING_KEY =
      "app/health_tracker/patient/pro_sent_to_ehr";
  private static final String PRO_PAYLOAD_ROUTING_KEY =
      "app/health_tracker/patient/pro_record";
  private static final String ENROLLMENT_PAYLOAD_ROUTING_KEY =
      "app/health_tracker/patient/enrollment_record";

  public static final String HEALTH_TRACKER_NAME = "Health Tracker";

  @Autowired RabbitTemplate rabbitTemplate;

  @Autowired ProFormatManager proFormatManager;

  public void publishMissedCheckIn(Enrollment enrollment, Integer missedCount) {
    log.debug("PatientRecordService::missedCheckIn");
    if (missedCount == null || missedCount <= 0) {
      log.debug("skipping missed checkin recording");
      return;
    }

    EnrollmentUpdatePayload payload = new EnrollmentUpdatePayload(enrollment, null);

    String description =
        missedCount == 1 ? "1 missed check-in" : String.format("%d missed check-ins", missedCount);
    payload.setSystemReportedMissedCheckIn(true);
    payload.setSystemReportedMissedCheckInDescription(description);

    push(payload);
  }

  public void publishEnrollmentCreated(Enrollment enrollment, Identity identity) {
    log.debug("PatientRecordService::publishEnrollmentCreated");

    EnrollmentUpdatePayload schedulePayload =
        new EnrollmentUpdatePayload(enrollment, identity.getClinicianName());

    push(schedulePayload);
  }

  public void publishEnrollmentStatusUpdated(
      Enrollment enrollment,
      NotificationService.Event event,
      String reason,
      String note,
      Identity identity) {
    log.debug("PatientRecordService::publishEnrollmentStatusUpdated");

    EnrollmentUpdatePayload schedulePayload = new EnrollmentUpdatePayload(enrollment, identity.getClinicianName());
    schedulePayload.setEnrollmentStatus(event.toString());
    schedulePayload.setReason(reason);
    schedulePayload.setReasonDetails(note);

    push(schedulePayload);
  }

  public void publishEnrollmentUpdated(Enrollment enrollment, Identity identity) {
    log.debug("PatientRecordService::publishEnrollmentUpdated");

    EnrollmentUpdatePayload schedulePayload = new EnrollmentUpdatePayload(enrollment, identity.getClinicianName());
    schedulePayload.setEnrollmentStatus("ht_edited");

    push(schedulePayload);
  }


  public void publishEnrollmentStatusChange(
      HealthTrackerStatus status,
      HealthTrackerStatusCategory from,
      HealthTrackerStatusCategory to,
      Identity identity) {
    log.debug("PatientRecordService::publishEnrollmentStatusChange");

    EnrollmentUpdatePayload payload = new EnrollmentUpdatePayload(status, from, to, identity);

    push(payload);
  }

  public void publishProData(
      Enrollment enrollment, HealthTrackerStatus htStatus, String completedBy) {
    log.debug("PatientRecordService::publishProData");
    log.debug("htStatus is {}", htStatus);

    // if we have a completeby passed in, use that otherwise, assume patient
    String createdBy =
        StringUtils.isNotBlank(completedBy)
            ? completedBy
            : String.format(
                "%s %s",
                htStatus.getPatientInfo().getFirstName(), htStatus.getPatientInfo().getLastName());

    ProPayload proPayload = new ProPayload(enrollment, createdBy);
    boolean isProCtcaeFormat = proFormatManager.followsCtcaeStandard(enrollment);

    if (htStatus.getSymptomDetails() != null) {
      proPayload.setAllSymptomDetails(htStatus.getSymptomDetails());
    } else {
      log.debug("symptomDetails missing {}", htStatus);
    }

    String medicationName = enrollment.getMedication();
    AdherenceParser adherenceParser = new AdherenceParser();

    log.debug("surveyPayload {}", htStatus.getSurveyPayloadSymptoms());
    if (htStatus.getSurveyPayloadSymptoms() != null) {
      // was throwing a null pointer on the below line
      proPayload.setAllSideEffects(
          SymptomParser.parseIntoSideEffects(
              isProCtcaeFormat, htStatus.getSurveyPayloadSymptoms()));
    } else {
      log.error("null surveypayload symptoms {}", htStatus);
    }

    if (htStatus.getSurveyPayloadOrals() != null) {
      proPayload.setAllAdherences(
          adherenceParser.parse(htStatus.getSurveyPayloadOrals(), medicationName));
    } else {
      log.error("null surveypayload orals {}", htStatus);
    }

    if (proPayload.getAllAdherences().isEmpty()
        && proPayload.getAllSymptomDetails().isEmpty()
        && proPayload.getAllSideEffects().isEmpty()) {
      log.debug(
          "PRO payload is missing adherence, side effects and symptom details. Not sending it: {}",
          proPayload);
      return;
    }

    proPayload.setEndCurrentCycle(htStatus.getEndCurrentCycle());

    push(proPayload);
  }

  public void publishProSentToEhr(
          String enrollmentId, String by, Long clinicId, Long patientId, String proReviewId, String documentTitle) {

    log.debug("PatientRecordService::publishProSentToEhr");

    ProSentToEhrPayload payload = new ProSentToEhrPayload(enrollmentId, by, clinicId, patientId, proReviewId, documentTitle);

    push(payload);
  }

  private void push(String routingKey, Object payload) {
    String json = JsonUtils.toJson(payload);
    log.info("Pushing payload with routing key {} with payload {}", routingKey, json);
    try {
      rabbitTemplate.convertAndSend(routingKey, json);
    } catch (AmqpException e) {
      log.error("unable to publish to rabbitmq ", e);
    }
  }

  private void push(ProSentToEhrPayload payload) {
    push(PRO_SENT_TO_EHR_PAYLOAD_ROUTING_KEY, payload);
  }

  private void push(ProPayload proPayload) {
    push(PRO_PAYLOAD_ROUTING_KEY, proPayload);
  }

  private void push(EnrollmentUpdatePayload enrollmentUpdatePayload) {
    push(ENROLLMENT_PAYLOAD_ROUTING_KEY, enrollmentUpdatePayload);
  }
}
