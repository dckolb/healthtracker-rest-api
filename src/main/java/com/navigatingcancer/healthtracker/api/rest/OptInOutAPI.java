package com.navigatingcancer.healthtracker.api.rest;

import java.util.List;

import javax.validation.Valid;

import com.navigatingcancer.healthtracker.api.data.model.OptInOut;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@Api(value = "optin")
public interface OptInOutAPI {
    @ApiOperation(value = "", nickname = "optinout_get", notes = "", tags = { "optin", })
    @ApiResponses(value = { @ApiResponse(code = 200, message = ""), @ApiResponse(code = 405, message = "") })
    @RequestMapping(value = "/optin", method = RequestMethod.GET)
    List<OptInOut> getOptInOutRecords(Long clinicId, Long locationId, Long patientId, String action, String surveyId);

    @ApiOperation(value = "", nickname = "optinout_create", notes = "", tags = { "optin", })
    @ApiResponses(value = { @ApiResponse(code = 200, message = ""), @ApiResponse(code = 405, message = "") })
    @RequestMapping(value = "/optin", method = RequestMethod.POST)
    OptInOut optInOut(@Valid @RequestBody OptInOut body);

    @ApiOperation(value = "", nickname = "optinout_status", notes = "", tags = { "optin", })
    @ApiResponses(value = { @ApiResponse(code = 200, message = ""), @ApiResponse(code = 405, message = "") })
    @RequestMapping(value = "/optin/{patientId}/status", method = RequestMethod.GET)
    OptInOut getOptInOutStatus(Long clinicId, @PathVariable Long patientId, String surveyId);
}