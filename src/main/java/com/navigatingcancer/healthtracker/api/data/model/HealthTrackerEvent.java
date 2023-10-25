package com.navigatingcancer.healthtracker.api.data.model;

import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Document(collection = "ht_events")
@CompoundIndex(name = "patient_event_idx", def = "{'clinicId': 1, 'patientId': 1, 'date': -1}")
public class HealthTrackerEvent extends AbstractDocument {

  public enum Type {
    ENROLLMENT_ACTIVE("Schedule active"), // 12 AM first day of cycle
    ENROLLMENT_UPDATED("Schedule updated"),
    ENROLLMENT_COMPLETED("Schedule ended"),
    ENROLLMENT_STOPPED("Schedule ended"),
    SCHEDULE_STARTED("Schedule created"),
    SCHEDULE_PENDING("Schedule pending"),
    REMINDER_SENT("Check-in reminder sent"),
    CYCLE_ENDED("Cycle check-ins ended"),
    CYCLE_STARTED("Cycle started"),
    CHECK_IN_MISSED("Missed check-in"),
    CHECK_IN_COMPLETED("Check-in completed"),
    PRACTICE_CHECK_IN_COMPLETED("Practice check-in completed"),
    CALL_ATTEMPT("Call attempt"),
    TRIAGE_TICKET_CREATED("Triage ticket created"), // rule-based, request a call, med
    TRIAGE_TICKET_CLOSED("Triage ticket resolved"),
    TRIAGE_TICKET_MARKED_AS_ERROR("Triage ticket marked as error"),
    STATUS_SET("Priority set"), // to “Action needed” or “Watch carefully” (rule-based)
    STATUS_CHANGED("Check-in moved"), // by clinician
    PRO_SENT_TO_EHR("PRO sent to EHR"), // by clinician
    PRO_REVIEW_NOTE_CREATED("PRO review note created"); // by clinician

    private String eventMessage;

    private Type(String msg) {
      eventMessage = msg;
    }

    public String getMessage() {
      return eventMessage;
    }
  }

  private String enrollmentId;
  private Long clinicId;
  private Long patientId;
  private Instant date; // date and time of the event
  private Type type;
  private String
      event; // Description of event ("Check-in completed", "Schedule started", "Enrollment
  // ended", etc.)
  private String by; // Description of who (Patient’s name, Clinician’s name)
  private List<String> relatedCheckinId; // PRO related to the event, if applicable
  private String reason;
  private String note;
  private String programType;
  @Deprecated private String surveyType; // CX, PX
  private List<CheckIn> relatedCheckins; // PRO related to the event, if applicable
  private Long missedCheckinsCount; // since last completed checkin
  private List<SideEffect> sideEffects;
  private List<Adherence> oralAdherence;
  private String proReviewId;
  private String proReviewNote;

  public void setType(Type tp) {
    this.type = tp;
    this.event = tp.getMessage();
  }
}
