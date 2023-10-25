package com.navigatingcancer.healthtracker.api.rest;

import com.navigatingcancer.healthtracker.api.data.model.CheckIn;
import com.navigatingcancer.healthtracker.api.data.model.CheckInData;
import com.navigatingcancer.healthtracker.api.data.model.CheckInStatus;
import com.navigatingcancer.healthtracker.api.data.model.PracticeCheckIn;
import com.navigatingcancer.healthtracker.api.data.model.survey.SurveyPayload;
import com.navigatingcancer.healthtracker.api.rest.representation.CheckInResponse;
import com.navigatingcancer.healthtracker.api.rest.representation.OnDemandCheckInRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import javax.validation.Valid;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Tag(name = "checkins")
public interface CheckInAPI {

  @Operation(summary = "", operationId = "allCheckinsData")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = ""),
        @ApiResponse(responseCode = "405", description = "")
      })
  @RequestMapping(value = "/checkins/by-ids", method = RequestMethod.POST)
  List<CheckInData> getCheckInDataByEnrollmentIDs(@Valid @RequestBody List<String> enrollmentIds);

  @Operation(summary = "", operationId = "checkinsData")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = ""),
        @ApiResponse(responseCode = "405", description = "")
      })
  @RequestMapping(value = "/checkins", method = RequestMethod.GET)
  CheckInData getCheckData(
      @RequestParam(required = false) List<Long> locationId,
      @RequestParam(required = false) List<Long> clinicId,
      @RequestParam(required = false) List<Long> patientId);

  @Operation(summary = "", operationId = "checkinsDataById")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = ""),
        @ApiResponse(responseCode = "405", description = "")
      })
  @RequestMapping(value = "/checkins/{id}", method = RequestMethod.GET)
  CheckInData getCheckData(@PathVariable("id") String enrollmentId);

  @Operation(summary = "", operationId = "checkin")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = ""),
        @ApiResponse(responseCode = "405", description = "")
      })
  @RequestMapping(value = "/checkins", method = RequestMethod.POST)
  void checkIn(@RequestBody SurveyPayload surveyPayload);

  @Operation(summary = "", operationId = "checkinBackfill")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = ""),
        @ApiResponse(responseCode = "405", description = "")
      })
  @RequestMapping(value = "/checkins/backfill", method = RequestMethod.POST)
  CheckIn checkInBackfill(@RequestBody CheckIn checkInBackfill);

  @Operation(summary = "", operationId = "remindMeLater")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = ""),
        @ApiResponse(responseCode = "405", description = "")
      })
  @RequestMapping(value = "/checkins/remindMeLater", method = RequestMethod.POST)
  void remindMeLater(@RequestParam String enrollmentId, @RequestParam Integer minutes);

  @Operation(summary = "", operationId = "remindMeNow")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = ""),
        @ApiResponse(responseCode = "405", description = "")
      })
  @RequestMapping(value = "/checkins/remindMeNow", method = RequestMethod.POST)
  void remindMeNow(@RequestParam String enrollmentId);

  @Operation(summary = "", operationId = "getPracticeCheckInData")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = ""),
        @ApiResponse(responseCode = "405", description = "")
      })
  @RequestMapping(value = "/checkins/practice_checkins", method = RequestMethod.GET)
  PracticeCheckIn getPracticeCheckInData(@RequestParam Long clinicId, @RequestParam Long patientId);

  @Operation(summary = "", operationId = "persistPracticeCheckInData")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = ""),
        @ApiResponse(responseCode = "405", description = "")
      })
  @RequestMapping(value = "/checkins/practice_checkins", method = RequestMethod.POST)
  PracticeCheckIn persistPracticeCheckInData(@RequestBody PracticeCheckIn practiceCheckIn);

  @RequestMapping(value = "/checkins/by-enrollment-and-status", method = RequestMethod.GET)
  List<CheckInResponse> getCheckInsByEnrollmentAndStatus(
      @RequestParam String enrollmentId, CheckInStatus checkInStatus);

  @Operation(summary = "Create a check-in on demand", operationId = "createOnDemandCheckIn")
  @ApiResponses(value = {@ApiResponse(responseCode = "200"), @ApiResponse(responseCode = "400")})
  @PostMapping("/checkins/on-demand")
  CheckInResponse createOnDemandCheckIn(
      @RequestBody OnDemandCheckInRequest request, BindingResult bindingResult);
}
