package com.navigatingcancer.healthtracker.api.data.repo;

import com.navigatingcancer.healthtracker.api.data.model.CheckIn;
import com.navigatingcancer.healthtracker.api.data.model.CheckInStatus;
import com.navigatingcancer.healthtracker.api.data.model.CheckInType;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface CheckInRepository
    extends MongoRepository<CheckIn, String>, CustomCheckInRepository {

  List<CheckIn> findByEnrollmentId(String enrollmentId);

  List<CheckIn> findByEnrollmentIdOrderByScheduleDateDesc(String enrollmentId);

  Stream<CheckIn> findByEnrollmentIdAndCheckInTypeAndStatusOrderByScheduleDateDesc(
      String enrollmentId, CheckInType checkInType, CheckInStatus status);

  List<CheckIn> findByStatusNotAndEnrollmentIdOrderByScheduleDateDesc(
      CheckInStatus status, String enrollmentId);

  @Deprecated
  CheckIn findTopByEnrollmentIdAndCheckInTypeOrderByScheduleDateDesc(
      String enrollmentId, CheckInType checkInType);

  void deleteByEnrollmentId(String enrollmentId);

  @Query(value = "{enrollmentId : ?0, status: ?1}")
  List<CheckIn> findByStatus(String enrollmentId, CheckInStatus status);

  @Query(value = "{enrollmentId : ?0, status: 'PENDING', scheduleDate: {$lte : ?1}}")
  List<CheckIn> findPastDue(String enrollmentId, LocalDate date);

  @Deprecated
  Stream<CheckIn> findTopByEnrollmentIdAndCheckInTypeAndStatusOrderByScheduleDateDesc(
      String id, CheckInType checkInType, CheckInStatus checkInStatus);

  @Deprecated
  Stream<CheckIn>
      findByEnrollmentIdAndCheckInTypeAndStatusAndScheduleDateGreaterThanOrderByScheduleDateDesc(
          String id, CheckInType checkInType, CheckInStatus checkInStatus, LocalDate scheduleDate);
}
