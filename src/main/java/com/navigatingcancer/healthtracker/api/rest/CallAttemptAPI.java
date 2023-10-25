package com.navigatingcancer.healthtracker.api.rest;

import com.navigatingcancer.healthtracker.api.data.model.CallAttempt;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "callattempts")
public interface CallAttemptAPI {
  @Operation(summary = "", operationId = "callattempt")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = ""),
        @ApiResponse(responseCode = "405", description = "")
      })
  @RequestMapping(value = "/callattempts", method = RequestMethod.POST)
  CallAttempt saveCallAttempt(@RequestBody CallAttempt callAttempt);

  @Operation(summary = "", operationId = "callattempt")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = ""),
        @ApiResponse(responseCode = "405", description = "")
      })
  @RequestMapping(value = "/callattempts", method = RequestMethod.GET)
  List<CallAttempt> getCallAttempts(@RequestParam(required = true) List<String> checkInIds);
}
