package com.navigatingcancer.healthtracker.api.data.repo.proReview;

import com.navigatingcancer.healthtracker.api.data.model.ProReview;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProReviewRepository extends MongoRepository<ProReview, String>, CustomProReviewRepository {
}
