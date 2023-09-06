package com.navigatingcancer.healthtracker.api.data.repo;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.navigatingcancer.healthtracker.api.data.model.Enrollment;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;


public interface EnrollmentRepository extends MongoRepository<Enrollment, String>, CustomEnrollmentRepository {
}
