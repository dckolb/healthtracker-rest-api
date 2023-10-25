package com.navigatingcancer.healthtracker.api.data.repo.surveyInstance;

import com.google.common.base.Preconditions;
import com.navigatingcancer.healthtracker.api.data.model.survey.SurveyInstance;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

public class CustomSurveyInstanceRepositoryImpl implements CustomSurveyInstanceRepository {
  private MongoTemplate template;

  public CustomSurveyInstanceRepositoryImpl(MongoTemplate template) {
    this.template = template;
  }

  public SurveyInstance findByHash(String hash) {
    Preconditions.checkArgument(
        !(hash == null || hash.isEmpty()), "null or empty hash provided to findByHash");
    Query query = new Query();
    query.addCriteria(Criteria.where("hash").is(hash));
    return this.template.findOne(query, SurveyInstance.class);
  }

  public SurveyInstance insertIgnore(SurveyInstance surveyInstance) {
    Preconditions.checkNotNull(surveyInstance, "surveyInstance is a required field");
    var existingInstance = findByHash(surveyInstance.getHash());
    if (existingInstance == null) {
      return this.template.insert(surveyInstance);
    }
    return existingInstance;
  }
}
