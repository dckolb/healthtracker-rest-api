package com.navigatingcancer.healthtracker.api.rest;

import com.navigatingcancer.healthtracker.api.rest.representation.PatientRequest;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.validation.Valid;

@Api(value = "request")
public interface RequestAPI {

    @ApiOperation(value = "", nickname = "requestCall", notes = "", tags = { "request", })
    @ApiResponses(value = { @ApiResponse(code = 200, message = ""), @ApiResponse(code = 424, message = "") })
    @RequestMapping(value = "/request/call",
            method = RequestMethod.POST)
    void requestCallHandler(@Valid @RequestBody PatientRequest callRequest);

    @ApiOperation(value = "", nickname = "requestRefill", notes = "", tags = { "request", })
    @ApiResponses(value = { @ApiResponse(code = 200, message = ""), @ApiResponse(code = 424, message = "") })
    @RequestMapping(value = "/request/refill",
            method = RequestMethod.POST)
    void requestRefillHandler(@Valid @RequestBody PatientRequest refillRequest);
}
