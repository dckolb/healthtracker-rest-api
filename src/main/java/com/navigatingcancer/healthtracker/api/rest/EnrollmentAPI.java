package com.navigatingcancer.healthtracker.api.rest;

import com.navigatingcancer.healthtracker.api.data.model.Enrollment;
import com.navigatingcancer.healthtracker.api.data.model.EnrollmentStatus;
import com.navigatingcancer.healthtracker.api.rest.representation.EnrollmentIdentifiers;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.List;
import javax.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Tag(name = "enrollments")
public interface EnrollmentAPI {

  @Operation(summary = "", operationId = "getEnrollment", description = "")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = ""),
        @ApiResponse(responseCode = "405", description = "")
      })
  @RequestMapping(value = "/enrollments/{id}", method = RequestMethod.GET)
  Enrollment getEnrollment(@PathVariable String id);

  @Operation(summary = "", operationId = "getEnrollmentsByIds", description = "")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = ""),
        @ApiResponse(responseCode = "405", description = "")
      })
  @RequestMapping(value = "/enrollments/by-ids", method = RequestMethod.POST)
  List<Enrollment> getEnrollmentsByIds(@Valid @RequestBody List<String> enrollmentIds);

  @Operation(summary = "", operationId = "getEnrollments", description = "")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = ""),
        @ApiResponse(responseCode = "405", description = "")
      })
  @RequestMapping(value = "/enrollments", method = RequestMethod.GET)
  List<Enrollment> getEnrollments(
      @RequestParam(required = false) List<Long> locationId,
      @RequestParam(required = false) List<Long> clinicId,
      @RequestParam(required = false) List<Long> patientId,
      @RequestParam(required = false) List<EnrollmentStatus> status,
      @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate startDate,
      @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate endDate,
      @RequestParam(required = false) Boolean all);

  @Operation(summary = "", operationId = "getCurrentEnrollments")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = ""),
        @ApiResponse(responseCode = "405", description = "")
      })
  @RequestMapping(
      value = "/enrollments/current-ids",
      params = "clinicIds",
      method = RequestMethod.GET)
  List<EnrollmentIdentifiers> getCurrentEnrollments(
      @RequestParam(required = false) List<Long> locationIds,
      @RequestParam(required = false) List<Long> providerIds,
      @RequestParam(required = false) Boolean isManualCollect,
      @RequestParam List<Long> clinicIds);

  @Operation(summary = "Enroll a patient in HealthTracker", operationId = "addEnrollment")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "201", description = ""),
        @ApiResponse(responseCode = "405", description = ""),
        @ApiResponse(responseCode = "409", description = "")
      })
  @RequestMapping(value = "/enrollments", method = RequestMethod.POST)
  Enrollment saveEnrollment(@Valid @RequestBody Enrollment enrollment, BindingResult result);

  @Operation(
      summary = "Update a patient's enrollment in HealthTracker",
      operationId = "updateEnrollment")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "201", description = ""),
        @ApiResponse(responseCode = "400", description = ""),
        @ApiResponse(responseCode = "405", description = "")
      })
  @RequestMapping(value = "/enrollments/{id}", method = RequestMethod.PUT)
  Enrollment updateEnrollment(
      @PathVariable String id, @Valid @RequestBody Enrollment enrollment, BindingResult result);

  @Operation(
      summary = "Set status for patient's enrollment in HealthTracker",
      operationId = "changeStatus")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "201", description = ""),
        @ApiResponse(responseCode = "405", description = "")
      })
  @RequestMapping(value = "/enrollments/{id}/status/{status}", method = RequestMethod.PUT)
  Enrollment changeStatus(
      @PathVariable String id,
      @PathVariable EnrollmentStatus status,
      @RequestParam(value = "reason", required = false) String reason,
      @RequestParam(value = "note", required = false) String note)
      throws Exception;

  @Operation(summary = "Resend consent request", operationId = "resendConsent")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = ""),
        @ApiResponse(responseCode = "404", description = "no consent found")
      })
  @PutMapping(value = "/enrollments/{id}/consent/resend")
  void resendConsentRequest(@PathVariable String id) throws Exception;

  @Operation(summary = "Force schedule rebuild", operationId = "rebuildSchedule")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "201", description = ""),
        @ApiResponse(responseCode = "400", description = ""),
        @ApiResponse(responseCode = "405", description = "")
      })
  @RequestMapping(value = "/enrollments/{id}/schedule/rebuild", method = RequestMethod.POST)
  void rebuildSchedule(@PathVariable String id);
}
