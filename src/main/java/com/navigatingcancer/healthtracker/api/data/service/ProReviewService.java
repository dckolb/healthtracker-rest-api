package com.navigatingcancer.healthtracker.api.data.service;

import com.navigatingcancer.healthtracker.api.data.model.HealthTrackerStatus;
import com.navigatingcancer.healthtracker.api.rest.representation.ProReviewResponse;
import com.navigatingcancer.healthtracker.api.rest.representation.ProReviewUpdateRequest;

public interface ProReviewService {
  ProReviewResponse getProReview(String id);

  HealthTrackerStatus processProReview(
      String proReviewId, ProReviewUpdateRequest request, String createdBy)
      throws IllegalArgumentException;

  void markEhrDelivered(String proReviewId, String createdBy, String documentId);
}
