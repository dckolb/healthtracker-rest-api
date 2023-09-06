package com.navigatingcancer.healthtracker.api.rest;

import com.navigatingcancer.healthtracker.api.data.model.HealthTrackerStatus;
import com.navigatingcancer.healthtracker.api.data.model.HealthTrackerStatusCategory;
import com.navigatingcancer.healthtracker.api.data.service.StatusService;
import com.navigatingcancer.healthtracker.api.processor.HealthTrackerStatusService;
import com.navigatingcancer.healthtracker.api.rest.representation.HealthTrackerStatusResponse;
import com.navigatingcancer.healthtracker.api.rest.representation.StatusRequest;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.List;
import javax.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin(originPatterns = "*", allowCredentials = "true")
@Slf4j
public class HealthTrackerStatusController {

  private HealthTrackerStatusService healthTrackerStatusService;

  private StatusService statusService;

  @Autowired
  public HealthTrackerStatusController(
      StatusService statusService, HealthTrackerStatusService healthTrackerStatusService) {
    this.statusService = statusService;
    this.healthTrackerStatusService = healthTrackerStatusService;
  }

  @GetMapping("/status")
  // Add patientIds filter here ?
  public List<HealthTrackerStatus> getStatus(
      @RequestParam(required = false) List<Long> clinicId,
      @RequestParam(required = false) List<Long> locationIds,
      @RequestParam(required = false) List<Long> patientIds) {
    return healthTrackerStatusService.getOrCreateNewStatus(clinicId, locationIds, patientIds);
  }

  @GetMapping("/status/{id}")
  public HealthTrackerStatus getById(@PathVariable("id") String id) {
    log.debug("HealthTrackerStatusController::getById");
    return healthTrackerStatusService.getById(id);
  }

  @PostMapping("/status/{id}/run")
  public void run(@PathVariable("id") String id) {
    log.debug("HealthTrackerStatusController::run");
    healthTrackerStatusService.push(id, null);
  }

  @Deprecated
  @ApiOperation(
      value = "",
      nickname = "getStatusByIds",
      notes = "",
      tags = {
        "status",
      })
  @ApiResponses(
      value = {@ApiResponse(code = 200, message = ""), @ApiResponse(code = 405, message = "")})
  @RequestMapping(value = "/status", params = "ids", method = RequestMethod.GET)
  public List<HealthTrackerStatus> getByIds(
      @RequestParam Long clinicId, @RequestParam List<String> ids) {
    log.debug("HealthTrackerStatusController::getByIds");
    return this.statusService.getByIds(clinicId, ids);
  }

  @ApiOperation(
      value = "",
      nickname = "getStatusByIds",
      notes = "",
      tags = {
        "status",
      })
  @ApiResponses(
      value = {@ApiResponse(code = 200, message = ""), @ApiResponse(code = 405, message = "")})
  @RequestMapping(value = "/status", method = RequestMethod.POST)
  public List<HealthTrackerStatus> findByIds(@Valid @RequestBody StatusRequest statusRequest) {
    log.debug("HealthTrackerStatusController::getByIds");
    return this.statusService.getByIds(statusRequest.getClinicId(), statusRequest.getIds());
  }

  @ApiOperation(
      value = "",
      nickname = "statusDue",
      notes = "",
      tags = {
        "status",
      })
  @ApiResponses(
      value = {@ApiResponse(code = 200, message = ""), @ApiResponse(code = 405, message = "")})
  @RequestMapping(value = "/status/due", method = RequestMethod.POST)
  public List<HealthTrackerStatusResponse> getHealthTrackerDue(
      @Valid @RequestBody StatusRequest statusRequest) {
    log.debug("HealthTrackerStatusController::getHealthTrackerDue");
    log.debug("status Request {}", statusRequest);
    return this.statusService.getManualCollectDueByIds(
        statusRequest.getClinicId(), statusRequest.getIds());
  }

  @ApiOperation(
      value = "",
      nickname = "setStatusCategory",
      notes = "",
      tags = {
        "status",
      })
  @ApiResponses(
      value = {@ApiResponse(code = 200, message = ""), @ApiResponse(code = 405, message = "")})
  @RequestMapping(value = "/status/{id}/category", method = RequestMethod.POST)
  public HealthTrackerStatus setStatusCategory(
      @PathVariable("id") String id,
      @RequestParam(required = false) List<String> checkinId,
      @Valid @RequestBody HealthTrackerStatusCategory category) {
    log.debug("HealthTrackerStatusController::setStatus");
    return healthTrackerStatusService.setCategory(id, category, checkinId);
  }
}
