package com.navigatingcancer.healthtracker.api.data.repo.proReviewNote;

import com.navigatingcancer.healthtracker.api.data.model.ProReviewNote;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProReviewNoteRepository extends MongoRepository<ProReviewNote, String>, CustomProReviewNoteRepository {
}
