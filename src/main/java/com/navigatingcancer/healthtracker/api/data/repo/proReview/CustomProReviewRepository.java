package com.navigatingcancer.healthtracker.api.data.repo.proReview;

import com.navigatingcancer.healthtracker.api.data.model.EHRDelivery;
import com.navigatingcancer.healthtracker.api.data.model.ProReview;
import com.navigatingcancer.healthtracker.api.rest.exception.RecordNotFoundException;

public interface CustomProReviewRepository {
  public void updateEhrDeliveryById(String proId, EHRDelivery status)
      throws RecordNotFoundException;

  public ProReview upsertByLatestCheckInDate(ProReview proReview);
}
