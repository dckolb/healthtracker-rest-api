package com.navigatingcancer.healthtracker.api.rest;

import com.navigatingcancer.healthtracker.api.data.model.survey.SurveyInstance;
import com.navigatingcancer.healthtracker.api.data.service.SurveyService;
import com.navigatingcancer.healthtracker.api.rest.representation.SurveyInstanceResponse;
import java.io.IOException;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin(originPatterns = "*", allowCredentials = "true")
public class SurveyAPIController implements SurveyAPI {

  @Autowired SurveyService surveyService;

  @Override
  @Deprecated
  public Map<?, ?> getDefinition(String fileName) throws IOException {
    return surveyService.getDefinition(fileName);
  }

  public SurveyInstanceResponse getSurveyInstance(String instanceId) {
    SurveyInstance svInstance = surveyService.getSurveyInstance(instanceId);
    return new SurveyInstanceResponse(svInstance);
  }
}
