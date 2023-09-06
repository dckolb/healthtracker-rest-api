package com.navigatingcancer.healthtracker.api.rest;

import com.navigatingcancer.healthtracker.api.data.auth.Identity;
import com.navigatingcancer.healthtracker.api.data.model.HealthTrackerStatus;
import com.navigatingcancer.healthtracker.api.data.service.ProReviewService;
import com.navigatingcancer.healthtracker.api.rest.representation.ProReviewResponse;
import com.navigatingcancer.healthtracker.api.rest.representation.ProReviewUpdateRequest;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
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

@Api(value = "pro-review")
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

  @ApiOperation(
      value = "Gets the Pro Review associated with the provided ID",
      nickname = "getProReview",
      notes = "Requires a valid 24 hex character object ID for the id",
      tags = {
        "proReview",
      })
  @ApiResponses(
      value = {
        @ApiResponse(code = 200, message = "Success", response = ProReviewResponse.class),
        @ApiResponse(code = 400, message = "Id is not valid object ID")
      })
  @RequestMapping(value = "/pro-review/{id}", method = RequestMethod.GET)
  public ProReviewResponse getProReviewHandler(
      @Valid @PathVariable(required = true, name = "id") @Pattern(regexp = "[a-f0-9]{24}")
          String id) {
    log.debug("ProReviewAPIController::getProReviewHandler {}", id);
    return this.proReviewService.getProReview(id);
  }

  @ApiOperation(
      value =
          "Handles the actions that need to trigger when a side panel pro review is submitted. "
              + " These can include, but are not limited to sending the pro to ehr, updating the"
              + " health tracker status category",
      nickname = "processProReview",
      notes = "Requires a valid 24 hex character object ID for the id",
      tags = {
        "proReview",
      })
  @ApiResponses(
      value = {
        @ApiResponse(code = 200, message = "Success", response = HealthTrackerStatus.class),
        @ApiResponse(code = 400, message = "Either the id, or request body is malformed"),
        @ApiResponse(code = 404, message = "Unable to find proReview with provided id")
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
