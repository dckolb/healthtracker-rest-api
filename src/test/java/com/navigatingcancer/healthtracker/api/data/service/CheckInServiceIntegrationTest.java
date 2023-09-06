package com.navigatingcancer.healthtracker.api.data.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.navigatingcancer.healthtracker.api.TestConfig;
import com.navigatingcancer.healthtracker.api.data.model.*;
import com.navigatingcancer.healthtracker.api.data.model.survey.SurveyItemPayload;
import com.navigatingcancer.healthtracker.api.data.model.survey.SurveyPayload;
import com.navigatingcancer.healthtracker.api.data.model.survey.SurveyPayloadContent;
import com.navigatingcancer.healthtracker.api.data.model.surveyConfig.ClinicConfig;
import com.navigatingcancer.healthtracker.api.data.repo.CheckInRepository;
import com.navigatingcancer.healthtracker.api.data.repo.EnrollmentRepository;
import com.navigatingcancer.healthtracker.api.data.repo.EnrollmentRepositoryTest;
import com.navigatingcancer.healthtracker.api.data.service.impl.NotificationService;
import com.navigatingcancer.healthtracker.api.data.service.impl.SchedulingServiceImpl;
import com.navigatingcancer.healthtracker.api.processor.HealthTrackerStatusService;
import com.navigatingcancer.healthtracker.api.rest.QueryParameters.EnrollmentQuery;
import com.navigatingcancer.patientinfo.PatientInfoClient;
import com.navigatingcancer.patientinfo.domain.PatientInfo;
import com.navigatingcancer.scheduler.client.domain.SchedulePayload;
import com.navigatingcancer.scheduler.client.service.SchedulerServiceClient;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
@Import(TestConfig.class)
public class CheckInServiceIntegrationTest {
  @Autowired private EnrollmentRepository enrollmentRepository;

  @Autowired private CheckInRepository checkInRepository;

  @MockBean private SchedulerServiceClient schedulerServiceClient;

  @MockBean private HealthTrackerStatusService healthTrackerStatusService;

  @MockBean private PatientInfoClient patientInfoClient;

  @Autowired private SchedulingServiceImpl schedulingService;

  @MockBean private SurveyConfigService surveyConfigService;

  @Autowired private CheckInService service;

  @MockBean private NotificationService notificationService;

  @Before
  public void setup() {

    SchedulerServiceClient.FeignClient mock =
        Mockito.mock(SchedulerServiceClient.FeignClient.class);
    given(schedulerServiceClient.getApi()).willReturn(mock);

    PatientInfoClient.FeignClient client = Mockito.mock(PatientInfoClient.FeignClient.class);
    Mockito.when(patientInfoClient.getApi()).thenReturn(client);
    Mockito.when(client.getPatients(Mockito.any(), Mockito.any()))
        .thenReturn(Arrays.asList(new PatientInfo()));

    //        Mockito.when(schedulingService.getNextCheckInDate(any())).thenCallRealMethod();
    //        Mockito.when(schedulingService.getNextCheckInDate(any(), any())).thenCallRealMethod();
    //        Mockito.when(schedulingService.getLastCheckInDate(any(), any())).thenCallRealMethod();

  }

  private EnrollmentQuery query(long i) {
    EnrollmentQuery query = new EnrollmentQuery();
    query.setLocationId(Arrays.asList(i));
    query.setClinicId(Arrays.asList(i));
    query.setPatientId(Arrays.asList(i));
    return query;
  }

  private CheckIn createCheckIn(
      String enrollmentId,
      CheckInStatus status,
      CheckInType type,
      LocalDate date,
      Boolean medicationTaken) {
    CheckIn ci = new CheckIn();
    ci.setEnrollmentId(enrollmentId);
    ci.setStatus(status);
    ci.setCheckInType(type);
    ci.setScheduleDate(date);
    ci.setScheduleTime(LocalTime.now());
    ci.setMedicationTaken(medicationTaken);

    return checkInRepository.save(ci);
  }

  private CheckIn createCheckIn(
      String enrollmentId, CheckInStatus status, CheckInType type, LocalDate date) {
    return createCheckIn(enrollmentId, status, type, date, null);
  }

  private CheckInSchedule createDailyOralCheckInSchedule(LocalDate originalStartDate) {
    CheckInSchedule schedule = new CheckInSchedule();
    schedule.setCheckInFrequency(CheckInFrequency.DAILY);
    schedule.setStartDate(originalStartDate);
    schedule.setEndDate(originalStartDate);
    schedule.setCheckInType(CheckInType.ORAL);

    return schedule;
  }

