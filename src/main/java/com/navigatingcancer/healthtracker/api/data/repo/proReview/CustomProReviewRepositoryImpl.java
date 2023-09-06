package com.navigatingcancer.healthtracker.api.data.repo.proReview;

import com.mongodb.client.result.UpdateResult;
import com.navigatingcancer.healthtracker.api.data.model.EHRDelivery;
import com.navigatingcancer.healthtracker.api.data.model.ProReview;
import com.navigatingcancer.healthtracker.api.rest.exception.RecordNotFoundException;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

public class CustomProReviewRepositoryImpl implements CustomProReviewRepository {
  private final MongoTemplate template;

  @Autowired
  public CustomProReviewRepositoryImpl(MongoTemplate template) {
    this.template = template;
  }

  public void updateEhrDeliveryById(String proId, EHRDelivery status)
      throws RecordNotFoundException, IllegalArgumentException {
    if (proId == null || !ObjectId.isValid(proId) || status == null) {
      throw new IllegalArgumentException(
          "Can not update ehr delivery with invalid pro id or status");
    }

    Query query = new Query();
    Criteria criteria = Criteria.where("_id").is(new ObjectId(proId));
    query.addCriteria(criteria);
    Update update = new Update();
    update.set("ehrDelivery", status);
    UpdateResult result = this.template.updateFirst(query, update, ProReview.class);
    if (result.getModifiedCount() == 0l) {
      throw new RecordNotFoundException("Unable to find pro review with provided ID");
    }
  }

  public void appendPatientActivityId(String proReviewId, Integer patientActivityId)
      throws RecordNotFoundException, IllegalArgumentException {
    if (proReviewId == null || !ObjectId.isValid(proReviewId))
      throw new IllegalArgumentException("Can not update with invalid or null proReviewIds");

    if (patientActivityId == null)
      throw new IllegalArgumentException("Can not update with invalid patientActivity Ids");

    Query query = new Query();
    Criteria criteria = Criteria.where("_id").is(new ObjectId(proReviewId));
    query.addCriteria(criteria);
    Update update = new Update();
    update.addToSet("patientActivityIds", patientActivityId);
    this.template.updateFirst(query, update, ProReview.class);
  }
}
