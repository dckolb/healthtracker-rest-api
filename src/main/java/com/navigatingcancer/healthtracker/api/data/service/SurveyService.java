package com.navigatingcancer.healthtracker.api.data.service;

import com.navigatingcancer.healthtracker.api.data.model.survey.SurveyInstance;
import java.io.IOException;
import java.util.Map;

public interface SurveyService {
  @Deprecated
  Map<?, ?> getDefinition(String fileName) throws IOException;

  SurveyInstance getSurveyInstance(String instanceId);

  /**
   * Return a survey instance with the provided parameters, creating one if it doesn't exist.
   *
   * @param clinicId
   * @param patientId
   * @param surveyId
   * @param surveyParameters
   * @return
   */
  SurveyInstance getSurveyInstance(
      Long clinicId, Long patientId, String surveyId, Map<String, Object> surveyParameters);
}
