package com.navigatingcancer.healthtracker.api.rest;

import java.util.List;
import java.util.UUID;

import com.navigatingcancer.healthtracker.api.data.model.CallAttempt;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.ApiResponse;

@Api(value = "callattempts")
public interface CallAttemptAPI {
    @ApiOperation(value = "", nickname = "callattempt", notes = "", tags = { "callattempt", })
    @ApiResponses(value = { @ApiResponse(code = 200, message = ""), @ApiResponse(code = 405, message = "") })
    @RequestMapping(value = "/callattempts", method = RequestMethod.POST)
    CallAttempt saveCallAttempt(@RequestBody CallAttempt callAttempt);

    @ApiOperation(value = "", nickname = "callattempt", notes = "", tags = { "callattempt", })
    @ApiResponses(value = { @ApiResponse(code = 200, message = ""), @ApiResponse(code = 405, message = "") })
    @RequestMapping(value = "/callattempts", method = RequestMethod.GET)
    List<CallAttempt> getCallAttempts(@RequestParam(required = true) List<String> checkInIds);
}