package com.navigatingcancer.healthtracker.api.data.service.impl;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.navigatingcancer.healthtracker.api.data.model.survey.SurveyInstance;
import com.navigatingcancer.healthtracker.api.data.repo.surveyInstance.SurveyInstanceRepository;
import com.navigatingcancer.healthtracker.api.data.service.SurveyService;
import com.navigatingcancer.healthtracker.api.rest.exception.RecordNotFoundException;
import com.navigatingcancer.json.utils.JsonUtils;
import java.io.IOException;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SurveyServiceImpl implements SurveyService {

  @Value("${survey.definition.bucket}")
  String surveyBucket;

  AmazonS3 client;
  @Autowired SurveyInstanceRepository surveyInstanceRepository;

  @Override
  @Deprecated
  public Map<?, ?> getDefinition(String fileName) throws IOException {
    return JsonUtils.fromJson(client().getObjectAsString(surveyBucket, fileName), Map.class);
  }

  @Override
  public SurveyInstance getSurveyInstance(String instanceId) {
    return surveyInstanceRepository
        .findById(instanceId)
        .orElseThrow(
            () ->
                new RecordNotFoundException(
                    String.format("Survey Instance with id not found %s", instanceId)));
  }

  @Override
  public SurveyInstance getSurveyInstance(
      Long clinicId, Long patientId, String surveyId, Map<String, Object> surveyParameters) {
    return surveyInstanceRepository.insertIgnore(
        new SurveyInstance(clinicId, patientId, surveyId, surveyParameters));
  }

  private AmazonS3 client() {
    if (client == null) {
      client = AmazonS3ClientBuilder.standard().build();
    }
    return client;
  }
}
