package com.navigatingcancer.healthtracker.api.rest;

import com.navigatingcancer.healthtracker.api.data.model.survey.SurveySubmission;
import com.navigatingcancer.healthtracker.api.data.service.SurveySubmissionService;
import com.navigatingcancer.healthtracker.api.rest.representation.SurveySubmissionResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "survey-submission")
@RestController
@Validated
@CrossOrigin(originPatterns = "*", allowCredentials = "true")
public class SurveySubmissionController {
  private SurveySubmissionService surveySubmissionService;

  public SurveySubmissionController(SurveySubmissionService surveySubmissionService) {
    this.surveySubmissionService = surveySubmissionService;
  }

  @PostMapping(value = "/survey-submission")
  public SurveySubmissionResponse submitSurvey(@RequestBody SurveySubmission submission) {
    String message = this.surveySubmissionService.process(submission);
    return new SurveySubmissionResponse(message);
  }
}
