package com.navigatingcancer.healthtracker.api.rest.representation;

import com.navigatingcancer.healthtracker.api.data.model.survey.SurveyInstance;
import java.util.Map;
import lombok.Data;

@Data
public class SurveyInstanceResponse {
  private Long patientId;
  private Long clinicId;
  private String surveyId;
  private Map<String, Object> surveyParameters;

  public SurveyInstanceResponse(SurveyInstance svInstance) {
    this.patientId = svInstance.getPatientId();
    this.clinicId = svInstance.getClinicId();
    this.surveyId = svInstance.getSurveyId();
    this.surveyParameters = svInstance.getSurveyParameters();
  }
}
