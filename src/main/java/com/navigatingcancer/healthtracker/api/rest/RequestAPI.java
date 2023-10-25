package com.navigatingcancer.healthtracker.api.rest;

import com.navigatingcancer.healthtracker.api.rest.representation.PatientRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Tag(name = "request")
public interface RequestAPI {

  @Operation(
      summary = "",
      operationId = "requestCall",
      description = "",
      tags = {
        "request",
      })
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = ""),
        @ApiResponse(responseCode = "424", description = "")
      })
  @RequestMapping(value = "/request/call", method = RequestMethod.POST)
  void requestCallHandler(@Valid @RequestBody PatientRequest callRequest);

  @Operation(
      summary = "",
      operationId = "requestRefill",
      description = "",
      tags = {
        "request",
      })
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = ""),
        @ApiResponse(responseCode = "424", description = "")
      })
  @RequestMapping(value = "/request/refill", method = RequestMethod.POST)
  void requestRefillHandler(@Valid @RequestBody PatientRequest refillRequest);
}
