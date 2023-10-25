package com.navigatingcancer.healthtracker.api.data.repo.surveyInstance;

import com.navigatingcancer.healthtracker.api.data.model.survey.SurveyInstance;

public interface CustomSurveyInstanceRepository {
  SurveyInstance findByHash(String hash);

  SurveyInstance insertIgnore(SurveyInstance surveyInstance);
}
