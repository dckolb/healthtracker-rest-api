package com.navigatingcancer.healthtracker.api.data.repo.proReviewActivity;

import com.navigatingcancer.healthtracker.api.data.model.ProReviewActivity;
import com.navigatingcancer.healthtracker.api.rest.exception.RecordNotFoundException;
import java.util.List;

public interface CustomProReviewActivityRepository {
  List<ProReviewActivity> getActivitiesByProReviewId(String id);

  void markActivitySynced(String proReviewActivityId, Long patientActivityId)
      throws RecordNotFoundException;
}
