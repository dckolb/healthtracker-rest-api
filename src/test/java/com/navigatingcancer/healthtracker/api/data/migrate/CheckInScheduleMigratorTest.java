package com.navigatingcancer.healthtracker.api.data.migrate;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.navigatingcancer.healthtracker.api.TestConfig;
import com.navigatingcancer.healthtracker.api.data.model.CheckInSchedule;
import com.navigatingcancer.healthtracker.api.data.model.CheckInType;
import com.navigatingcancer.healthtracker.api.data.model.Enrollment;
import com.navigatingcancer.healthtracker.api.data.model.survey.SurveyInstance;
import com.navigatingcancer.healthtracker.api.data.repo.CheckInRepository;
import com.navigatingcancer.healthtracker.api.data.repo.EnrollmentRepository;
import com.navigatingcancer.healthtracker.api.data.repo.surveyInstance.SurveyInstanceRepository;
import com.navigatingcancer.healthtracker.api.data.service.SurveyConfigService;
import java.util.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
@Import(TestConfig.class)
public class CheckInScheduleMigratorTest {
  private static final long CLINIC_ID = 99;
  private static final long PATIENT_ID = 1;

  @MockBean SurveyConfigService surveyConfigService;

  @MockBean SurveyInstanceRepository surveyInstanceRepository;

  @MockBean EnrollmentRepository enrollmentRepository;

  @MockBean CheckInRepository checkInRepository;

  @Autowired CheckInScheduleMigrator migrator;

  @Captor ArgumentCaptor<SurveyInstance> surveyInstanceCaptor;

  SurveyInstance surveyInstance;

  @Before
  public void setup() {
    surveyInstance =
        new SurveyInstance(CLINIC_ID, PATIENT_ID, "5ef3ccd0296b54c5bc8af1d3", new TreeMap<>());
    surveyInstance.setId("survey_instance_id");

    when(surveyInstanceRepository.findById(surveyInstance.getId()))
        .thenAnswer(
            (Answer<Optional<SurveyInstance>>) invocationOnMock -> Optional.of(surveyInstance));

    when(surveyConfigService.isFeatureEnabled(CLINIC_ID, "multi-survey")).thenReturn(true);

    when(surveyConfigService.getSurveyIdForCheckInType(any(), any()))
        .thenReturn(Optional.of(surveyInstance.getSurveyId()));

    when(surveyInstanceRepository.insertIgnore(any()))
        .thenAnswer((Answer<SurveyInstance>) invocationOnMock -> surveyInstance);

    when(enrollmentRepository.updateCheckInScheduleByCheckInType(any(), any())).thenReturn(true);

    when(checkInRepository.bulkUpdateFieldsByCheckInType(any(), any(), any(), any()))
        .thenReturn(1L);
  }

  @Test
  public void migrateIfNecessary_doesNotReplaceScheduleIds() {
    CheckInSchedule schedule = new CheckInSchedule();
    schedule.setCheckInType(CheckInType.ORAL);
    schedule.setId("schedule-id");
    schedule.setCreatedBy("whomever");

    Enrollment enrollment = new Enrollment();
    enrollment.setClinicId(CLINIC_ID);
    enrollment.setSchedules(List.of(schedule));

    var migrated = migrator.migrateIfNecessary(enrollment, schedule);
    assertTrue(migrated);

    assertEquals(schedule.getId(), "schedule-id");
    assertEquals(schedule.getCreatedBy(), "whomever");
  }

  @Test(expected = IllegalArgumentException.class)
  public void migrateIfNecessary_doesNotMigrateSchedulesNotRelatedToEnrollment() {
    CheckInSchedule schedule = new CheckInSchedule();
    schedule.setCheckInType(CheckInType.ORAL);

    Enrollment enrollment = new Enrollment();
    enrollment.setClinicId(CLINIC_ID);
    enrollment.setSchedules(List.of());

    migrator.migrateIfNecessary(enrollment, schedule);
  }

