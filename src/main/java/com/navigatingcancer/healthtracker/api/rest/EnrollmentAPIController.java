package com.navigatingcancer.healthtracker.api.rest;

import com.navigatingcancer.healthtracker.api.data.auth.Identity;
import com.navigatingcancer.healthtracker.api.data.model.Enrollment;
import com.navigatingcancer.healthtracker.api.data.model.EnrollmentStatus;
import com.navigatingcancer.healthtracker.api.data.service.EnrollmentService;
import com.navigatingcancer.healthtracker.api.data.util.ValidatorUtils;
import com.navigatingcancer.healthtracker.api.rest.QueryParameters.EnrollmentQuery;
import com.navigatingcancer.healthtracker.api.rest.exception.BadDataException;
import com.navigatingcancer.healthtracker.api.rest.exception.MissingParametersException;
import com.navigatingcancer.healthtracker.api.rest.representation.EnrollmentIdentifiers;
import java.security.InvalidParameterException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import javax.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RestController;

@RestController("/enrollments")
@CrossOrigin(originPatterns = "*", allowCredentials = "true")
public class EnrollmentAPIController implements EnrollmentAPI {

  private static final Logger logger = LoggerFactory.getLogger(EnrollmentAPIController.class);

  private EnrollmentService enrollmentService;

  private Identity identity;

  @Autowired
  public EnrollmentAPIController(EnrollmentService enrollmentService, Identity identity) {
    this.enrollmentService = enrollmentService;
    this.identity = identity;
  }

  @Override
  public Enrollment getEnrollment(String id) {
    logger.debug("EnrollmentAPIController::getEnrollment");
    return enrollmentService.getEnrollment(id);
  }

  @Override
  public List<Enrollment> getEnrollments(
      List<Long> locationId,
      List<Long> clinicId,
      List<Long> patientId,
      List<EnrollmentStatus> status,
      LocalDate startDate,
      LocalDate endDate,
      Boolean all) {
    logger.debug("EnrollmentAPIController::getEnrollments");
    EnrollmentQuery q = new EnrollmentQuery();
    q.setPatientId(patientId);
    q.setClinicId(clinicId);
    q.setLocationId(locationId);
    q.setStatus(status);
    q.setStartDate(startDate);
    q.setEndDate(endDate);

    if (all != null && all) {
      q.setAll(true);
    }

    if (identity != null && identity.isSet()) {
      q.setPatientId(Arrays.asList(identity.getPatientId()));
      q.setClinicId(Arrays.asList(identity.getClinicId()));
      q.setLocationId(Arrays.asList(identity.getLocationId()));
    }

    if (!q.isValid()) {
      throw new MissingParametersException(
          "not a valid query. One of patientId, clinicId, locationId, status or date range must be"
              + " specified");
    }

    return enrollmentService.getEnrollments(q);
  }

  @Override
  public List<EnrollmentIdentifiers> getCurrentEnrollments(
      List<Long> locationIds,
      List<Long> providerIds,
      Boolean isManualCollect,
      List<Long> clinicIds) {
    logger.debug("EnrollmentAPIController::getCurrentEnrollments");
    return enrollmentService.getCurrentEnrollments(
        clinicIds, locationIds, providerIds, isManualCollect);
  }

  @Override
  public List<Enrollment> getEnrollmentsByIds(List<String> enrollmentIds) {
    logger.debug("EnrollmentAPIController::getEnrollmentsByIds");
    return enrollmentService.getEnrollmentsByIds(enrollmentIds);
  }

  @Override
  public Enrollment saveEnrollment(@Valid Enrollment enrollment, BindingResult result) {
    logger.debug("EnrollmentAPIController::saveEnrollment");
    ValidatorUtils.raiseValidationError(result);

    if (enrollment.getCycles() != null && enrollment.getCycles() != 0) {
      // If enrollment has a limited duration, make sure the end date is in not in the past
      enrollment.validateDates(); // make sure schedule start and end date are set
      LocalDate endDate = enrollment.getSchedulesLastDate();
      if (endDate != null && endDate.isBefore(LocalDate.now())) {
        throw new BadDataException("treatment end date must not be in the past");
      }
    }

    if (enrollment.getId() == null && identity != null && identity.isSet()) {
      enrollment.setPatientId(identity.getPatientId());
      enrollment.setLocationId(identity.getLocationId());
      enrollment.setClinicId(identity.getClinicId());
    }

    if (enrollment.getId() != null) {
      // mongo save will try to insert instead of update if version is null,
      // which causes a duplicate key exception
      if (enrollment.getVersion() == null) {
        throw new BadDataException("Id and version must both be specified for update");
      }

      return enrollmentService.updateEnrollment(enrollment);
    }

    return enrollmentService.createEnrollment(enrollment);
  }

  @Override
  public Enrollment changeStatus(String id, EnrollmentStatus status, String reason, String note)
      throws Exception {
    return enrollmentService.changeStatus(id, status, reason, note);
  }

  @Override
  public void resendConsentRequest(String id) throws Exception {
    this.enrollmentService.resendConsentRequest(id);
  }

  // Deprecated - use saveEnrollment instead.
  @Override
  public Enrollment updateEnrollment(
      String id, @Valid Enrollment enrollment, BindingResult result) {
    logger.debug("EnrollmentAPIController::updateEnrollment");
    ValidatorUtils.raiseValidationError(result);

    if (!id.equalsIgnoreCase(enrollment.getId())) {
      throw new InvalidParameterException("id's don't match");
    }

    if (enrollment.getCycles() != null && enrollment.getCycles() != 0) {
      // If enrollment has a limited duration, make sure the end date is in not in the past
      LocalDate endDate = enrollment.getSchedulesLastDate(); // Get the end date
      if (endDate != null) {
        if (endDate.isBefore(LocalDate.now())) {
          throw new BadDataException("treatment end date must not be in the past");
        }
      }
    }

    return enrollmentService.updateEnrollment(enrollment);
  }

  @Override
  public void rebuildSchedule(String id) {
    enrollmentService.rebuildSchedule(id);
  }
}
