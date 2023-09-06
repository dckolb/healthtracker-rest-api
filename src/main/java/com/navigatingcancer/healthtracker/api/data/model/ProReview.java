package com.navigatingcancer.healthtracker.api.data.model;

import com.navigatingcancer.healthtracker.api.data.model.survey.SurveyPayload;
import java.util.List;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "pro_reviews")
public class ProReview {
  @Id private String id;

  private Long clinicId;
  private String enrollmentId;
  private List<String> checkInIds;
  private SurveyPayload surveyPayload;
  private List<SideEffect> sideEffects;
  private List<Adherence> oralAdherence;
  private EHRDelivery ehrDelivery;
  private HealthTrackerStatus healthTrackerStatus;
  private List<Integer> patientActivityIds;

  public ProReview(
      Long clinicId,
      String enrollmentId,
      List<String> checkInIds,
      SurveyPayload surveyPayload,
      List<SideEffect> sideEffects,
      List<Adherence> oralAdherence,
      HealthTrackerStatus healthTrackerStatus) {
    this.clinicId = clinicId;
    this.enrollmentId = enrollmentId;
    this.checkInIds = checkInIds;
    this.surveyPayload = surveyPayload;
    this.sideEffects = sideEffects;
    this.oralAdherence = oralAdherence;
    this.healthTrackerStatus = healthTrackerStatus;
  }
}
