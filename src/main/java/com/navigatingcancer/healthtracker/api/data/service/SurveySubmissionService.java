package com.navigatingcancer.healthtracker.api.data.service;

import com.navigatingcancer.healthtracker.api.data.model.survey.SurveySubmission;
import com.navigatingcancer.healthtracker.api.rest.exception.BadDataException;

public interface SurveySubmissionService {
  public String process(SurveySubmission submission) throws BadDataException;
}
