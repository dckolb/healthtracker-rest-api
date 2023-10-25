package com.navigatingcancer.healthtracker.api.data.service;

import com.navigatingcancer.healthtracker.api.data.model.CheckInStatus;
import com.navigatingcancer.healthtracker.api.data.model.CheckInType;
import com.navigatingcancer.healthtracker.api.rest.representation.ScheduleCheckInDetailsResponse;
import java.util.Optional;

public interface CheckInScheduleService {

  /**
   * Retrieve check-in details by schedule by id.
   *
   * @param enrollmentId
   * @param scheduleId
   * @param checkInStatus
   * @return
   */
  ScheduleCheckInDetailsResponse getCheckInDetailsBySchedule(
      String enrollmentId, String scheduleId, Optional<CheckInStatus> checkInStatus);

  /**
   * Retrieve check-in details by schedule using the check-in type as a surrogate key for the
   * schedule. This is provided solely for backwards compatibility for schedules without ids.
   *
   * @param enrollmentId
   * @param checkInType
   * @param checkInStatus
   * @return
   */
  ScheduleCheckInDetailsResponse getCheckInDetailsBySchedule(
      String enrollmentId, CheckInType checkInType, Optional<CheckInStatus> checkInStatus);

  /**
   * Retrieve check-in details by schedule id.
   *
   * @param checkInScheduleId
   * @param checkInStatus
   * @return
   */
  ScheduleCheckInDetailsResponse getCheckInDetailsBySchedule(
      String checkInScheduleId, Optional<CheckInStatus> checkInStatus);
}
