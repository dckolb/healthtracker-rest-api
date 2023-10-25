package com.navigatingcancer.healthtracker.api.rest;

import com.navigatingcancer.healthtracker.api.data.auth.Identity;
import com.navigatingcancer.healthtracker.api.data.model.*;
import com.navigatingcancer.healthtracker.api.data.model.survey.SurveyPayload;
import com.navigatingcancer.healthtracker.api.data.repo.HealthTrackerEventsRepository;
import com.navigatingcancer.healthtracker.api.data.service.CheckInCreationService;
import com.navigatingcancer.healthtracker.api.data.service.CheckInService;
import com.navigatingcancer.healthtracker.api.data.service.EnrollmentService;
import com.navigatingcancer.healthtracker.api.data.service.SurveyService;
import com.navigatingcancer.healthtracker.api.data.service.impl.NotificationService;
import com.navigatingcancer.healthtracker.api.data.service.impl.SchedulingServiceImpl;
import com.navigatingcancer.healthtracker.api.data.util.ValidatorUtils;
import com.navigatingcancer.healthtracker.api.rest.QueryParameters.EnrollmentQuery;
import com.navigatingcancer.healthtracker.api.rest.exception.BadDataException;
import com.navigatingcancer.healthtracker.api.rest.representation.CheckInResponse;
import com.navigatingcancer.healthtracker.api.rest.representation.OnDemandCheckInRequest;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import javax.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RestController;

@RestController("/checkins")
@CrossOrigin(originPatterns = "*", allowCredentials = "true")
@Slf4j
public class CheckInAPIController implements CheckInAPI {

  @Autowired private CheckInService checkInService;

  @Autowired private SchedulingServiceImpl schedulingService;

  @Autowired private Identity identity;
  @Autowired private CheckInCreationService checkInCreationService;
  @Autowired private EnrollmentService enrollmentService;
  @Autowired private NotificationService notificationService;
  @Autowired private HealthTrackerEventsRepository eventsRepository;
  @Autowired private SurveyService surveyService;

  @Override
  public List<CheckInData> getCheckInDataByEnrollmentIDs(List<String> enrollmentIds) {
    return checkInService.getCheckInDataByEnrollmentIDs(enrollmentIds);
  }

  @Override
  public CheckInData getCheckData(
      List<Long> locationId, List<Long> clinicId, List<Long> patientId) {
    EnrollmentQuery query = new EnrollmentQuery();
    query.setPatientId(patientId);
    query.setClinicId(clinicId);
    query.setLocationId(locationId);
    query.setStatus(Arrays.asList(EnrollmentStatus.ACTIVE));

    if (identity.isSet()) {
      query.setPatientId(Arrays.asList(identity.getPatientId()));
      query.setClinicId(Arrays.asList(identity.getClinicId()));
      query.setLocationId(Arrays.asList(identity.getLocationId()));
    }

    return checkInService.getCheckInData(query);
  }

  @Override
  public CheckInData getCheckData(String enrollmentId) {
    return checkInService.getCheckInData(enrollmentId);
  }

  @Override
  public void checkIn(SurveyPayload surveyPayload) {
    checkInService.checkIn(surveyPayload);
  }

  @Override
  public void remindMeLater(String enrollmentId, Integer minutes) {
    log.info("requested checkin reminder for enrollmentId {} later by {}", enrollmentId, minutes);
    schedulingService.remindMeLater(enrollmentId, minutes);
  }

  @Override
  public void remindMeNow(String enrollmentId) {
    log.info("requested checkin reminder for enrollmentId {}", enrollmentId);
    schedulingService.remindMeNow(enrollmentId);
  }

  @Override
  public CheckIn checkInBackfill(CheckIn checkIn) {
    return checkInService.checkInBackfill(checkIn);
  }

  @Override
  public PracticeCheckIn getPracticeCheckInData(Long clinicId, Long patientId) {
    return checkInService.getPracticeCheckInData(clinicId, patientId);
  }

  @Override
  public PracticeCheckIn persistPracticeCheckInData(PracticeCheckIn practiceCheckIn) {
    return checkInService.persistPracticeCheckInData(practiceCheckIn);
  }

  @Override
  public List<CheckInResponse> getCheckInsByEnrollmentAndStatus(
      String enrollmentId, CheckInStatus checkInStatus) {
    return checkInService.getCheckInsByEnrollmentAndStatus(enrollmentId, checkInStatus, false);
  }

  @Override
  public CheckInResponse createOnDemandCheckIn(
      @Valid OnDemandCheckInRequest request, BindingResult bindingResult) {
    ValidatorUtils.raiseValidationError(bindingResult);

    if (request.getReason() != ReasonForCheckInCreation.CARE_TEAM_REQUESTED
        && request.getReason() != ReasonForCheckInCreation.PATIENT_REQUESTED) {
      throw new BadDataException("unsupported check-in creation reason");
    }

    // use query validate patient and clinic id and enrollment match identity
    var query = new EnrollmentQuery();
    query.setId(List.of(request.getEnrollmentId()));
    query.setStatus(List.of(EnrollmentStatus.ACTIVE));

    if (identity.isSet()) {
      query.setClinicId(List.of(identity.getClinicId()));
      query.setPatientId(List.of(identity.getPatientId()));
    }

    var enrollments = enrollmentService.getEnrollments(query);
    if (enrollments.isEmpty()) {
      throw new BadDataException(
          "unable to find active enrollment with id associated with authorized identity");
    }
    var enrollment = enrollments.get(0);

    var surveyInstance =
        surveyService.getSurveyInstance(
            enrollment.getClinicId(),
            enrollment.getPatientId(),
            request.getSurveyId(),
            request.getSurveyParameters());

    var checkIn =
        switch (request.getReason()) {
          case CARE_TEAM_REQUESTED -> checkInCreationService.createCareTeamRequestedCheckIn(
              enrollment, surveyInstance, request.getOneTimeContact());
          case PATIENT_REQUESTED -> checkInCreationService.createPatientRequestedCheckIn(
              enrollment, surveyInstance);
          default -> throw new RuntimeException();
        };

    if (request.isSendNotification()) {
      var notificationId = String.format("%s_on-demand_%s", enrollment.getId(), checkIn.getId());
      log.info(
          "sending notification for on-demand check-in, check-in id: {}, notification id: {}",
          checkIn.getId(),
          notificationId);

      if (request.getOneTimeContact() != null) {
        notificationService.sendOneTimeNotification(
            notificationId,
            enrollment,
            NotificationService.Event.ON_DEMAND_SURVEY,
            NotificationService.Category.ON_DEMAND_SURVEY_REQUEST,
            request.getOneTimeContact());
      } else {
        notificationService.sendNotification(
            notificationId,
            enrollment,
            NotificationService.Event.ON_DEMAND_SURVEY,
            NotificationService.Category.ON_DEMAND_SURVEY_REQUEST);
      }

      // TODO: review this event type
      HealthTrackerEvent event = new HealthTrackerEvent();
      event.setType(HealthTrackerEvent.Type.REMINDER_SENT);
      event.setEvent("Check-in reminder sent");
      event.setRelatedCheckinId(List.of(checkIn.getId()));
      event.setEnrollmentId(enrollment.getId());
      event.setClinicId(enrollment.getClinicId());
      event.setPatientId(enrollment.getPatientId());
      event.setDate(Instant.now());
      event.setBy(checkIn.getCreatedBy());
      eventsRepository.upsertCheckinEvent(event);
    }

    return new CheckInResponse(checkIn, null);
  }
}
