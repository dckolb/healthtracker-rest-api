package com.navigatingcancer.healthtracker.api.data.repo;

import com.navigatingcancer.healthtracker.api.data.model.HealthTrackerStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface HealthTrackerStatusRepository extends MongoRepository<HealthTrackerStatus,String>, CustomHealthTrackerStatusRepository {
}
