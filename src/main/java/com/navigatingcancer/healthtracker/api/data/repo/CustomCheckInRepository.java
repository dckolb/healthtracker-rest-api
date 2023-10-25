package com.navigatingcancer.healthtracker.api.data.repo;

import com.navigatingcancer.healthtracker.api.data.model.CheckIn;
import com.navigatingcancer.healthtracker.api.data.model.CheckInSchedule;
import com.navigatingcancer.healthtracker.api.data.model.CheckInStatus;
import com.navigatingcancer.healthtracker.api.data.model.CheckInType;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface CustomCheckInRepository {

  CheckIn upsertByNaturalKey(CheckIn ci);

  Integer getCompletedCount(String enrollmentId);

  Integer getTotalCount(String enrollmentId);

  float getAdherencePercent(String enrollmentId);

  Integer getTotalOralCount(String enrollmentId);

  Integer getTotalOralMedsTakenCount(String enrollmentId);

  List<String> findCheckIns(
      List<String> enrollmentIds, List<String> status, CheckInType checkInType);

  boolean isPending(String... enrollmentIds);

  Integer getMissedCheckins(String... enrollmentIds);

  List<CheckIn> getMissedCheckins(String enrollmentId, LocalDate fromDate);

  Long setMissedCheckins(String enrollmentId, LocalDate fromDate);

  /**
   * count how many checkins missed since the last completed checkin
   *
   * @param enrollmentId
   * @return
   */
  Long getLastMissedCheckinsCount(String enrollmentId);

  Long stopCheckins(String enrollmentId);

  /**
   * Given list of checkin IDs find the last one of a specific type
   *
   * @param checkInIds
   * @param type
   * @return
   */
  CheckIn getLastCheckinByType(List<String> checkInIds, CheckInType type);

  boolean hasCompletedACheckIn(Long patientId);

  void updateFieldsById(String checkInId, Map<String, Object> updates);

  List<CheckIn> findCheckInsBySchedule(String enrollmentId, CheckInSchedule schedule);

  long bulkUpdateFieldsByCheckInType(
      String enrollmentId,
      CheckInType checkInType,
      CheckInStatus checkInStatus,
      Map<String, Object> updates);
}
