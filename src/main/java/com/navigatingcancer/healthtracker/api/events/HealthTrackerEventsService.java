package com.navigatingcancer.healthtracker.api.events;

import com.navigatingcancer.healthtracker.api.data.auth.AuthInterceptor;
import com.navigatingcancer.healthtracker.api.data.model.CallAttempt;
import com.navigatingcancer.healthtracker.api.data.model.CheckIn;
import com.navigatingcancer.healthtracker.api.data.model.Enrollment;
import com.navigatingcancer.healthtracker.api.data.model.HealthTrackerEvent;
import com.navigatingcancer.healthtracker.api.data.model.HealthTrackerStatusCategory;
import com.navigatingcancer.healthtracker.api.data.model.survey.SurveyPayload;
import com.navigatingcancer.healthtracker.api.data.model.surveyConfig.ProgramConfig;
import com.navigatingcancer.healthtracker.api.data.repo.CheckInRepository;
import com.navigatingcancer.healthtracker.api.data.repo.HealthTrackerEventsRepository;
import com.navigatingcancer.healthtracker.api.data.service.SurveyConfigService;
import com.navigatingcancer.healthtracker.api.processor.model.SurveyPayloadParser;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class HealthTrackerEventsService {

  @Autowired HealthTrackerEventsRepository eventsRepository;

  @Autowired SurveyConfigService surveyConfigService;

  @Autowired CheckInRepository checkInRepository;

  @Autowired SurveyPayloadParser surveyPayloadParser;

  private final void parseAndSetSymptoms(
      Enrollment enr, SurveyPayload sp, HealthTrackerEvent event) {
    event.setSideEffects(surveyPayloadParser.parseSideEffects(enr, sp));
    event.setOralAdherence(surveyPayloadParser.parseOralAdherence(enr, sp));
  }

  private void addRelatedCheckinToEvent(List<String> ciids, HealthTrackerEvent event) {
    if (ciids == null) {
      return;
    }
    List<CheckIn> checkins =
        ciids.stream()
            .map(i -> checkInRepository.findById(i))
            .filter(o -> o.isPresent())
            .map(o -> o.get())
            .collect(Collectors.toList());
    List<String> validIds = checkins.stream().map(ci -> ci.getId()).collect(Collectors.toList());
    if (validIds.size() != ciids.size()) {
      log.error("Can not find all requested checkins {}, only found {}", ciids, validIds);
    }

    event.setRelatedCheckinId(validIds);
    event.setRelatedCheckins(checkins);
  }

  private static final String statusChangeMessage(String from, String to) {
    return "Status change from " + from + " to " + to;
  }

  @Async
  @EventListener
  void handleStatusChangeEventWithLog(StatusChangeEvent notification) {
    HealthTrackerEvent event = new HealthTrackerEvent();
    if (notification.getChangeType() == StatusChangeEvent.Type.BY_CLINICIAN) {
      event.setType(HealthTrackerEvent.Type.STATUS_CHANGED);
    } else if (notification.getChangeType() == StatusChangeEvent.Type.BY_RULES) {
      event.setType(HealthTrackerEvent.Type.STATUS_SET);
    } else {
      log.error("Unsupported state change notification type {}", notification);
    }
    event.setEnrollmentId(notification.getEnrollmentId());
    event.setClinicId(notification.getClinicId());
    event.setPatientId(notification.getPatientId());
    event.setDate(notification.getDate());
    event.setNote(statusChangeMessage(notification.getFromStatus(), notification.getToStatus()));
    event.setBy(notification.getClinicianName());

    if (notification.getEnrollment() != null && notification.getSurveyPayload() != null) {
      parseAndSetSymptoms(notification.getEnrollment(), notification.getSurveyPayload(), event);
    } else if (notification.getChangeType() == StatusChangeEvent.Type.BY_RULES) {
      log.error("missing enrollment or survey in event {}", notification);
    }

    // add related check-in if ID was provided
    addRelatedCheckinToEvent(notification.getCheckinIds(), event);

    eventsRepository.save(event); // TODO. Error processing
  }

  private static HealthTrackerEvent eventFromNotification(EnrollmentEvent notification) {
    HealthTrackerEvent event = new HealthTrackerEvent();
    Enrollment e = notification.getEnrollment();
    event.setEnrollmentId(e.getId());
    event.setClinicId(e.getClinicId());
    event.setPatientId(e.getPatientId());
    event.setDate(notification.getDate());
    event.setBy(notification.getClinicianName());
    return event;
  }

  @Async
  @EventListener
  void handleEnrollmentCreate(EnrollmentCreated notification) {
    HealthTrackerEvent event = eventFromNotification(notification);
    event.setType(HealthTrackerEvent.Type.SCHEDULE_STARTED);
    eventsRepository.save(event); // TODO. Error processing

    // TODO. Compare create date with the treatment start date before doing this
    event = eventFromNotification(notification);
    event.setType(HealthTrackerEvent.Type.SCHEDULE_PENDING);
    event.setEvent("Schedule pending");
    event.setBy(AuthInterceptor.HEALTH_TRACKER_NAME);
    eventsRepository.save(event); // TODO. Error processing
  }

  @Async
  @EventListener
  void handleEnrollmentUpdated(EnrollmentUpdated notification) {
    HealthTrackerEvent event = eventFromNotification(notification);
    event.setType(HealthTrackerEvent.Type.ENROLLMENT_UPDATED);
    if (notification.getDiffs() != null) {
      event.setNote(String.join("\n", notification.getDiffs()));
    }
    eventsRepository.save(event); // TODO. Error processing
  }

  @Async
  @EventListener
  void handleEnrollmentStopped(EnrollmentStopped notification) {
    HealthTrackerEvent event = eventFromNotification(notification);
    event.setType(HealthTrackerEvent.Type.ENROLLMENT_STOPPED);
    event.setReason(notification.getReason());
    event.setNote(notification.getNote());
    eventsRepository.save(event); // TODO. Error processing
  }

  @Async
  @EventListener
  void handleTriageTicket(TriageTicketEvent notification) {
    HealthTrackerEvent event = new HealthTrackerEvent();
    switch (notification.getType()) {
      case CREATED:
        event.setType(HealthTrackerEvent.Type.TRIAGE_TICKET_CREATED);
        event.setNote(notification.getNote()); // Not sure if there is a note.
        break;
      case CLOSED:
        event.setType(HealthTrackerEvent.Type.TRIAGE_TICKET_CLOSED);
        event.setNote(
            statusChangeMessage(
                HealthTrackerStatusCategory.TRIAGE.toString(),
                notification.getStatus().toString()));
        break;
      case MARKED_AS_ERROR:
        event.setType(HealthTrackerEvent.Type.TRIAGE_TICKET_MARKED_AS_ERROR);
        event.setNote(
            statusChangeMessage(
                HealthTrackerStatusCategory.TRIAGE.toString(),
                notification.getStatus().toString()));
        break;
      case REQUEST_CALL:
        event.setType(HealthTrackerEvent.Type.TRIAGE_TICKET_CREATED);
        event.setReason("Call request");
        event.setNote(notification.getNote()); // Note can come with request a call
        break;
      case REQUEST_REFILL:
        event.setType(HealthTrackerEvent.Type.TRIAGE_TICKET_CREATED);
        event.setReason("Medication: Refill Request");
        event.setNote(notification.getNote()); // Note can come with request refill
        break;
    }
    event.setEnrollmentId(notification.getEnrollmentId());
    event.setClinicId(notification.getClinicId());
    event.setPatientId(notification.getPatientId());
    event.setDate(notification.getDate());
    event.setBy(notification.getClinicianName());
    eventsRepository.save(event); // TODO. Error processing
  }

  @Async
  @EventListener
  void handleCheckinCompleted(CheckinCompleted notification) {
    HealthTrackerEvent event = new HealthTrackerEvent();
    List<CheckIn> checkins = notification.getCheckin();
    event.setType(HealthTrackerEvent.Type.CHECK_IN_COMPLETED);
    Enrollment enr = notification.getEnrollment();
    event.setEnrollmentId(enr.getId());
    if (enr.getProgramId() == null) {
      log.warn("No program ID in enrollment {}", enr);
      event.setProgramType("HealthTracker");
    } else {
      // Get program type: HealthTracker, PRO-CARE, ...
      ProgramConfig programConfig = surveyConfigService.getProgramConfig(enr.getProgramId());
      if (programConfig != null) {
        event.setProgramType(programConfig.getType());
      } else {
        log.error("Unknown program ID in enrollment {}", enr);
      }
    }
    // Set survey type: CX or PX
    String surveyType = null;
    if (enr.isManualCollect()) {
      surveyType = ProgramConfig.CLINIC_COLLECT;
    } else {
      // TODO. Is it possible that the clincian did the checkin even if it is not
      // manual collect?
      surveyType = ProgramConfig.PATIENT_COLLECT;
    }
    event.setSurveyType(surveyType);
    event.setRelatedCheckins(checkins);
    List<String> relatedIds = checkins.stream().map(c -> c.getId()).collect(Collectors.toList());
    event.setRelatedCheckinId(relatedIds);

    parseAndSetSymptoms(enr, notification.getSurveyPayload(), event);

    event.setDate(notification.getDate());
    event.setBy(notification.getBy());
    event.setClinicId(enr.getClinicId());
    event.setPatientId(enr.getPatientId());
    eventsRepository.upsertCheckinEvent(event); // TODO. Error processing
  }

  @Async
  @EventListener
  void handleCallAttempt(CallAttemptEvent notification) {
    HealthTrackerEvent event = new HealthTrackerEvent();
    event.setType(HealthTrackerEvent.Type.CALL_ATTEMPT);
    CallAttempt ca = notification.getCallAttemptData();
    addRelatedCheckinToEvent(List.of(ca.getCheckInId()), event);
    if (event.getRelatedCheckins().isEmpty()) {
      log.error("Can not find call attempt checkin for {}", ca);
    } else {
      event.setPatientId(event.getRelatedCheckins().get(0).getPatientId());
    }
    event.setNote(ca.getNotes());
    event.setEnrollmentId(ca.getEnrollmentId());
    event.setClinicId(ca.getClinicId());
    event.setDate(notification.getDate());
    event.setBy(notification.getBy());
    eventsRepository.save(event); // TODO. Error processing
  }

  @Async
  @EventListener
  void handleCycleAutoClosed(CycleAutoClosed notification) {
    HealthTrackerEvent event = new HealthTrackerEvent();
    event.setType(HealthTrackerEvent.Type.CYCLE_ENDED);
    event.setReason("Check-ins have auto-ended for this cycle");
    Enrollment enr = notification.getEnrollment();
    event.setEnrollmentId(enr.getId());
    event.setClinicId(enr.getClinicId());
    event.setPatientId(enr.getPatientId());
    event.setDate(notification.getDate());
    List<CheckIn> cins = notification.getCheckin();
    if (cins != null && !cins.isEmpty()) {
      event.setRelatedCheckins(cins);
      event.setRelatedCheckinId(cins.stream().map(ci -> ci.getId()).collect(Collectors.toList()));
    }
    event.setBy(AuthInterceptor.HEALTH_TRACKER_NAME);
    eventsRepository.save(event); // TODO. Error processing
  }

  @Async
  @EventListener
  void handlePracticeCheckInCompleted(PracticeCheckInCompleted notification) {
    HealthTrackerEvent event = new HealthTrackerEvent();
    event.setType(HealthTrackerEvent.Type.PRACTICE_CHECK_IN_COMPLETED);
    event.setClinicId(notification.getClinicId());
    event.setPatientId(notification.getPatientId());
    event.setBy(notification.getBy());
    event.setDate(notification.getDate());
    eventsRepository.save(event);
  }

  @Async
  @EventListener
  void handleProSentToEhr(ProSentToEhrEvent notification) {
    HealthTrackerEvent event = new HealthTrackerEvent();
    event.setType(HealthTrackerEvent.Type.PRO_SENT_TO_EHR);
    event.setClinicId(notification.clinicId);
    event.setBy(notification.by);
    event.setPatientId(notification.patientId);
    event.setDate(java.time.Instant.now());
    event.setProReviewId(notification.proReviewId);
    addRelatedCheckinToEvent(notification.checkinIds, event);

    eventsRepository.save(event);
  }

  @Async
  @EventListener
  void handleProReviewNoteCreated(ProReviewNoteEvent notification) {
    HealthTrackerEvent event = new HealthTrackerEvent();
    event.setType(HealthTrackerEvent.Type.PRO_REVIEW_NOTE_CREATED);
    event.setClinicId(notification.clinicId);
    event.setBy(notification.by);
    event.setPatientId(notification.patientId);
    event.setDate(java.time.Instant.now());
    event.setProReviewId(notification.proReviewId);
    addRelatedCheckinToEvent(notification.checkinIds, event);
    event.setProReviewNote(notification.noteContent);

    eventsRepository.save(event);
  }
}
