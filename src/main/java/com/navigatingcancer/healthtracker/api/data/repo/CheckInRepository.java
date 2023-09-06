package com.navigatingcancer.healthtracker.api.data.repo;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import com.navigatingcancer.healthtracker.api.data.model.CheckIn;
import com.navigatingcancer.healthtracker.api.data.model.CheckInStatus;
import com.navigatingcancer.healthtracker.api.data.model.CheckInType;

@Repository
public interface CheckInRepository extends MongoRepository<CheckIn, String> {

	List<CheckIn> findByEnrollmentId(String enrollmentId);


	List<CheckIn> findByEnrollmentIdOrderByScheduleDateDesc(String enrollmentId);

	Stream<CheckIn> findByEnrollmentIdAndCheckInTypeAndStatusOrderByScheduleDateDesc(String enrollmentId, CheckInType checkInType, CheckInStatus status);

	List<CheckIn> findByStatusNotAndEnrollmentIdOrderByScheduleDateDesc( CheckInStatus status, String enrollmentId);
	
	CheckIn findTopByEnrollmentIdAndCheckInTypeOrderByScheduleDateDesc(String enrollmentId, CheckInType checkInType);

	List<CheckIn> findByEnrollmentIdAndStatusOrderByScheduleDateDesc(String enrollmentId, CheckInStatus status);

	void deleteByEnrollmentId(String enrollmentId);

	@Query(value = "{enrollmentId : ?0, status: ?1}")
	List<CheckIn> findByStatus(String enrollmentId, CheckInStatus status);
	
	@Query(value = "{enrollmentId : ?0, status: ?1, checkInType: ?2}")
	List<CheckIn> findByStatusAndType(String enrollmentId, CheckInStatus status, CheckInType checkInType);

	@Query(value = "{enrollmentId : ?0, status: 'PENDING', scheduleDate: {$lte : ?1}}")
	List<CheckIn> findPastDue(String enrollmentId, LocalDate date);
	
	@Query(value = "{enrollmentId : ?0, status: ?1, checkInType: ?2}", count = true)
	Long countByStatus(String enrollmentId, CheckInStatus status, CheckInType checkInType);
	
	@Query(value = "{enrollmentId : ?0, status: 'COMPLETED', checkInType: ?1, medicationTaken: true}", count = true)
	Long medicationTaken(String enrollmentId, CheckInType checkInType);

	Stream<CheckIn> findTopByEnrollmentIdAndCheckInTypeAndStatusOrderByScheduleDateDesc(String id, CheckInType checkInType, CheckInStatus checkInStatus);

	Stream<CheckIn> findByEnrollmentIdAndCheckInTypeAndStatusAndScheduleDateGreaterThanOrderByScheduleDateDesc(String id, CheckInType checkInType, CheckInStatus checkInStatus, LocalDate scheduleDate);

}
