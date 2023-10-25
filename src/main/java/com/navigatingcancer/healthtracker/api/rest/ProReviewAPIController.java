package com.navigatingcancer.healthtracker.api.rest;

import com.navigatingcancer.healthtracker.api.data.auth.Identity;
import com.navigatingcancer.healthtracker.api.data.model.HealthTrackerStatus;
import com.navigatingcancer.healthtracker.api.data.service.ProReviewService;
import com.navigatingcancer.healthtracker.api.rest.representation.ProReviewResponse;
import com.navigatingcancer.healthtracker.api.rest.representation.ProReviewUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.validation.Valid;
import javax.validation.constraints.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "pro-review")
@Slf4j
@RestController
@Validated
@CrossOrigin(originPatterns = "*", allowCredentials = "true")
public class ProReviewAPIController {
  private ProReviewService proReviewService;

  @Autowired Identity identity;

  @Autowired
  public ProReviewAPIController(ProReviewService proReviewService) {
    this.proReviewService = proReviewService;
  }

  @Operation(
      summary = "Gets the Pro Review associated with the provided ID",
      operationId = "getProReview",
      description = "Requires a valid 24 hex character object ID for the id")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Success", useReturnTypeSchema = true),
        @ApiResponse(responseCode = "400", description = "Id is not valid object ID")
      })
  @RequestMapping(value = "/pro-review/{id}", method = RequestMethod.GET)
  public ProReviewResponse getProReviewHandler(
      @Valid @PathVariable(required = true, name = "id") @Pattern(regexp = "[a-f0-9]{24}")
          String id) {
    log.debug("ProReviewAPIController::getProReviewHandler {}", id);
    return this.proReviewService.getProReview(id);
  }

  @Operation(
      summary =
          "Handles the actions that need to trigger when a side panel pro review is submitted. "
              + " These can include, but are not limited to sending the pro to ehr, updating the"
              + " health tracker status category",
      operationId = "processProReview",
      description = "Requires a valid 24 hex character object ID for the id")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Success", useReturnTypeSchema = true),
        @ApiResponse(
            responseCode = "400",
            description = "Either the id, or request body is malformed"),
        @ApiResponse(
            responseCode = "404",
            description = "Unable to find proReview with provided id")
      })
  @RequestMapping(value = "/pro-review/{id}", method = RequestMethod.PATCH)
  public HealthTrackerStatus processProReviewHandler(
      @Valid @PathVariable(required = true, name = "id") @Pattern(regexp = "[a-f0-9]{24}")
          String id,
      @Valid @RequestBody ProReviewUpdateRequest request) {
    log.debug("ProReviewAPIController::processProReviewHandler {}", request);

    return proReviewService.processProReview(id, request, identity.getClinicianName());
  }
}
