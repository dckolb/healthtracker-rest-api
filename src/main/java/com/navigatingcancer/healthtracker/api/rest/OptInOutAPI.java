package com.navigatingcancer.healthtracker.api.rest;

import com.navigatingcancer.healthtracker.api.data.model.OptInOut;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Tag(name = "optin")
public interface OptInOutAPI {
  @Operation(
      summary = "",
      operationId = "optinout_get",
      description = "",
      tags = {
        "optin",
      })
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = ""),
        @ApiResponse(responseCode = "405", description = "")
      })
  @RequestMapping(value = "/optin", method = RequestMethod.GET)
  List<OptInOut> getOptInOutRecords(
      Long clinicId, Long locationId, Long patientId, String action, String surveyId);

  @Operation(
      summary = "",
      operationId = "optinout_create",
      description = "",
      tags = {
        "optin",
      })
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = ""),
        @ApiResponse(responseCode = "405", description = "")
      })
  @RequestMapping(value = "/optin", method = RequestMethod.POST)
  OptInOut optInOut(@Valid @RequestBody OptInOut body);

  @Operation(
      summary = "",
      operationId = "optinout_status",
      description = "",
      tags = {
        "optin",
      })
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = ""),
        @ApiResponse(responseCode = "405", description = "")
      })
  @RequestMapping(value = "/optin/{patientId}/status", method = RequestMethod.GET)
  OptInOut getOptInOutStatus(Long clinicId, @PathVariable Long patientId, String surveyId);
}
