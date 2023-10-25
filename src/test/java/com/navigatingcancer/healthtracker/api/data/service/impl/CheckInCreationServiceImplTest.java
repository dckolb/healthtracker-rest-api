package com.navigatingcancer.healthtracker.api.data.service.impl;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.navigatingcancer.healthtracker.api.TestConfig;
import com.navigatingcancer.healthtracker.api.data.migrate.CheckInScheduleMigrator;
import com.navigatingcancer.healthtracker.api.data.model.*;
import com.navigatingcancer.healthtracker.api.data.model.survey.SurveyId;
import com.navigatingcancer.healthtracker.api.data.model.survey.SurveyInstance;
import com.navigatingcancer.healthtracker.api.data.repo.CheckInRepository;
import com.navigatingcancer.healthtracker.api.data.repo.surveyInstance.SurveyInstanceRepository;
import com.navigatingcancer.healthtracker.api.data.service.SurveyConfigService;
import com.navigatingcancer.healthtracker.api.metrics.MetersService;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
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
public class CheckInCreationServiceImplTest {
  @MockBean MetersService metersService;

  @MockBean CheckInRepository checkInRepository;

  @MockBean SurveyConfigService surveyConfigService;

  @MockBean SurveyInstanceRepository surveyInstanceRepository;

  @MockBean CheckInScheduleMigrator migrator;

  @Autowired CheckInCreationServiceImpl service;

  @Captor ArgumentCaptor<CheckIn> checkInCaptor;

  @Before
  public void setUp() {
    when(checkInRepository.upsertByNaturalKey(any()))
        .thenAnswer((Answer<CheckIn>) invocationOnMock -> invocationOnMock.getArgument(0));
  }

  @Test
  public void createCheckInForSchedule_withSurveyInstance() {
    SurveyInstance surveyInstance = new SurveyInstance(1L, 1L, "my_survey", new TreeMap<>());
    surveyInstance.setId("survey_instance_id");

    Enrollment enrollment = new Enrollment();
    CheckInSchedule schedule = new CheckInSchedule();
    schedule.setSurveyInstanceId(surveyInstance.getId());

    when(surveyInstanceRepository.findById(any())).thenReturn(Optional.of(surveyInstance));

    service.createCheckInForSchedule(enrollment, schedule, LocalDateTime.now());

    verify(checkInRepository).upsertByNaturalKey(checkInCaptor.capture());
    var checkIn = checkInCaptor.getValue();
    assertNotNull(checkIn.getSurveyInstanceId());
  }

  @Test
  public void createCheckInForSchedule_withUnknownSurveyInstance() {
    Enrollment enrollment = new Enrollment();
    CheckInSchedule schedule = new CheckInSchedule();
    schedule.setSurveyInstanceId("unknown survey instance");

    when(surveyInstanceRepository.findById(any())).thenReturn(Optional.empty());

    assertThrows(
        "unknown survey instance id 'unknown survey instance'",
        RuntimeException.class,
        () -> {
          service.createCheckInForSchedule(enrollment, schedule, LocalDateTime.now());
        });

    verifyNoInteractions(checkInRepository);
  }

  @Test
  public void createCheckInForSchedule_withoutSurveyInstance() {
    SurveyInstance surveyInstance = new SurveyInstance(1L, 1L, "my_survey", new TreeMap<>());
    surveyInstance.setId("survey_instance_id");

    Enrollment enrollment = new Enrollment();
    CheckInSchedule schedule = new CheckInSchedule();
    schedule.setCheckInType(CheckInType.ORAL);

    when(migrator.migrateIfNecessary(enrollment, schedule))
        .thenAnswer(
            (Answer<Boolean>)
                invocation -> {
                  schedule.setSurveyInstanceId(surveyInstance.getId());
                  schedule.setId("my_id");
                  return true;
                });

    when(surveyInstanceRepository.findById(surveyInstance.getId()))
        .thenReturn(Optional.of(surveyInstance));

    service.createCheckInForSchedule(enrollment, schedule, LocalDateTime.now());

    verify(checkInRepository).upsertByNaturalKey(checkInCaptor.capture());

    var checkIn = checkInCaptor.getValue();
    assertEquals("survey_instance_id", checkIn.getSurveyInstanceId());
    assertEquals(schedule.getId(), checkIn.getCheckInScheduleId());
    assertEquals(ReasonForCheckInCreation.SCHEDULED, checkIn.getCreatedReason());
  }

  @Test
  public void createCareTeamRequestedCheckIn() {
    Enrollment enrollment = new Enrollment();
    enrollment.setPatientId(1L);
    enrollment.setClinicId(1L);
    SurveyInstance surveyInstance =
        new SurveyInstance(1L, 1L, SurveyId.NCCN_2022_DISTRESS, Map.of());
    surveyInstance.setId("survey_instance_id");

    CheckIn checkIn = service.createCareTeamRequestedCheckIn(enrollment, surveyInstance);
    assertNotNull(checkIn);
    assertEquals(surveyInstance.getId(), checkIn.getSurveyInstanceId());
    assertEquals(surveyInstance.getSurveyId(), checkIn.getSurveyId());
    assertEquals(enrollment.getClinicId(), checkIn.getClinicId());
    assertEquals(enrollment.getPatientId(), checkIn.getPatientId());
    assertEquals(ReasonForCheckInCreation.CARE_TEAM_REQUESTED, checkIn.getCreatedReason());
  }

  @Test
  public void createPatientRequestedCheckIn() {
    Enrollment enrollment = new Enrollment();
    enrollment.setPatientId(1L);
    enrollment.setClinicId(1L);
    SurveyInstance surveyInstance =
        new SurveyInstance(1L, 1L, SurveyId.NCCN_2022_DISTRESS, Map.of());
    surveyInstance.setId("survey_instance_id");

    CheckIn checkIn = service.createPatientRequestedCheckIn(enrollment, surveyInstance);
    assertNotNull(checkIn);
    assertEquals(surveyInstance.getId(), checkIn.getSurveyInstanceId());
    assertEquals(surveyInstance.getSurveyId(), checkIn.getSurveyId());
    assertEquals(enrollment.getClinicId(), checkIn.getClinicId());
    assertEquals(enrollment.getPatientId(), checkIn.getPatientId());
    assertEquals(ReasonForCheckInCreation.PATIENT_REQUESTED, checkIn.getCreatedReason());
  }
}
