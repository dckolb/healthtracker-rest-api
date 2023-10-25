package com.navigatingcancer.healthtracker.api.data.repo.proReviewActivity;

import com.navigatingcancer.healthtracker.api.data.model.ProReviewActivity;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ProReviewActivityRepository
    extends MongoRepository<ProReviewActivity, String>, CustomProReviewActivityRepository {}
