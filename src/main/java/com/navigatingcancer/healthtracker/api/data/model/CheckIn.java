package com.navigatingcancer.healthtracker.api.data.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.navigatingcancer.healthtracker.api.data.model.survey.SurveyItemPayload;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Document(collection = "checkins")
public class CheckIn extends AbstractDocument {

  @JsonIgnore public static final String HEALTH_TRACKER_CX = "5eb1b09b24992c0fe1779085";
  @JsonIgnore public static final String HEALTH_TRACKER_PX = "5ef3cc9c296b54c5bc8af1d2";
  @JsonIgnore public static final String ORAL_ADHERENCE_CX = "5eb2461624992c0fe1779088";
  @JsonIgnore public static final String ORAL_ADHERENCE_PX = "5ef3ccd0296b54c5bc8af1d3";
  @JsonIgnore public static final String ORAL_ADHERENCE_HT_CX = "5eb23a0924992c0fe1779087";
  @JsonIgnore public static final String ORAL_ADHERENCE_HT_PX = "5ef503f66e236d2a8797613d";
  @JsonIgnore public static final String PROCTCAE_CX = "5eb1fc9824992c0fe1779086";
  @JsonIgnore public static final String PROCTCAE_PX = "5ef28c0b5931f51c58ed6ff7";

  public CheckIn(Enrollment enrollment) {
    this.enrollmentId = enrollment.getId();
    this.patientId = enrollment.getPatientId();
    this.clinicId = enrollment.getClinicId();
    this.locationId = enrollment.getLocationId();
  }

  public CheckIn(String enrollmentId) {
    this.enrollmentId = enrollmentId;
  }

  @Indexed private String enrollmentId;
  private Long patientId;
  private Long clinicId;
  private Long locationId;
  private String completedBy;
  private CheckInType checkInType;
  private LocalDate scheduleDate;
  private LocalTime scheduleTime;
  private CheckInStatus status;
  private SurveyItemPayload surveyPayload;
  private Boolean medicationTaken;
  private Boolean medicationStarted;
  private LocalDate patientReportedTxStartDate;
  private LocalDate txStartDate;
  private String surveyId;
  private Boolean clinicCollect;
  private LocalDate enrollmentPatientReportedStartDate;
  private LocalDate enrollmentReminderStartDate;
  private Boolean declineACall;
  private String declineACallComment;

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

  public String getSurveyId() {
    if (surveyId == ORAL_ADHERENCE_HT_PX && clinicCollect == true) {
      return ORAL_ADHERENCE_HT_CX;
    }
    if (surveyId == ORAL_ADHERENCE_PX && clinicCollect == true) {
      return ORAL_ADHERENCE_CX;
    }
    if (surveyId == HEALTH_TRACKER_PX && clinicCollect == true) {
      return HEALTH_TRACKER_CX;
    }
    return surveyId;
  }
}
