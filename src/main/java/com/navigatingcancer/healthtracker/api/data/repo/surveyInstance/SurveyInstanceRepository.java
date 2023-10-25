package com.navigatingcancer.healthtracker.api.data.repo.surveyInstance;

import com.navigatingcancer.healthtracker.api.data.model.survey.SurveyInstance;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SurveyInstanceRepository
    extends MongoRepository<SurveyInstance, String>, CustomSurveyInstanceRepository {}
