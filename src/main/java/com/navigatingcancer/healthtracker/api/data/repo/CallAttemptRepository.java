package com.navigatingcancer.healthtracker.api.data.repo;

import java.util.UUID;

import com.navigatingcancer.healthtracker.api.data.model.CallAttempt;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CallAttemptRepository extends MongoRepository<CallAttempt, UUID>, CustomCallAttemptRepository {

}