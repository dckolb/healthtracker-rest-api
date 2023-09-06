package com.navigatingcancer.healthtracker.api.rest;

import com.navigatingcancer.healthtracker.api.data.model.CheckIn;
import com.navigatingcancer.healthtracker.api.data.model.CheckInData;
import com.navigatingcancer.healthtracker.api.data.model.PracticeCheckIn;
import com.navigatingcancer.healthtracker.api.data.model.survey.SurveyPayload;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@Api(value = "checkins")
public interface CheckInAPI {

	@ApiOperation(value = "", nickname = "allCheckinsData", notes = "", tags = { "checkin", })
	@ApiResponses(value = { @ApiResponse(code = 200, message = ""), @ApiResponse(code = 405, message = "") })
	@RequestMapping(value = "/checkins/by-ids",
			method = RequestMethod.POST)
	List<CheckInData> getCheckInDataByEnrollmentIDs(@Valid @RequestBody List<String> enrollmentIds);

	@ApiOperation(value = "", nickname = "checkinsData", notes = "", tags = { "checkin", })
	@ApiResponses(value = { @ApiResponse(code = 200, message = ""), @ApiResponse(code = 405, message = "") })
	@RequestMapping(value = "/checkins", method = RequestMethod.GET)
	CheckInData getCheckData(@RequestParam(required = false) List<Long> locationId,
			@RequestParam(required = false) List<Long> clinicId, @RequestParam(required = false) List<Long> patientId);
	
	@ApiOperation(value = "", nickname = "checkinsDataById", notes = "", tags = { "checkin", })
	@ApiResponses(value = { @ApiResponse(code = 200, message = ""), @ApiResponse(code = 405, message = "") })
	@RequestMapping(value = "/checkins/{id}", method = RequestMethod.GET)
	CheckInData getCheckData(@PathVariable("id") String enrollmentId);

	@ApiOperation(value = "", nickname = "checkin", notes = "", tags = { "checkin", })
	@ApiResponses(value = { @ApiResponse(code = 200, message = ""), @ApiResponse(code = 405, message = "") })
	@RequestMapping(value = "/checkins", method = RequestMethod.POST)
	void checkIn(@RequestBody SurveyPayload surveyPayload);

	@ApiOperation(value = "", nickname = "checkinBackfill", notes = "", tags = { "checkin", })
	@ApiResponses(value = { @ApiResponse(code = 200, message = ""), @ApiResponse(code = 405, message = "") })
	@RequestMapping(value = "/checkins/backfill", method = RequestMethod.POST)
	CheckIn checkInBackfill(@RequestBody CheckIn checkInBackfill);

	@ApiOperation(value = "", nickname = "remindMeLater", notes = "", tags = { "checkin", })
	@ApiResponses(value = { @ApiResponse(code = 200, message = ""), @ApiResponse(code = 405, message = "") })
	@RequestMapping(value = "/checkins/remindMeLater", method = RequestMethod.POST)
	void remindMeLater(@RequestParam String enrollmentId, @RequestParam Integer minutes);

	@ApiOperation(value = "", nickname = "remindMeNow", notes = "", tags = { "checkin", })
	@ApiResponses(value = { @ApiResponse(code = 200, message = ""), @ApiResponse(code = 405, message = "") })
	@RequestMapping(value = "/checkins/remindMeNow", method = RequestMethod.POST)
	void remindMeNow(@RequestParam String enrollmentId);

	@ApiOperation(value = "", nickname = "", notes = "", tags = { "checkin", })
	@ApiResponses(value = { @ApiResponse(code = 200, message = ""), @ApiResponse(code = 405, message = "") })
	@RequestMapping(value = "/checkins/practice_checkins", method = RequestMethod.GET)
	PracticeCheckIn getPracticeCheckInData(@RequestParam Long clinicId, @RequestParam Long patientId);

	@ApiOperation(value = "", nickname = "", notes = "", tags = { "checkin", })
	@ApiResponses(value = { @ApiResponse(code = 200, message = ""), @ApiResponse(code = 405, message = "") })
	@RequestMapping(value = "/checkins/practice_checkins", method = RequestMethod.POST)
	PracticeCheckIn persistPracticeCheckInData(@RequestBody PracticeCheckIn practiceCheckIn);
}
