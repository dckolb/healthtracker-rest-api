package com.navigatingcancer.healthtracker.api.events;

import com.navigatingcancer.healthtracker.api.data.auth.Identity;
import com.navigatingcancer.healthtracker.api.data.model.CallAttempt;
import com.navigatingcancer.healthtracker.api.data.model.CheckIn;
import com.navigatingcancer.healthtracker.api.data.model.Enrollment;
import com.navigatingcancer.healthtracker.api.data.model.HealthTrackerStatusCategory;
import com.navigatingcancer.healthtracker.api.data.model.PracticeCheckIn;
import com.navigatingcancer.healthtracker.api.data.model.survey.SurveyPayload;
import com.navigatingcancer.healthtracker.api.data.service.impl.PatientRecordService;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class HealthTrackerEventsPublisher {

  @Autowired private ApplicationEventPublisher publisher;

  public static final String catName(HealthTrackerStatusCategory cat) {
    if (cat == null) {
      return HealthTrackerStatusCategory.NO_ACTION_NEEDED.name();
    } else {
      return cat.name();
    }
  }

  protected static StatusChangeEvent prepareStatusChangeEvent(
      String enrollmentId,
      Long clinicId,
      Long patientId,
      String reason,
      HealthTrackerStatusCategory from,
      HealthTrackerStatusCategory to,
      Identity identity) {
    // From original call:
    // enrollmentService.appendEventsLog(id, EnrollmentStatus.STATUS_CHANGE, "HT
    // status change with API call", "from " + catName(status.getCategory()) + " to
    // " + catName(cat));
    if (identity != null) {
      return prepareStatusChangeEvent(
          enrollmentId,
          clinicId,
          patientId,
          reason,
          from,
          to,
          identity.getClinicianName() == null
              ? PatientRecordService.HEALTH_TRACKER_NAME
              : identity.getClinicianName(),
          identity.getClinicianId());
    } else {
      return prepareStatusChangeEvent(
          enrollmentId,
          clinicId,
          patientId,
          reason,
          from,
          to,
          PatientRecordService.HEALTH_TRACKER_NAME,
          null);
    }
  }

  protected static StatusChangeEvent prepareStatusChangeEvent(
      String enrollmentId,
      Long clinicId,
      Long patientId,
      String reason,
      HealthTrackerStatusCategory from,
      HealthTrackerStatusCategory to,
      String clinicianName,
      String clinicianId) {
    StatusChangeEvent e = new StatusChangeEvent();
    e.setEnrollmentId(enrollmentId);
    e.setClinicId(clinicId);
    e.setPatientId(patientId);
    e.setReason(reason);
    e.setNote("from " + catName(from) + " to " + catName(to));
    e.setFromStatus(
        from != null ? from.name() : HealthTrackerStatusCategory.NO_ACTION_NEEDED.name());
    e.setToStatus(to != null ? to.name() : HealthTrackerStatusCategory.NO_ACTION_NEEDED.name());
    e.setClinicianName(clinicianName);
    e.setClinicianId(clinicianId);
    e.setDate(Instant.now());
    return e;
  }

  public void publishStatusChange(
      String enrollmentId,
      Long clinicId,
      Long patientId,
      String reason,
      HealthTrackerStatusCategory from,
      HealthTrackerStatusCategory to,
      List<String> checkinIds,
      Identity identity) {
    // From original call:
    // enrollmentService.appendEventsLog(id, EnrollmentStatus.STATUS_CHANGE, "HT
    // status change with API call", "from " + catName(status.getCategory()) + " to
    // " + catName(cat));
    StatusChangeEvent e =
        prepareStatusChangeEvent(enrollmentId, clinicId, patientId, reason, from, to, identity);
    e.setChangeType(StatusChangeEvent.Type.BY_CLINICIAN);
    e.setCheckinIds(checkinIds);
    log.debug("HealthTrackerEventsPublisher publishing event {}", e);
    publisher.publishEvent(e);
  }

  public void publishEnrollmentCreated(Enrollment enrollment, Identity identity) {
    EnrollmentCreated e = new EnrollmentCreated();
    e.setEnrollment(enrollment);
    e.setClinicianId(identity.getClinicianId());
    e.setClinicianName(identity.getClinicianName());
    e.setDate(enrollment.getCreatedDate().toInstant());
    log.debug("HealthTrackerEventsPublisher publishing event {}", e);
    publisher.publishEvent(e);
  }

  public void publishPracticeCheckinCompleted(PracticeCheckIn practiceCheckIn) {
    PracticeCheckInCompleted e = new PracticeCheckInCompleted();
    e.setClinicId(practiceCheckIn.getClinicId());
    e.setPatientId(practiceCheckIn.getPatientId());
    e.setDate(Instant.now());
    e.setBy(practiceCheckIn.getCompletedBy());
    log.debug("HealthTrackerEventsPublisher publishing event {}", e);
    publisher.publishEvent(e);
  }

  public void publishEnrollmentUpdated(
      Enrollment enrollment, List<String> diffList, Identity identity) {
    EnrollmentUpdated e = new EnrollmentUpdated();
    e.setEnrollment(enrollment);
    e.setDiffs(diffList);
    e.setClinicianId(identity.getClinicianId());
    e.setClinicianName(identity.getClinicianName());
    e.setDate(Instant.now());
    log.debug("HealthTrackerEventsPublisher publishing event {}", e);
    publisher.publishEvent(e);
  }

  public void publishEnrollmentStopped(
      Enrollment enrollment,
      String reason,
      String note,
      Identity identity,
      Instant statusChangeTime) {
    EnrollmentStopped e = new EnrollmentStopped();
    e.setEnrollment(enrollment);
    e.setClinicianId(identity.getClinicianId());
    e.setClinicianName(identity.getClinicianName());
    e.setDate(statusChangeTime);
    e.setReason(reason);
    e.setNote(note);
    log.debug("HealthTrackerEventsPublisher publishing event {}", e);
    publisher.publishEvent(e);
  }

  public void publishCheckinCompleted(
      Enrollment enrollment, List<CheckIn> checkins, SurveyPayload surveyPayload) {
    if (checkins.size() > 0) {
      CheckinCompleted e = new CheckinCompleted();
      e.setCheckin(checkins);
      e.setEnrollment(enrollment);
      // Find the min update time
      Optional<Instant> updateTime =
          checkins.stream()
              .map(c -> c.getUpdatedDate().toInstant())
              .min(Comparator.comparing(Instant::toEpochMilli));
      e.setDate(updateTime.get());
      e.setBy(checkins.get(0).getCompletedBy());
      e.setSurveyPayload(surveyPayload);
      log.debug("HealthTrackerEventsPublisher publishing event {}", e);
      publisher.publishEvent(e);
    } else {
      log.error("checkin completed event called without any checkins {}", enrollment);
    }
  }

  private TriageTicketEvent prepareTriageTicket(
      String enrollmentId,
      Long clinicId,
      Long patientId,
      TriageTicketEvent.Type type,
      Identity identity) {
    TriageTicketEvent e = new TriageTicketEvent();
    e.setType(type);
    e.setEnrollmentId(enrollmentId);
    e.setClinicId(clinicId);
    e.setPatientId(patientId);
    if (identity != null) {
      e.setClinicianName(
          identity.getClinicianName() == null
              ? PatientRecordService.HEALTH_TRACKER_NAME
              : identity.getClinicianName());
      e.setClinicianId(identity.getClinicianId());
    } else {
      e.setClinicianName(PatientRecordService.HEALTH_TRACKER_NAME);
    }
    e.setDate(Instant.now()); // TODO. Is it really now??
    return e;
  }

  public void publishTriageTicketCreated(
      String enrollmentId, Long clinicId, Long patientId, Identity identity) {
    var e =
        prepareTriageTicket(
            enrollmentId, clinicId, patientId, TriageTicketEvent.Type.CREATED, identity);
    log.debug("HealthTrackerEventsPublisher publishing event {}", e);
    publisher.publishEvent(e);
  }

  public void publishTriageTicketClosed(
      String enrollmentId,
      HealthTrackerStatusCategory status,
      Long clinicId,
      Long patientId,
      String clinician) {
    var e =
        prepareTriageTicket(enrollmentId, clinicId, patientId, TriageTicketEvent.Type.CLOSED, null);
    e.setStatus(status);
    e.setClinicianName(clinician);
    log.debug("HealthTrackerEventsPublisher publishing event {}", e);
    publisher.publishEvent(e);
  }

  public void publishTriageTicketMarkedAsError(
      String enrollmentId,
      HealthTrackerStatusCategory status,
      Long clinicId,
      Long patientId,
      String clinician) {
    var e =
        prepareTriageTicket(
            enrollmentId, clinicId, patientId, TriageTicketEvent.Type.MARKED_AS_ERROR, null);
    e.setStatus(status);
    e.setClinicianName(clinician);
    log.debug("HealthTrackerEventsPublisher publishing event {}", e);
    publisher.publishEvent(e);
  }

  public void publishRequestCallEvent(
      Long patientId, Long clinicId, String details, Identity identity) {
    // TODO. Do we need to find out the enrollment ID?
    var e =
        prepareTriageTicket(
            null, clinicId, patientId, TriageTicketEvent.Type.REQUEST_CALL, identity);
    e.setNote(details);
    log.debug("HealthTrackerEventsPublisher publishing event {}", e);
    publisher.publishEvent(e);
  }

  public void publishRequestRefillEvent(
      Long patientId, Long clinicId, String details, Identity identity) {
    // TODO. Do we need to find out the enrollment ID?
    var e =
        prepareTriageTicket(
            null, clinicId, patientId, TriageTicketEvent.Type.REQUEST_REFILL, identity);
    e.setNote(details);
    log.debug("HealthTrackerEventsPublisher publishing event {}", e);
    publisher.publishEvent(e);
  }

  public void publishStatusChangeByRules(
      Enrollment enrollment,
      HealthTrackerStatusCategory originalStatus,
      HealthTrackerStatusCategory toStatus,
      SurveyPayload surveyPayload) {
    StatusChangeEvent e =
        prepareStatusChangeEvent(
            enrollment.getId(),
            enrollment.getClinicId(),
            enrollment.getPatientId(),
            "HT status change by rules",
            originalStatus,
            toStatus,
            null);
    e.setChangeType(StatusChangeEvent.Type.BY_RULES);
    e.setEnrollment(enrollment);
    e.setSurveyPayload(surveyPayload);
    log.debug("HealthTrackerEventsPublisher publishing event {}", e);
    publisher.publishEvent(e);
  }

  public void publishCallAttempt(CallAttempt ca, Identity identity) {
    CallAttemptEvent e = new CallAttemptEvent();
    e.setCallAttemptData(ca);
    e.setBy(identity.getClinicianName());
    e.setDate(Instant.now());
    log.debug("HealthTrackerEventsPublisher publishing event {}", e);
    publisher.publishEvent(e);
  }

  public void publishCycleAutoClosed(Enrollment enrollment, CheckIn cin) {
    CycleAutoClosed e = new CycleAutoClosed();
    e.setDate(Instant.now());
    e.setEnrollment(enrollment);
    if (cin != null) {
      e.setCheckin(List.of(cin));
    } else {
      log.warn("no checkin in the auto close event for enrollment {}", enrollment);
    }
    log.debug("HealthTrackerEventsPublisher publishing event {}", e);
    publisher.publishEvent(e);
  }

  public void publishProSentToEhr(
      String enrollmentId,
      String by,
      Long clinicId,
      Long patientId,
      List<String> checkinIds,
      String proReviewId) {
    ProSentToEhrEvent e =
        new ProSentToEhrEvent(enrollmentId, by, clinicId, patientId, checkinIds, proReviewId);

    log.debug("HealthTrackerEventsPublisher publishing event {}", e);
    publisher.publishEvent(e);
  }

  public void publishProReviewNote(
      String enrollmentId,
      String by,
      Long clinicId,
      Long patientId,
      List<String> checkinIds,
      String proReviewId,
      String noteContent) {

    ProReviewNoteEvent e =
        new ProReviewNoteEvent(
            enrollmentId, by, clinicId, patientId, checkinIds, proReviewId, noteContent);

    log.debug("HealthTrackerEventsPublisher publishing event {}", e);
    publisher.publishEvent(e);
  }
}
