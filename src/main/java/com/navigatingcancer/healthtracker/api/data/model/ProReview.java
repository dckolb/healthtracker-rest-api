package com.navigatingcancer.healthtracker.api.data.model;

import com.navigatingcancer.healthtracker.api.data.model.survey.SurveyPayload;
import java.time.LocalDate;
import java.util.List;
import lombok.*;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@Document(collection = "pro_reviews")
public class ProReview extends AbstractDocument {
  private Long clinicId;
  private String enrollmentId;
  private List<String> checkInIds;
  private SurveyPayload surveyPayload;
  private List<SideEffect> sideEffects;
  private List<Adherence> oralAdherence;
  private EHRDelivery ehrDelivery;
  private HealthTrackerStatus healthTrackerStatus;
  private List<Integer> patientActivityIds;
  private LocalDate mostRecentCheckInDate;

  // "unique key that supports grouping the review of multiple check-ins"
  @Indexed(sparse = true, unique = true)
  private String groupingKey;

  public ProReview(
      Long clinicId,
      String enrollmentId,
      List<String> checkInIds,
      SurveyPayload surveyPayload,
      List<SideEffect> sideEffects,
      List<Adherence> oralAdherence,
      HealthTrackerStatus healthTrackerStatus,
      LocalDate mostRecentCheckInDate) {
    this.clinicId = clinicId;
    this.enrollmentId = enrollmentId;
    this.checkInIds = checkInIds;
    this.surveyPayload = surveyPayload;
    this.sideEffects = sideEffects;
    this.oralAdherence = oralAdherence;
    this.healthTrackerStatus = healthTrackerStatus;
    this.mostRecentCheckInDate = mostRecentCheckInDate;
    setGroupingKey();
  }

  public ProReview(
      String id,
      Long clinicId,
      String enrollmentId,
      List<String> checkInIds,
      SurveyPayload surveyPayload,
      List<SideEffect> sideEffects,
      List<Adherence> oralAdherence,
      EHRDelivery ehrDelivery,
      HealthTrackerStatus healthTrackerStatus,
      List<Integer> patientActivityIds) {
    this.setId(id);
    this.clinicId = clinicId;
    this.enrollmentId = enrollmentId;
    this.checkInIds = checkInIds;
    this.surveyPayload = surveyPayload;
    this.sideEffects = sideEffects;
    this.oralAdherence = oralAdherence;
    this.ehrDelivery = ehrDelivery;
    this.healthTrackerStatus = healthTrackerStatus;
    this.patientActivityIds = patientActivityIds;
  }

  public void setGroupingKey() {
    //  If both fields are not set, don't set the unique key so the sparse index will ignore it
    if (this.getEnrollmentId() == null || this.getMostRecentCheckInDate() == null) {
      return;
    }

    this.groupingKey = enrollmentId + "+" + mostRecentCheckInDate.toString();
  }
}
