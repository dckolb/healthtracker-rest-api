package com.navigatingcancer.healthtracker.api.data.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.navigatingcancer.healthtracker.api.data.model.survey.SurveyId;
import com.navigatingcancer.healthtracker.api.data.model.survey.SurveyItemPayload;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;
import java.util.Optional;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Document(collection = "checkins")
public class CheckIn extends AbstractDocument implements Comparable<CheckIn> {
  @Indexed private String enrollmentId;
  @Indexed private Long patientId;
  private Long clinicId;
  private Long locationId;
  private String checkInScheduleId;
  private String completedBy;
  private String surveyInstanceId;
  private Map<String, Object> checkInParameters;
  private LocalDate scheduleDate;
  private LocalTime scheduleTime;
  private CheckInStatus status;
  private SurveyItemPayload surveyPayload;
  private LocalDate patientReportedTxStartDate;
  private LocalDate txStartDate;
  private LocalDateTime expiresAt;
  private ContactPreferences contactPreferences;
  private String surveyId;
  private ReasonForCheckInCreation createdReason;

  /**
   * @deprecated Remove or refactor to be schedule-scoped or, if needed for survey UX, add as
   *     surveyParameter
   */
  @Deprecated private LocalDate enrollmentPatientReportedStartDate;

  /**
   * @deprecated Remove or refactor to be schedule-scoped or, if needed for survey UX, add as
   *     surveyParameter
   */
  @Deprecated private LocalDate enrollmentReminderStartDate;

  /** @deprecated get from surveyResponse */
  @Deprecated private Boolean declineACall;

  @Deprecated private String declineACallComment;
  @Deprecated private Boolean medicationTaken;
  @Deprecated private Boolean medicationStarted;
  /* End get from survey response */

  /** @deprecated use surveyInstance and params instead */
  @Deprecated private CheckInType checkInType;

  public CheckIn(Enrollment enrollment) {
    this.enrollmentId = enrollment.getId();
    this.patientId = enrollment.getPatientId();
    this.clinicId = enrollment.getClinicId();
    this.locationId = enrollment.getLocationId();
  }

  public CheckIn(String enrollmentId) {
    this.enrollmentId = enrollmentId;
  }

  public boolean getHasNotStartedMedication() {
    if (medicationStarted == null) return false;
    return !medicationStarted;
  }

  public Boolean patientReportedDiffrentReminderStartDate() {
    if (patientReportedTxStartDate == null) return false;
    if (enrollmentReminderStartDate == null) return null;
    return (patientReportedTxStartDate.getDayOfYear()
        != enrollmentReminderStartDate.getDayOfYear());
  }

  public Boolean patientReportedDifferentTxStartDate() {
    if (patientReportedTxStartDate == null) return false;
    if (patientReportedTxStartDate != null && txStartDate == null) return true;
    if (txStartDate == null) return null;
    return (patientReportedTxStartDate.getDayOfYear() != txStartDate.getDayOfYear());
  }

  public boolean patientReportedDifferentStartDate() {
    Boolean reminder = patientReportedDiffrentReminderStartDate();
    Boolean treatment = patientReportedDifferentTxStartDate();
    if (reminder != null && reminder == true) return true;
    if (treatment != null && treatment == true) return true;
    return false;
  }

  public boolean patientHasYetToReportStartDate() {
    return (patientReportedTxStartDate == null && enrollmentPatientReportedStartDate == null);
  }

  @JsonIgnore
  public LocalDateTime getScheduleDateTime() {
    if (getScheduleDate() == null || getScheduleTime() == null) {
      return null;
    }
    return LocalDateTime.of(getScheduleDate(), getScheduleTime());
  }

  @Override
  public int compareTo(CheckIn o) {
    var c1Idx =
        SurveyId.PREFERRED_SURVEY_ORDER.indexOf(Optional.ofNullable(getSurveyId()).orElse(""));
    var c2Idx =
        SurveyId.PREFERRED_SURVEY_ORDER.indexOf(Optional.ofNullable(o.getSurveyId()).orElse(""));
    if (c1Idx == c2Idx) {
      return 0;
    }

    if (c1Idx == -1) {
      return 1;
    }

    if (c2Idx == -1) {
      return -1;
    }

    return c1Idx - c2Idx;
  }
}