  @Test
  public void migrateIfNecessary_doesNotMigrateForClinicWithoutFeature() {
    CheckInSchedule schedule = new CheckInSchedule();
    schedule.setCheckInType(CheckInType.ORAL);

    Enrollment enrollment = new Enrollment();
    enrollment.setPatientId(PATIENT_ID);
    enrollment.setClinicId(1L);
    enrollment.setMedication("my_medication");
    enrollment.setSchedules(List.of(schedule));

    var migrated = migrator.migrateIfNecessary(enrollment, schedule);
    assertFalse(migrated);

    verify(surveyConfigService, times(1)).isFeatureEnabled(1L, "multi-survey");
    verifyNoMoreInteractions(surveyConfigService);
    verifyNoInteractions(surveyInstanceRepository);
    verifyNoInteractions(enrollmentRepository);
  }

  @Test
  public void migrateIfNecessary_createsSurveyInstance() {

    CheckInSchedule schedule = new CheckInSchedule();
    schedule.setCheckInType(CheckInType.ORAL);

    Enrollment enrollment = new Enrollment();
    enrollment.setPatientId(PATIENT_ID);
    enrollment.setClinicId(CLINIC_ID);
    enrollment.setMedication("my_medication");
    enrollment.setSchedules(List.of(schedule));

    when(surveyConfigService.getSurveyIdForCheckInType(any(), any()))
        .thenReturn(Optional.of("5ef3ccd0296b54c5bc8af1d3"));

    when(enrollmentRepository.updateCheckInScheduleByCheckInType(enrollment, schedule))
        .thenReturn(true);

    var migrated = migrator.migrateIfNecessary(enrollment, schedule);
    assertTrue(migrated);

    verify(surveyConfigService, times(2)).isFeatureEnabled(CLINIC_ID, "multi-survey");
    verify(surveyConfigService).getSurveyIdForCheckInType(any(), any());
    verify(surveyInstanceRepository).insertIgnore(surveyInstanceCaptor.capture());
    verify(enrollmentRepository).updateCheckInScheduleByCheckInType(enrollment, schedule);

    assertEquals(surveyInstance.getId(), schedule.getSurveyInstanceId());

    var surveyInstanceArg = surveyInstanceCaptor.getValue();
    assertEquals(enrollment.getPatientId(), surveyInstanceArg.getPatientId());
    assertEquals(enrollment.getClinicId(), surveyInstanceArg.getClinicId());
    assertEquals(
        Map.of("medicationName", "my_medication"), surveyInstanceArg.getSurveyParameters());
  }

  @Test
  public void migrateIfNecessary_setsId() {
    SurveyInstance surveyInstance =
        new SurveyInstance(CLINIC_ID, PATIENT_ID, "my_survey", new TreeMap<>());
    surveyInstance.setId("survey_instance_id");

    CheckInSchedule schedule = new CheckInSchedule();
    schedule.setSurveyInstanceId(surveyInstance.getId());
    schedule.setCheckInType(CheckInType.ORAL);

    Enrollment enrollment = new Enrollment();
    enrollment.setId("enrollment id");
    enrollment.setPatientId(PATIENT_ID);
    enrollment.setClinicId(CLINIC_ID);
    enrollment.setMedication("my_medication");
    enrollment.setSchedules(List.of(schedule));

    when(enrollmentRepository.updateCheckInScheduleByCheckInType(enrollment, schedule))
        .thenReturn(true);

    var migrated = migrator.migrateIfNecessary(enrollment, schedule);
    assertTrue(migrated);

    verify(surveyInstanceRepository).findById(surveyInstance.getId());
    verifyNoMoreInteractions(surveyInstanceRepository);
    verify(enrollmentRepository).updateCheckInScheduleByCheckInType(enrollment, schedule);
    verify(surveyConfigService, times(2)).isFeatureEnabled(CLINIC_ID, "multi-survey");
    verifyNoMoreInteractions(surveyConfigService);

    assertNotNull(schedule.getId());
  }
}
