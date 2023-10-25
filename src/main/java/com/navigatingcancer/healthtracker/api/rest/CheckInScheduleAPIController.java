package com.navigatingcancer.healthtracker.api.rest;

import com.navigatingcancer.healthtracker.api.data.model.CheckInStatus;
import com.navigatingcancer.healthtracker.api.data.model.CheckInType;
import com.navigatingcancer.healthtracker.api.data.service.CheckInScheduleService;
import com.navigatingcancer.healthtracker.api.rest.representation.ScheduleCheckInDetailsResponse;
import java.util.Optional;
import org.apache.commons.lang3.EnumUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CheckInScheduleAPIController implements CheckInScheduleAPI {
  private final CheckInScheduleService checkInScheduleService;

  @Autowired
  public CheckInScheduleAPIController(CheckInScheduleService checkInScheduleService) {
    this.checkInScheduleService = checkInScheduleService;
  }

  @Override
  public ScheduleCheckInDetailsResponse getScheduleCheckInDetails(
      String checkInScheduleId, Optional<CheckInStatus> checkInStatus) {
    return checkInScheduleService.getCheckInDetailsBySchedule(checkInScheduleId, checkInStatus);
  }

  @Override
  public ScheduleCheckInDetailsResponse getScheduleCheckInDetails(
      String enrollmentId, String scheduleIdOrCheckInType, Optional<CheckInStatus> checkInStatus) {
    var checkInType = EnumUtils.getEnumIgnoreCase(CheckInType.class, scheduleIdOrCheckInType);
    if (checkInType != null) {
      return checkInScheduleService.getCheckInDetailsBySchedule(
          enrollmentId, checkInType, checkInStatus);
    }

    return checkInScheduleService.getCheckInDetailsBySchedule(
        enrollmentId, scheduleIdOrCheckInType, checkInStatus);
  }
}
