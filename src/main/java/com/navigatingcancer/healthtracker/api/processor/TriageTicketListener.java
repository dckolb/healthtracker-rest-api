package com.navigatingcancer.healthtracker.api.processor;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.navigatingcancer.healthtracker.api.data.model.Enrollment;
import com.navigatingcancer.healthtracker.api.data.model.EnrollmentStatus;
import com.navigatingcancer.healthtracker.api.data.model.HealthTrackerStatus;
import com.navigatingcancer.healthtracker.api.data.model.HealthTrackerStatusCategory;
import com.navigatingcancer.healthtracker.api.data.repo.CustomEnrollmentRepository;
import com.navigatingcancer.healthtracker.api.data.repo.HealthTrackerStatusRepository;
import com.navigatingcancer.healthtracker.api.data.service.EnrollmentService;
import com.navigatingcancer.healthtracker.api.data.service.ProReviewService;
import com.navigatingcancer.healthtracker.api.data.service.impl.PatientRecordService;
import com.navigatingcancer.healthtracker.api.events.HealthTrackerEventsPublisher;
import com.navigatingcancer.healthtracker.api.metrics.HealthTrackerCounterMetric;
import com.navigatingcancer.healthtracker.api.metrics.MetersService;
import com.navigatingcancer.healthtracker.api.rest.QueryParameters.EnrollmentQuery;
import com.navigatingcancer.json.utils.JsonUtils;
import java.io.ByteArrayInputStream;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class TriageTicketListener implements MessageListener {
  public static final String TRIAGE_STATUS_CHANGE_REASON = "Triage status change";
  public static final String TRIAGE_MARKED_AS_ERROR_REASON = "Triage marked as error";

  private final EnrollmentService enrollmentService;
  private final HealthTrackerStatusRepository healthTrackerStatusRepository;
  private final HealthTrackerEventsPublisher eventsPublisher;
  private final ProReviewService proReviewService;
  private final MetersService metersService;
  private final CustomEnrollmentRepository enrollmentRepository;

  @Autowired
  public TriageTicketListener(
      EnrollmentService enrollmentService,
      HealthTrackerStatusRepository healthTrackerStatusRepository,
      HealthTrackerEventsPublisher eventsPublisher,
      ProReviewService proReviewService,
      MetersService metersService,
      CustomEnrollmentRepository enrollmentRepository) {
    this.enrollmentService = enrollmentService;
    this.healthTrackerStatusRepository = healthTrackerStatusRepository;
    this.eventsPublisher = eventsPublisher;
    this.proReviewService = proReviewService;
    this.metersService = metersService;
    this.enrollmentRepository = enrollmentRepository;
  }

  @RabbitListener(queues = "gc-ht2")
  @Override
  public void onMessage(Message message) {
    log.debug("Processing triage status update {}", message);

    TriageTicketPayload payload =
        JsonUtils.fromJson(new ByteArrayInputStream(message.getBody()), TriageTicketPayload.class);
    if (!payload.isValid()) {
      log.error("Message is not valid, skipping processing. payload={}", payload);
      return;
    }

    log.debug(
        "looking for enrollment with clinic_id {} and patientId {}",
        payload.clinicId,
        payload.patientId);

    EnrollmentQuery query = new EnrollmentQuery();
    query.setClinicId(List.of(payload.clinicId));
    query.setPatientId(List.of(payload.patientId));
    query.setStatus(List.of(EnrollmentStatus.ACTIVE));

    List<Enrollment> enrollments = enrollmentRepository.findEnrollments(query);
    log.debug("found {} enrollments", enrollments.size());
    enrollments.forEach(enrollment -> processEnrollment(enrollment, payload));
  }

  private void processEnrollment(Enrollment enrollment, TriageTicketPayload payload) {
    HealthTrackerStatus healthTrackerStatus =
        healthTrackerStatusRepository.getById(enrollment.getId());

    if (healthTrackerStatus == null) {
      log.error("failed to find ht_status for enrollment {}", enrollment);
      return;
    }

    healthTrackerStatus.setCategory(
        HealthTrackerStatusCategory.valueOf(payload.status.toUpperCase()));
    healthTrackerStatus.setActionPerformedBy(payload.updatedByName);
    healthTrackerStatusRepository.save(healthTrackerStatus);

    metersService.incrementCounter(
        enrollment.getClinicId(), HealthTrackerCounterMetric.TRIAGE_TICKET_CLOSED);

    if (healthTrackerStatus.getProReviewId() != null
        && ObjectId.isValid(healthTrackerStatus.getProReviewId())) {
      proReviewService.markEhrDelivered(
          healthTrackerStatus.getProReviewId(), payload.updatedByName, null);
    }

    String updatedById =
        payload.updatedBySecurityId != null
            ? payload.updatedBySecurityId
            : PatientRecordService.HEALTH_TRACKER_NAME;

    if (payload.markAsError != null && payload.markAsError) {
      enrollmentService.appendEventsLog(
          healthTrackerStatus.getId(),
          EnrollmentStatus.STATUS_CHANGE,
          TRIAGE_MARKED_AS_ERROR_REASON,
          null,
          updatedById,
          payload.updatedByName);

      eventsPublisher.publishTriageTicketMarkedAsError(
          enrollment.getId(),
          healthTrackerStatus.getCategory(),
          enrollment.getClinicId(),
          enrollment.getPatientId(),
          payload.updatedByName);
    } else {
      enrollmentService.appendEventsLog(
          healthTrackerStatus.getId(),
          EnrollmentStatus.STATUS_CHANGE,
          TRIAGE_STATUS_CHANGE_REASON,
          null,
          updatedById,
          payload.updatedByName);

      eventsPublisher.publishTriageTicketClosed(
          enrollment.getId(),
          healthTrackerStatus.getCategory(),
          enrollment.getClinicId(),
          enrollment.getPatientId(),
          payload.updatedByName);
    }
  }
}

@JsonIgnoreProperties(ignoreUnknown = true)
class TriageTicketPayload {
  @JsonProperty("clinic_id")
  Long clinicId;

  @JsonProperty("patient_id")
  Long patientId;

  @JsonProperty("updated_by_name")
  String updatedByName;

  @JsonProperty("updated_by_security_identity_id")
  String updatedBySecurityId;

  @JsonProperty("mark_as_error")
  Boolean markAsError;

  @JsonProperty("status")
  String status;

  boolean isValid() {
    return clinicId != null && patientId != null;
  }
}
