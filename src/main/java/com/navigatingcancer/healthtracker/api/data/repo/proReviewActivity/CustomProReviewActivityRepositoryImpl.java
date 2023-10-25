package com.navigatingcancer.healthtracker.api.data.repo.proReviewActivity;

import com.google.common.base.Preconditions;
import com.navigatingcancer.healthtracker.api.data.model.ProReviewActivity;
import com.navigatingcancer.healthtracker.api.rest.exception.RecordNotFoundException;
import java.util.List;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

public class CustomProReviewActivityRepositoryImpl implements CustomProReviewActivityRepository {
  private MongoTemplate template;

  @Autowired
  public CustomProReviewActivityRepositoryImpl(MongoTemplate template) {
    this.template = template;
  }

  @Override
  public List<ProReviewActivity> getActivitiesByProReviewId(String id) {
    Preconditions.checkArgument(
        !(id == null || id.isEmpty()), "Null or empty id provided to getProReviewById");

    var query = new Query();
    query.addCriteria(Criteria.where("proReviewId").is(id));
    query.with(Sort.by(Sort.Direction.DESC, "createdDate"));
    return this.template.find(query, ProReviewActivity.class);
  }

  public void markActivitySynced(String proReviewActivityId, Long patientActivityId)
      throws RecordNotFoundException {
    Preconditions.checkArgument(
        proReviewActivityId != null && ObjectId.isValid(proReviewActivityId),
        "proReviewActivityId must be non-null and a valid object id");
    Preconditions.checkNotNull(patientActivityId, "patientActivityId must not be null");

    var query = new Query();
    query.addCriteria(Criteria.where("_id").is(new ObjectId(proReviewActivityId)));
    var update = new Update();
    update.set("patientActivityId", patientActivityId);

    var result = this.template.updateFirst(query, update, ProReviewActivity.class);
    if (result.getModifiedCount() == 0L) {
      throw new RecordNotFoundException("Unable to find pro_review_activity with provided ID");
    }
  }
}
