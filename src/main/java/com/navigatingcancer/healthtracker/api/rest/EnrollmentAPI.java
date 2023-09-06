package com.navigatingcancer.healthtracker.api.rest;

import com.navigatingcancer.healthtracker.api.data.model.Enrollment;
import com.navigatingcancer.healthtracker.api.data.model.EnrollmentStatus;
import com.navigatingcancer.healthtracker.api.rest.representation.EnrollmentIdentifiers;
import io.swagger.annotations.*;
import java.time.LocalDate;
import java.util.List;
import javax.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Api(value = "enrollments")
public interface EnrollmentAPI {

  @ApiOperation(
      value = "",
      nickname = "getEnrollment",
      notes = "",
      tags = {
        "enrollment",
      })
  @ApiResponses(
      value = {@ApiResponse(code = 200, message = ""), @ApiResponse(code = 405, message = "")})
  @RequestMapping(value = "/enrollments/{id}", method = RequestMethod.GET)
  Enrollment getEnrollment(@PathVariable String id);

  @ApiOperation(
      value = "",
      nickname = "getEnrollmentsByIds",
      notes = "",
      tags = {
        "enrollment",
      })
  @ApiResponses(
      value = {@ApiResponse(code = 200, message = ""), @ApiResponse(code = 405, message = "")})
  @RequestMapping(value = "/enrollments/by-ids", method = RequestMethod.POST)
  List<Enrollment> getEnrollmentsByIds(@Valid @RequestBody List<String> enrollmentIds);

  @ApiOperation(
      value = "",
      nickname = "getEnrollments",
      notes = "",
      tags = {
        "enrollment",
      })
  @ApiResponses(
      value = {@ApiResponse(code = 200, message = ""), @ApiResponse(code = 405, message = "")})
  @RequestMapping(value = "/enrollments", method = RequestMethod.GET)
  List<Enrollment> getEnrollments(
      @RequestParam(required = false) List<Long> locationId,
      @RequestParam(required = false) List<Long> clinicId,
      @RequestParam(required = false) List<Long> patientId,
      @RequestParam(required = false) List<EnrollmentStatus> status,
      @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate startDate,
      @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate endDate,
      @RequestParam(required = false) Boolean all);

  @ApiOperation(
      value = "",
      nickname = "getCurrentEnrollments",
      notes = "",
      tags = {
        "enrollment",
      })
  @ApiResponses(
      value = {@ApiResponse(code = 200, message = ""), @ApiResponse(code = 405, message = "")})
  @RequestMapping(
      value = "/enrollments/current-ids",
      params = "clinicIds",
      method = RequestMethod.GET)
  List<EnrollmentIdentifiers> getCurrentEnrollments(
      @RequestParam(required = false) List<Long> locationIds,
      @RequestParam(required = false) List<Long> providerIds,
      @RequestParam(required = false) Boolean isManualCollect,
      @RequestParam List<Long> clinicIds);

  @ApiOperation(
      value = "Enroll a patient in HealthTracker",
      nickname = "addEnrollment",
      notes = "",
      tags = {
        "enrollment",
      })
  @ApiResponses(
      value = {
        @ApiResponse(code = 201, message = ""),
        @ApiResponse(code = 405, message = ""),
        @ApiResponse(code = 409, message = "")
      })
  @RequestMapping(value = "/enrollments", method = RequestMethod.POST)
  Enrollment saveEnrollment(@Valid @RequestBody Enrollment enrollment, BindingResult result);

  @ApiOperation(
      value = "Update a patient's enrollment in HealthTracker",
      nickname = "updateEnrollment",
      notes = "",
      tags = {
        "enrollment",
      })
  @ApiResponses(
      value = {
        @ApiResponse(code = 201, message = ""),
        @ApiResponse(code = 400, message = ""),
        @ApiResponse(code = 405, message = "")
      })
  @RequestMapping(value = "/enrollments/{id}", method = RequestMethod.PUT)
  Enrollment updateEnrollment(
      @PathVariable String id, @Valid @RequestBody Enrollment enrollment, BindingResult result);

  @ApiOperation(
      value = "Set status for patient's enrollment in HealthTracker",
      nickname = "changeStatus",
      notes = "",
      tags = {
        "enrollment",
      })
  @ApiResponses(
      value = {@ApiResponse(code = 201, message = ""), @ApiResponse(code = 405, message = "")})
  @RequestMapping(value = "/enrollments/{id}/status/{status}", method = RequestMethod.PUT)
  Enrollment changeStatus(
      @PathVariable String id,
      @PathVariable EnrollmentStatus status,
      @RequestParam(value = "reason", required = false) String reason,
      @RequestParam(value = "note", required = false) String note)
      throws Exception;

  @ApiOperation(value = "Resend consent request", nickname = "resendConsent", tags = ("enrollment"))
  @ApiResponses(
      value = {
        @ApiResponse(code = 200, message = ""),
        @ApiResponse(code = 404, message = "no consent found")
      })
  @PutMapping(value = "/enrollments/{id}/consent/resend")
  void resendConsentRequest(@PathVariable String id) throws Exception;

  @ApiOperation(
      value = "Force schedule rebuild",
      nickname = "rebuildSchedule",
      notes = "",
      tags = {
        "enrollment",
      })
  @ApiResponses(
      value = {
        @ApiResponse(code = 201, message = ""),
        @ApiResponse(code = 400, message = ""),
        @ApiResponse(code = 405, message = "")
      })
  @RequestMapping(value = "/enrollments/{id}/schedule/rebuild", method = RequestMethod.POST)
  void rebuildSchedule(@PathVariable String id);
}
