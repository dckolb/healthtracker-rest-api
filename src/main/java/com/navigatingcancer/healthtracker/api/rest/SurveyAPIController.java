package com.navigatingcancer.healthtracker.api.rest;

import com.navigatingcancer.healthtracker.api.data.service.SurveyService;
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
  public Map<?, ?> getDefinition(String fileName) throws IOException {
    return surveyService.getDefinition(fileName);
  }
}
