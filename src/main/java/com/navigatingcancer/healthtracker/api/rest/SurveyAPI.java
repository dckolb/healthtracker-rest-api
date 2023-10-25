package com.navigatingcancer.healthtracker.api.rest;

import com.navigatingcancer.healthtracker.api.rest.representation.SurveyInstanceResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.util.Map;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Tag(name = "survey")
public interface SurveyAPI {

  @Operation(
      summary = "",
      operationId = "surveyDefinition",
      description = "",
      tags = {
        "survey",
      })
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = ""),
        @ApiResponse(responseCode = "405", description = "")
      })
  @RequestMapping(value = "/survey/{fileName}", method = RequestMethod.GET)
  @Deprecated
  Map<?, ?> getDefinition(@PathVariable("fileName") String fileName) throws IOException;

  @Operation(
      summary = "",
      operationId = "surveyInstance",
      description = "",
      tags = {"survey", "surveyInstance"})
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = ""),
        @ApiResponse(responseCode = "404", description = "")
      })
  @RequestMapping(value = "/survey/instance/{instanceId}", method = RequestMethod.GET)
  SurveyInstanceResponse getSurveyInstance(@PathVariable("instanceId") String instanceId);
}