  private Enrollment createEnrollment(
      long clinicId, LocalDate originalStartDate, CheckInSchedule schedule) {
    Enrollment en =
        EnrollmentRepositoryTest.createEnrollmentWithSchedules(
            clinicId, clinicId, clinicId, schedule);
    en.setDaysInCycle(10);
    en.setTxStartDate(originalStartDate);
    en = enrollmentRepository.save(en);

    if (originalStartDate != null) {
      System.out.println(originalStartDate);
      System.out.println(en.getTxStartDate());
      Assert.assertFalse(en.getTxStartDate().isBefore(originalStartDate));
    }

    return en;
  }

  private SurveyPayload createOralSurveyPayload(Enrollment en) {
    SurveyPayload surveyPayload = new SurveyPayload();
    SurveyPayloadContent content = new SurveyPayloadContent();
    content.setOral(new ArrayList<>());
    content.setEnrollmentId(en.getId());
    surveyPayload.setContent(content);

    return surveyPayload;
  }

  private SurveyItemPayload createOralSurveyItemPayload(
      CheckIn ci, SurveyPayloadContent content, LocalDate patientReportedStartDate) {
    SurveyItemPayload oral = new SurveyItemPayload();

    Map<String, Object> payload = new HashMap<>();
    payload.put(
        CheckInService.MEDICATION_TAKEN_QUESTION_ID, CheckInService.MEDICATION_TAKEN_ANSWER_ID);
    content.getOral().add(oral);

    oral.setPayload(payload);
    oral.setId(ci.getId());

    if (patientReportedStartDate != null) {
      payload.put(
          CheckInService.MEDICATION_STARTED_QUESTION_ID, CheckInService.MEDICATION_TAKEN_ANSWER_ID);
      payload.put(
          CheckInService.MEDICATION_STARTED_DATE_QUESTIONS_ID,
          patientReportedStartDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
    }

    return oral;
  }

  @Test
  public void givenDifferentStartDate_shouldAdjustSchedule() {
    ClinicConfig clinicConfig = new ClinicConfig();
    Map<String, Boolean> features = new HashMap<>();
    features.put("patient-reported-start-date", true);
    clinicConfig.setFeatures(features);
    Map<String, String> programs = new HashMap<>();
    programs.put("HealthTracker", "5f065dfc7be5761f058b6cc7");
    clinicConfig.setPrograms(programs);

    CheckInSchedule schedule = new CheckInSchedule();
    schedule.setCheckInFrequency(CheckInFrequency.DAILY);
    LocalDate originalStartDate = LocalDate.now();
    schedule.setStartDate(originalStartDate);
    schedule.setEndDate(originalStartDate);
    schedule.setCheckInType(CheckInType.ORAL);
    Enrollment en = EnrollmentRepositoryTest.createEnrollmentWithSchedules(1, 1, 1, schedule);
    en.setDaysInCycle(10);
    en.setTxStartDate(originalStartDate);
    en = enrollmentRepository.save(en);
    Mockito.when(surveyConfigService.getClinicConfig(en.getClinicId())).thenReturn(clinicConfig);

    System.out.println(originalStartDate);
    System.out.println(en.getTxStartDate());
    Assert.assertFalse(en.getTxStartDate().isBefore(originalStartDate));
    CheckIn ci =
        createCheckIn(en.getId(), CheckInStatus.PENDING, CheckInType.ORAL, LocalDate.now());

    CheckInData checkInData = service.getCheckInData(query(1));

    assertEquals(checkInData.getPending().size(), 1);
    assertEquals(checkInData.getPending().get(0).getStatus(), CheckInStatus.PENDING);

    SurveyPayload surveyPayload = new SurveyPayload();
    SurveyPayloadContent content = surveyPayload.content;
    content.setEnrollmentId(en.getId());
    surveyPayload.setContent(content);

    SurveyItemPayload oral = new SurveyItemPayload();
    oral.setId(ci.getId());
    Map<String, Object> payload = new HashMap<>();
    oral.setPayload(payload);
    content.setOral(new ArrayList<>());
    content.getOral().add(oral);
    payload.put(
        CheckInService.MEDICATION_TAKEN_QUESTION_ID, CheckInService.MEDICATION_TAKEN_ANSWER_ID);
    // subtract 2 days
    LocalDate startDate = LocalDate.now().minusDays(2);

    payload.put(
        CheckInService.MEDICATION_STARTED_QUESTION_ID, CheckInService.MEDICATION_TAKEN_ANSWER_ID);
    payload.put(
        CheckInService.MEDICATION_STARTED_DATE_QUESTIONS_ID,
        startDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));

    // put in backfill
    SurveyItemPayload oralBackfill = new SurveyItemPayload();
    Map<String, Object> backfill = new HashMap<>();
    content.getOral().add(oralBackfill);
    oralBackfill.setPayload(backfill);
    backfill.put(
        CheckInService.MEDICATION_TAKEN_QUESTION_ID, CheckInService.MEDICATION_TAKEN_ANSWER_ID);
    // subtract 1 days
    LocalDate takenDate = LocalDate.now().minusDays(1);
    backfill.put(
        CheckInService.MEDICATION_TAKEN_DATE,
        takenDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));

