package com.navigatingcancer.healthtracker.api.data.repo.proReview;

import com.google.common.base.Preconditions;
import com.mongodb.client.result.UpdateResult;
import com.navigatingcancer.healthtracker.api.data.model.EHRDelivery;
import com.navigatingcancer.healthtracker.api.data.model.ProReview;
import com.navigatingcancer.healthtracker.api.rest.exception.RecordNotFoundException;
import java.util.Set;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
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

    Query query = new Query(Criteria.where("_id").is(new ObjectId(proId)));
    Update update = new Update();
    update.set("ehrDelivery", status);
    UpdateResult result = this.template.updateFirst(query, update, ProReview.class);
    if (result.getModifiedCount() == 0l) {
      throw new RecordNotFoundException("Unable to find pro review with provided ID");
    }
  }

  public ProReview upsertByLatestCheckInDate(ProReview proReview) {
    Preconditions.checkArgument(proReview != null, "proReview can not be null");
    Query query =
        new Query(
            Criteria.where("mostRecentCheckInDate")
                .is(proReview.getMostRecentCheckInDate())
                .and("enrollmentId")
                .is(proReview.getEnrollmentId()));

    Document proReviewDocument = new Document();
    template.getConverter().write(proReview, proReviewDocument);
    FindAndModifyOptions options = new FindAndModifyOptions();
    options.upsert(true);
    options.returnNew(true);
    Update update = new Update();
    Set<String> pushOnlyFields =
        Set.of("checkInIds", "oralAdherence", "sideEffects", "surveyPayload");
    proReviewDocument
        .entrySet()
        .forEach(
            (entry) -> {
              if (!pushOnlyFields.contains(entry.getKey())) {
                update.setOnInsert(entry.getKey(), entry.getValue());
              }
            });

    update.setOnInsert("surveyPayload.content.enrollmentId", proReview.getEnrollmentId());

    if (proReview.getCheckInIds() != null) {
      update.push("checkInIds").each(proReview.getCheckInIds().toArray());
    }

    if (proReview.getSurveyPayload() != null && proReview.getSurveyPayload().getOral() != null) {
      update
          .push("surveyPayload.content.oral")
          .each(proReview.getSurveyPayload().getOral().toArray());
    }

    if (proReview.getSurveyPayload() != null
        && proReview.getSurveyPayload().getSymptoms() != null) {
      update
          .push("surveyPayload.content.symptoms")
          .each(proReview.getSurveyPayload().getSymptoms().toArray());
    }

    if (proReview.getOralAdherence() != null) {
      update.push("oralAdherence").each(proReview.getOralAdherence().toArray());
    }

    if (proReview.getSideEffects() != null) {
      update.push("sideEffects").each(proReview.getSideEffects().toArray());
    }

    // TODO: mongo 4.2+ does this by default.  Once upgraded this can be removed
    try {
      return template.findAndModify(query, update, options, ProReview.class);
    } catch (DuplicateKeyException e) {
      // retry to mitigate race condition resulting in DuplicateKeyException
      return template.findAndModify(query, update, options, ProReview.class);
    }
  }
}
