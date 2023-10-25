package com.navigatingcancer.healthtracker.api.data.service.impl;

import com.navigatingcancer.healthtracker.api.data.auth.Identity;
import com.navigatingcancer.healthtracker.api.data.migrate.CheckInScheduleMigrator;
import com.navigatingcancer.healthtracker.api.data.model.*;
import com.navigatingcancer.healthtracker.api.data.model.survey.SurveyId;
import com.navigatingcancer.healthtracker.api.data.model.survey.SurveyInstance;
import com.navigatingcancer.healthtracker.api.data.model.survey.SurveySubmission;
import com.navigatingcancer.healthtracker.api.data.repo.CheckInRepository;
import com.navigatingcancer.healthtracker.api.data.service.CheckInCreationService;
import com.navigatingcancer.healthtracker.api.data.service.SurveyService;
import com.navigatingcancer.healthtracker.api.metrics.HealthTrackerCounterMetric;
import com.navigatingcancer.healthtracker.api.metrics.MetersService;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class CheckInCreationServiceImpl implements CheckInCreationService {
  private final MetersService metersService;

  private final CheckInRepository checkInRepository;
  private final CheckInScheduleMigrator checkInScheduleMigrator;
  private final Identity identity;
  private final SurveyService surveyService;

  @Autowired
  public CheckInCreationServiceImpl(
      MetersService metersService,
      CheckInRepository checkInRepository,
      CheckInScheduleMigrator checkInScheduleMigrator,
      SurveyService surveyService,
      Identity identity) {
    this.metersService = metersService;
    this.checkInRepository = checkInRepository;
    this.checkInScheduleMigrator = checkInScheduleMigrator;
    this.surveyService = surveyService;
    this.identity = identity;
  }

  @Override
  public CheckIn createCheckInForSchedule(
      Enrollment enrollment, CheckInSchedule schedule, LocalDateTime scheduledAt) {

    var migrated = checkInScheduleMigrator.migrateIfNecessary(enrollment, schedule);
    if (migrated) {
      log.info("proactively migrated check-in schedule before creating new check-in");
    }

    SurveyInstance surveyInstance = getSurveyInstance(schedule.getSurveyInstanceId());
    if (schedule.getId() != null) {
      log.info("creating check-in for schedule {}", schedule.getId());
    } else {
      log.info("creating check-in for check-in type {}", schedule.getCheckInType());
    }

    CheckIn checkIn =
        buildCheckIn(enrollment, surveyInstance, scheduledAt, ReasonForCheckInCreation.SCHEDULED);

    checkIn.setCheckInScheduleId(schedule.getId());
    // legacy compat: set check-in type if available
    checkIn.setCheckInType(schedule.getCheckInType());

    return persistCheckIn(checkIn);
  }

  @Override
  public CheckIn createCareTeamRequestedCheckIn(
      Enrollment enrollment, SurveyInstance surveyInstance) {
    return createCareTeamRequestedCheckIn(enrollment, surveyInstance, null);
  }

  @Override
  public CheckIn createCareTeamRequestedCheckIn(
      Enrollment enrollment, SurveyInstance surveyInstance, ContactPreferences contactPreferences) {
    var checkIn =
        buildCheckIn(
            enrollment,
            surveyInstance,
            nowForUnscheduledCheckIn(enrollment),
            ReasonForCheckInCreation.CARE_TEAM_REQUESTED);
    checkIn.setCreatedBy(identity.getClinicianName());
    checkIn.setContactPreferences(contactPreferences);
    return persistCheckIn(checkIn);
  }

  @Override
  public CheckIn createPatientRequestedCheckIn(
      Enrollment enrollment, SurveyInstance surveyInstance) {
    var checkIn =
        buildCheckIn(
            enrollment,
            surveyInstance,
            nowForUnscheduledCheckIn(enrollment),
            ReasonForCheckInCreation.PATIENT_REQUESTED);
    checkIn.setCreatedBy(String.format("patientId=%d", identity.getPatientId()));

    return persistCheckIn(checkIn);
  }

  @Override
  public CheckIn buildPracticeCheckIn(Enrollment enrollment, CheckInSchedule schedule) {
    SurveyInstance surveyInstance = getSurveyInstance(schedule.getSurveyInstanceId());
    CheckIn checkIn =
        buildCheckIn(
            enrollment, surveyInstance, LocalDateTime.now(), ReasonForCheckInCreation.SCHEDULED);
    checkIn.setId(SurveySubmission.PRACTICE_CHECKIN_ID);
    checkIn.getCheckInParameters().put("isPractice", true);
    return checkIn;
  }

  private LocalDateTime nowForUnscheduledCheckIn(Enrollment enrollment) {
    // TODO: lookup clinic default time zone
    var tz = ZoneId.of("America/Denver");
    if (enrollment.getReminderTimeZone() != null) {
      tz = ZoneId.of(enrollment.getReminderTimeZone());
    }

    return LocalDateTime.now(tz);
  }

  private CheckIn buildCheckIn(
      Enrollment enrollment,
      SurveyInstance surveyInstance,
      LocalDateTime scheduledAt,
      ReasonForCheckInCreation reason) {
    log.info("creating check-in for enrollment {}", enrollment.getId());

    var checkIn = new CheckIn(enrollment);
    checkIn.setStatus(CheckInStatus.PENDING);
    checkIn.setScheduleDate(scheduledAt.toLocalDate());
    checkIn.setScheduleTime(scheduledAt.toLocalTime());
    checkIn.setCreatedReason(reason);

    if (surveyInstance != null) {
      checkIn.setSurveyInstanceId(surveyInstance.getId());
      checkIn.setSurveyId(surveyInstance.getSurveyId());
      checkIn.setCheckInParameters(getParams(enrollment, surveyInstance));
    }

    if (checkIn.getCheckInParameters() == null) {
      checkIn.setCheckInParameters(new HashMap<>());
    }

    return checkIn;
  }

  private CheckIn persistCheckIn(CheckIn checkIn) {
    checkIn = checkInRepository.upsertByNaturalKey(checkIn);

    switch (checkIn.getCreatedReason()) {
      case SCHEDULED -> metersService.incrementCounter(
          checkIn.getClinicId(), HealthTrackerCounterMetric.CHECKIN_CREATED_ON_SCHEDULE);
      case CARE_TEAM_REQUESTED -> metersService.incrementCounter(
          checkIn.getClinicId(), HealthTrackerCounterMetric.CHECKIN_CREATED_BY_CARE_TEAM);
      case PATIENT_REQUESTED -> metersService.incrementCounter(
          checkIn.getClinicId(), HealthTrackerCounterMetric.CHECKIN_CREATED_BY_PATIENT);
      default -> metersService.incrementCounter(
          checkIn.getClinicId(), HealthTrackerCounterMetric.CHECKIN_CREATED);
    }
    return checkIn;
  }

  private Map<String, Object> getParams(Enrollment enrollment, SurveyInstance surveyInstance) {
    if (SurveyId.isOralSurvey(surveyInstance.getSurveyId())) {
      return getOralAdherenceParams(enrollment, surveyInstance);
    }
    return surveyInstance.getSurveyParameters();
  }

  private Map<String, Object> getOralAdherenceParams(
      Enrollment enrollment, SurveyInstance surveyInstance) {
    // TODO determine these parameters
    return surveyInstance.getSurveyParameters();
  }

  private SurveyInstance getSurveyInstance(String surveyInstanceId) {
    SurveyInstance surveyInstance = null;
    if (surveyInstanceId != null) {
      surveyInstance = surveyService.getSurveyInstance(surveyInstanceId);
    }
    return surveyInstance;
  }
}