    // creates a single checkin
    service.checkIn(surveyPayload);

    Enrollment enResult = enrollmentRepository.findById(en.getId()).get();
    System.out.println(enResult.getTxStartDate());
    Assert.assertTrue(en.getTxStartDate().isAfter(startDate));

    Assert.assertTrue(enResult.getPatientReportedTxStartDate().isBefore(originalStartDate));
    Assert.assertEquals(enResult.getPatientReportedTxStartDate(), startDate);
    Assert.assertNotNull(enResult.getFirstCheckInResponseDate());

    ArgumentCaptor<SchedulePayload> schedulePayloadArgumentCaptor =
        ArgumentCaptor.forClass(SchedulePayload.class);

    verify(schedulerServiceClient.getApi(), times(1))
        .schedule(any(), schedulePayloadArgumentCaptor.capture());

    SchedulePayload schedulePayload = schedulePayloadArgumentCaptor.getValue();
    for (SchedulePayload.ScheduleItemPayload itemPayload : schedulePayload.getItems()) {
      System.out.println(itemPayload);
      Assert.assertTrue(
          itemPayload.getStartDate().isBefore(itemPayload.getEndDate())
              || itemPayload.getStartDate().isEqual(itemPayload.getEndDate()));
    }

    // check that backfill was properly created
    List<CheckIn> checkins = checkInRepository.findByEnrollmentId(en.getId());

    Assert.assertEquals(1, checkins.size());
  }

  @Test
  public void verifyScheduleStartDateChangeUpdatesCorrectly() {
    // create enrollment with txStartDate
    long clinicId = 2;
    LocalDate originalStartDate = LocalDate.now();
    ClinicConfig clinicConfig = new ClinicConfig();
    Map<String, Boolean> features = new HashMap<>();
    features.put("patient-reported-start-date", true);
    clinicConfig.setFeatures(features);

    CheckInSchedule schedule = createDailyOralCheckInSchedule(originalStartDate);
    Enrollment en = createEnrollment(clinicId, originalStartDate, schedule);
    Mockito.when(surveyConfigService.getClinicConfig(en.getClinicId())).thenReturn(clinicConfig);
    CheckIn ci =
        createCheckIn(en.getId(), CheckInStatus.PENDING, CheckInType.ORAL, LocalDate.now());

    // create checkIn with different startDate
    LocalDate patientReportedMedicationStartDate = LocalDate.now().minusDays(2);
    SurveyPayload surveyPayload = createOralSurveyPayload(en);
    createOralSurveyItemPayload(ci, surveyPayload.getContent(), patientReportedMedicationStartDate);

    // checkIn
    service.checkIn(surveyPayload);

    // do another checkIn and verify that scheduleStartDateChanged is not set to true for that one.
    SurveyPayload newSurveyPayload = createOralSurveyPayload(en);
    CheckIn newCi =
        createCheckIn(en.getId(), CheckInStatus.PENDING, CheckInType.ORAL, LocalDate.now());
    createOralSurveyItemPayload(newCi, newSurveyPayload.getContent(), null);

    // 2nd oral checkIn
    service.checkIn(newSurveyPayload);
  }

  @Test
  public void verifyGetCheckInDataByEnrollmentIDsReturnsAllCheckIns() {
    // create 2 enrollments with 2 checkIns
    LocalDate startDate1 = LocalDate.now();
    CheckInSchedule schedule1 = createDailyOralCheckInSchedule(startDate1);
    Enrollment en1 = createEnrollment(11, startDate1, schedule1);

    LocalDate startDate2 = LocalDate.now().minusDays(2);
    CheckInSchedule schedule2 = createDailyOralCheckInSchedule(startDate2);
    Enrollment en2 = createEnrollment(11, startDate2, schedule2);

    List<CheckInData> data =
        service.getCheckInDataByEnrollmentIDs(List.of(en1.getId(), en2.getId()));

    Assert.assertEquals(2, data.size());

    // enrollments are in desc order by createdDate
    Assert.assertEquals(data.get(0).getEnrollment().getId(), en2.getId());
    Assert.assertEquals(data.get(1).getEnrollment().getId(), en1.getId());
  }
}
