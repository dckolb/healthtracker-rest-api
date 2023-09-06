package com.navigatingcancer.healthtracker.api.data.repo;

import java.util.List;

import com.navigatingcancer.healthtracker.api.data.model.CallAttempt;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

public interface CustomCallAttemptRepository {
    List<CallAttempt> getCallAttempts(List<String> checkInIds);
}