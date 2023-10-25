package com.navigatingcancer.healthtracker.api.data.migrate;

import com.google.common.base.Preconditions;
import com.navigatingcancer.healthtracker.api.data.model.CheckInSchedule;
import com.navigatingcancer.healthtracker.api.data.model.CheckInScheduleStatus;
import com.navigatingcancer.healthtracker.api.data.model.CheckInStatus;
import com.navigatingcancer.healthtracker.api.data.model.Enrollment;
import com.navigatingcancer.healthtracker.api.data.model.survey.SurveyId;
import com.navigatingcancer.healthtracker.api.data.model.survey.SurveyInstance;
import com.navigatingcancer.healthtracker.api.data.repo.CheckInRepository;
import com.navigatingcancer.healthtracker.api.data.repo.EnrollmentRepository;
import com.navigatingcancer.healthtracker.api.data.repo.surveyInstance.SurveyInstanceRepository;
import com.navigatingcancer.healthtracker.api.data.service.SurveyConfigService;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CheckInScheduleMigrator {
  private final SurveyConfigService surveyConfigService;
  private final SurveyInstanceRepository surveyInstanceRepository;
  private final EnrollmentRepository enrollmentRepository;
  private final CheckInRepository checkInRepository;

  public CheckInScheduleMigrator(
      SurveyConfigService surveyConfigService,
      SurveyInstanceRepository surveyInstanceRepository,
      EnrollmentRepository enrollmentRepository,
      CheckInRepository checkInRepository) {
    this.surveyConfigService = surveyConfigService;
    this.surveyInstanceRepository = surveyInstanceRepository;
    this.enrollmentRepository = enrollmentRepository;
    this.checkInRepository = checkInRepository;
  }

  /**
   * Performs the following migration for the enrollment and schedule: - creates and persists a
   * SurveyInstance for check-in type based schedules - sets an object id on the schedule
   *
   * <p>Note: These migrations are only applied when the "multi-survey" feature is enabled for a
   * clinic.
   *
   * <p>Note: this method mutates the enrollment and schedule arguments
   *
   * @param enrollment
   * @param schedule
   * @return whether the migration was executed or not
   */
  public boolean migrateIfNecessary(Enrollment enrollment, CheckInSchedule schedule) {
    if (!isSupported(enrollment)) return false;

    try {
      setScheduleDefaults(enrollment, schedule);
      var updated = enrollmentRepository.updateCheckInScheduleByCheckInType(enrollment, schedule);

      if (!updated) {
        log.error(
            "migration: failed to update schedule owned by enrollment {}, unable to find and update"
                + " schedule with check-in type {}",
            enrollment.getId(),
            schedule.getCheckInType());

        return false;
      }

      updatePendingCheckIns(enrollment, schedule);

      log.info(
          "migration: successfully updated schedule {} owned by enrollment {}",
          schedule.getId(),
          enrollment.getId());
      return true;
    } catch (Exception e) {
      log.error(
          "migration: failed to update schedule {} owned by enrollment {}",
          schedule.getId(),
          enrollment.getId());
      throw e;
    }
  }

  /**
   * Initializes the following properties if not present: - id - createdDate - createdBy -
   * updatedDate - updatedBy - status: ACTIVE
   *
   * @param enrollment
   * @param schedule
   */
  public void setScheduleDefaults(Enrollment enrollment, CheckInSchedule schedule) {
    if (!isSupported(enrollment)) return;

    Preconditions.checkArgument(
        enrollment.getSchedules() != null && enrollment.getSchedules().contains(schedule),
        "enrollment must reference schedule");

    if (schedule.getId() == null) {
      schedule.setId(new ObjectId().toString());
      schedule.setCreatedDate(enrollment.getCreatedDate());
      schedule.setCreatedBy(enrollment.getCreatedBy());
      schedule.setUpdatedBy("Health Tracker");
      schedule.setUpdatedDate(new Date());

      log.info(
          "migration: generated an id for schedule {} owned by enrollment {}",
          schedule.getId(),
          enrollment.getId());
    }

    if (schedule.getSurveyInstanceId() == null) {
      SurveyInstance surveyInstance = findOrCreateSurveyInstance(enrollment, schedule);
      schedule.setSurveyInstanceId(surveyInstance.getId());
    }

    if (schedule.getStatus() == null) {
      schedule.setStatus(CheckInScheduleStatus.ACTIVE);
    }

    schedule.setEnrollmentId(enrollment.getId());
  }

  private void updatePendingCheckIns(Enrollment enrollment, CheckInSchedule schedule) {
    var surveyInstance =
        surveyInstanceRepository.findById(schedule.getSurveyInstanceId()).orElseThrow();

    try {
      var numUpdatedCheckIns =
          checkInRepository.bulkUpdateFieldsByCheckInType(
              enrollment.getId(),
              schedule.getCheckInType(),
              CheckInStatus.PENDING,
              Map.of(
                  "checkInScheduleId", schedule.getId(),
                  "surveyInstanceId", surveyInstance.getId(),
                  "surveyId", surveyInstance.getSurveyId(),
                  "checkInParameters", surveyInstance.getSurveyParameters()));
      log.info(
          "migration: setting schedule details for schedule {} on {} {} check-ins for enrollment {}",
          schedule.getId(),
          numUpdatedCheckIns,
          schedule.getCheckInType(),
          enrollment.getId());
    } catch (Exception e) {
      log.error(
          "migration: unable to migrate {} check-ins for enrollment {}",
          schedule.getCheckInType(),
          enrollment.getId());
      throw e;
    }
  }

  public void updateIfNecessary(
      Enrollment enrollment, CheckInSchedule persistedSchedule, CheckInSchedule incomingSchedule) {

    if (!isSupported(enrollment)) return;

    SurveyInstance surveyInstance = findOrCreateSurveyInstance(enrollment, incomingSchedule);
    if (incomingSchedule.matches(persistedSchedule)
        && Objects.equals(surveyInstance.getId(), persistedSchedule.getSurveyInstanceId())) {
      return;
    }
    incomingSchedule.setSurveyInstanceId(surveyInstance.getId());
    return;
  }

  private SurveyInstance findOrCreateSurveyInstance(
      Enrollment enrollment, CheckInSchedule schedule) {
    SurveyInstance surveyInstance = null;
    try {
      var surveyId =
          surveyConfigService
              .getSurveyIdForCheckInType(enrollment, schedule.getCheckInType())
              .orElseThrow(
                  () ->
                      new RuntimeException(
                          "unable to resolve survey id for check-in type "
                              + schedule.getCheckInType()));

      var params = new TreeMap<String, Object>();
      if (SurveyId.isOralSurvey(surveyId)) {
        params.put("medicationName", enrollment.getMedication());
      }

      surveyInstance =
          new SurveyInstance(enrollment.getClinicId(), enrollment.getPatientId(), surveyId, params);

      surveyInstance =
          surveyInstanceRepository.insertIgnore(
              new SurveyInstance(
                  enrollment.getClinicId(), enrollment.getPatientId(), surveyId, params));

      log.info(
          "migration: using survey instance {} for legacy check-in type {}",
          surveyInstance,
          schedule.getCheckInType());

      return surveyInstance;
    } catch (Exception e) {
      log.error(
          "migration: unable to create survey instance {} for legacy check-in type  {}",
          surveyInstance,
          schedule.getCheckInType(),
          e);
      throw e;
    }
  }

  private boolean isSupported(Enrollment enrollment) {
    if (!surveyConfigService.isFeatureEnabled(enrollment.getClinicId(), "multi-survey")) {
      log.info("migration: skipping, clinic not allowed {}", enrollment.getClinicId());
      return false;
    }
    return true;
  }
}
