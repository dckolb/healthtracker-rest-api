package com.navigatingcancer.healthtracker.api.rest;

import com.navigatingcancer.healthtracker.api.data.auth.Identity;
import com.navigatingcancer.healthtracker.api.data.model.CheckIn;
import com.navigatingcancer.healthtracker.api.data.model.CheckInData;
import com.navigatingcancer.healthtracker.api.data.model.EnrollmentStatus;
import com.navigatingcancer.healthtracker.api.data.model.PracticeCheckIn;
import com.navigatingcancer.healthtracker.api.data.model.survey.SurveyPayload;
import com.navigatingcancer.healthtracker.api.data.service.CheckInService;
import com.navigatingcancer.healthtracker.api.data.service.impl.SchedulingServiceImpl;
import com.navigatingcancer.healthtracker.api.rest.QueryParameters.EnrollmentQuery;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RestController;

@RestController("/checkins")
@CrossOrigin(originPatterns = "*", allowCredentials = "true")
@Slf4j
public class CheckInAPIController implements CheckInAPI {

  @Autowired CheckInService checkInService;

  @Autowired SchedulingServiceImpl schedulingService;

  @Autowired Identity identity;

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

  // @Override
  public PracticeCheckIn persistPracticeCheckInData(PracticeCheckIn practiceCheckIn) {
    return checkInService.persistPracticeCheckInData(practiceCheckIn);
  }
}
