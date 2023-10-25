package com.navigatingcancer.healthtracker.api.rest;

import com.navigatingcancer.healthtracker.api.data.model.CheckInStatus;
import com.navigatingcancer.healthtracker.api.rest.representation.ScheduleCheckInDetailsResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Optional;
import org.springframework.web.bind.annotation.*;

@Tag(name = "schedules")
public interface CheckInScheduleAPI {
  @Operation(
      summary = "Provides check-ins and related data for a check-in schedule",
      operationId = "getScheduleCheckInDetailsByScheduleId")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "success"),
        @ApiResponse(responseCode = "404", description = "unknown schedule id"),
        @ApiResponse(responseCode = "405", description = "method not allowed")
      })
  @RequestMapping(value = "/schedules/{id}/checkins", method = RequestMethod.GET)
  ScheduleCheckInDetailsResponse getScheduleCheckInDetails(
      @PathVariable("id") String id,
      @RequestParam("checkInStatus") Optional<CheckInStatus> checkInStatus);

  @Operation(
      summary =
          "Provides check-ins and related data for a check-in schedule by id."
              + "For backward compatibility, if the schedule id is a valid check-in type,"
              + "the schedule is identified using that type.",
      operationId = "getScheduleCheckInDetailsByEnrollmentAndSchedule")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "success"),
        @ApiResponse(responseCode = "404", description = "unknown enrollment or schedule id")
      })
  @GetMapping(value = "/enrollments/{id}/schedules/{scheduleIdOrCheckInType}/checkins")
  ScheduleCheckInDetailsResponse getScheduleCheckInDetails(
      @PathVariable("id") String enrollmentId,
      @PathVariable("scheduleIdOrCheckInType") String scheduleIdOrCheckInType,
      @RequestParam("checkInStatus") Optional<CheckInStatus> checkInStatus);
}
