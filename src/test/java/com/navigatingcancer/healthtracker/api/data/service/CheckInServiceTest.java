package com.navigatingcancer.healthtracker.api.data.service;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.navigatingcancer.date.utils.DateTimeUtils;
import com.navigatingcancer.healthtracker.api.TestConfig;
import com.navigatingcancer.healthtracker.api.data.model.CheckIn;
import com.navigatingcancer.healthtracker.api.data.model.CheckInData;
import com.navigatingcancer.healthtracker.api.data.model.CheckInFrequency;
import com.navigatingcancer.healthtracker.api.data.model.CheckInSchedule;
import com.navigatingcancer.healthtracker.api.data.model.CheckInStatus;
import com.navigatingcancer.healthtracker.api.data.model.CheckInType;
import com.navigatingcancer.healthtracker.api.data.model.Enrollment;
import com.navigatingcancer.healthtracker.api.data.model.survey.SurveyItemPayload;
import com.navigatingcancer.healthtracker.api.data.model.survey.SurveyPayload;
import com.navigatingcancer.healthtracker.api.data.model.surveyConfig.ClinicConfig;
import com.navigatingcancer.healthtracker.api.data.repo.CheckInRepository;
import com.navigatingcancer.healthtracker.api.data.repo.EnrollmentRepository;
import com.navigatingcancer.healthtracker.api.data.repo.EnrollmentRepositoryTest;
import com.navigatingcancer.healthtracker.api.data.repo.HealthTrackerStatusRepository;
import com.navigatingcancer.healthtracker.api.data.service.impl.NotificationService;
import com.navigatingcancer.healthtracker.api.data.service.impl.SchedulingServiceImpl;
import com.navigatingcancer.healthtracker.api.data.service.impl.SurveyConfigServiceImpl;
import com.navigatingcancer.healthtracker.api.processor.HealthTrackerStatusService;
import com.navigatingcancer.healthtracker.api.rest.QueryParameters.EnrollmentQuery;
import com.navigatingcancer.patientinfo.PatientInfoClient;
import com.navigatingcancer.patientinfo.domain.PatientInfo;
import com.navigatingcancer.scheduler.client.service.SchedulerServiceClient;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(SpringRunner.class)
@SpringBootTest
@Import(TestConfig.class)
public class CheckInServiceTest {

  @Autowired private EnrollmentRepository enrollmentRepository;

  @Autowired private CheckInRepository checkInRepository;

  @MockBean private SchedulerServiceClient schedulerServiceClient;

  @MockBean private HealthTrackerStatusService healthTrackerStatusService;

  @MockBean private HealthTrackerStatusRepository healthTrackerStatusRepository;

  @MockBean private PatientInfoClient patientInfoClient;

  @MockBean private SchedulingServiceImpl schedulingService;
  @MockBean private SurveyConfigServiceImpl surveyConfigService;

  @Autowired private CheckInService service;

  @MockBean private NotificationService notificationService;

  private Random random = new Random();

  @Before
  public void setup() {

    SchedulerServiceClient.FeignClient mock =
        Mockito.mock(SchedulerServiceClient.FeignClient.class);
    given(schedulerServiceClient.getApi()).willReturn(mock);

    PatientInfoClient.FeignClient client = Mockito.mock(PatientInfoClient.FeignClient.class);
    Mockito.when(patientInfoClient.getApi()).thenReturn(client);
    Mockito.when(client.getPatients(Mockito.any(), Mockito.any()))
        .thenReturn(Arrays.asList(new PatientInfo()));

    Mockito.when(schedulingService.getNextCheckInDate(any())).thenCallRealMethod();
    Mockito.when(schedulingService.getNextCheckInDate(any(), any())).thenCallRealMethod();
    Mockito.when(schedulingService.getLastCheckInDate(any(), any())).thenCallRealMethod();

    ReflectionTestUtils.setField(
        schedulingService, "healthTrackerStatusRepository", healthTrackerStatusRepository);
    ReflectionTestUtils.setField(schedulingService, "checkInRepository", checkInRepository);
  }

  @Test
  public void testCheckInData() {

    CheckInSchedule schedule = new CheckInSchedule();
    schedule.setCheckInFrequency(CheckInFrequency.DAILY);
    schedule.setStartDate(LocalDate.now());
    schedule.setEndDate(LocalDate.now().plusDays(1));
    schedule.setCheckInType(CheckInType.ORAL);

    Enrollment en =
        enrollmentRepository.save(
            EnrollmentRepositoryTest.createEnrollmentWithSchedules(1, 1, 1, schedule));

    createCheckIn(en.getId(), CheckInStatus.PENDING, CheckInType.ORAL, LocalDate.now());

    createCheckIn(
        en.getId(), CheckInStatus.COMPLETED, CheckInType.ORAL, LocalDate.now().minusDays(1), true);
    createCheckIn(
        en.getId(),
        CheckInStatus.COMPLETED,
        CheckInType.SYMPTOM,
        LocalDate.now().minusDays(2),
        true);

    CheckInData checkInData = service.getCheckInData(query(1));

    assertEquals(checkInData.getPending().size(), 1);
    assertEquals(checkInData.getPending().get(0).getStatus(), CheckInStatus.PENDING);
    assertTrue(checkInData.getPending().get(0).getMedicationTaken());

    // completed 2: Oral 1 day ago and Symptom 2 days ago
    assertEquals(checkInData.getCompletedCount().intValue(), 2);

    assertNull(en.getFirstCheckInResponseDate());
    assertNotNull(checkInData.getNext());
    assertNotNull(checkInData.getNextOralCheckIn());
    assertNotNull(checkInData.getLastOralCheckIn());
  }

  @Test
  public void testCheckIn() {

    Enrollment en =
        enrollmentRepository.save(EnrollmentRepositoryTest.createEnrollmentWithSchedules(0, 0, 0));

    CheckIn ci =
        createCheckIn(en.getId(), CheckInStatus.PENDING, CheckInType.ORAL, LocalDate.now());

    // verify that this is 1st check-in
    CheckInData checkInData = service.getCheckInData(query(0));
    assertTrue(checkInData.getIsFirst());

    submitCheckIn(ci.getId(), "question", "answer");

    ci = checkInRepository.findById(ci.getId()).get();

    assertNotNull(ci.getId());
    assertEquals(ci.getStatus(), CheckInStatus.COMPLETED);

    // Verify that this is not 1st check-in
    checkInData = service.getCheckInData(query(0));
    assertFalse(checkInData.getIsFirst());
  }

  @Test
  public void testCheckIn_isFirst_True() {

    Enrollment en =
        enrollmentRepository.save(EnrollmentRepositoryTest.createEnrollmentWithSchedules(0, 0, 0));

    CheckIn ci =
        createCheckIn(en.getId(), CheckInStatus.PENDING, CheckInType.ORAL, LocalDate.now());

    // verify that this is 1st check-in
    CheckInData checkInData = service.getCheckInData(query(0));
    assertTrue(checkInData.getIsFirst());

    submitCheckIn(ci.getId(), "question", "answer");

    ci = checkInRepository.findById(ci.getId()).get();

    assertNotNull(ci.getId());
    assertEquals(ci.getStatus(), CheckInStatus.COMPLETED);

    CheckIn ciTwo =
        createCheckIn(en.getId(), CheckInStatus.PENDING, CheckInType.ORAL, LocalDate.now());

    // Verify that this is not 1st check-in as they haven't submitted a medication start date yet
    CheckInData checkInDataTwo = service.getCheckInData(query(0));
    assertTrue(checkInDataTwo.getIsFirst());
  }

  @Test
  public void testCheckIn_FirstCheckInResponseDate_True() {

    Long id = 25L;

    Enrollment en =
        enrollmentRepository.save(
            EnrollmentRepositoryTest.createEnrollmentWithSchedules(id, id, id));

    CheckIn ci =
        createCheckIn(en.getId(), CheckInStatus.PENDING, CheckInType.ORAL, LocalDate.now());

    LocalDate patientReportedStartDate = LocalDate.now();

    SurveyItemPayload surveyItemPayload = new SurveyItemPayload();
    surveyItemPayload.setPayload(new HashMap<>());
    surveyItemPayload.getPayload().put(CheckInService.MEDICATION_TAKEN_QUESTION_ID, "yes");
    surveyItemPayload
        .getPayload()
        .put(CheckInService.MEDICATION_TAKEN_DATE, patientReportedStartDate.toString());
    surveyItemPayload.setId(ci.getId());

    SurveyPayload surveyPayload = new SurveyPayload();
    surveyPayload.setOral(Arrays.asList(surveyItemPayload));

    ClinicConfig clinicConfig = new ClinicConfig();
    Map<String, Boolean> features = new HashMap<>();
    features.put("patient-reported-start-date", true);
    clinicConfig.setFeatures(features);

    Mockito.when(surveyConfigService.getClinicConfig(id)).thenReturn(clinicConfig);

    // Act
    service.checkIn(surveyPayload);

    // Assert
    Enrollment result = enrollmentRepository.findById(en.getId()).get();

    assertNotNull(result.getFirstCheckInResponseDate());
  }

  @Test
  public void testCheckIn_FirstCheckInResponseDate_False() {
    Long id = 35L;

    Enrollment en =
        enrollmentRepository.save(
            EnrollmentRepositoryTest.createEnrollmentWithSchedules(id, id, id));

    // create completed check in prior to flag being set
    createCheckIn(
        en.getId(), CheckInStatus.COMPLETED, CheckInType.ORAL, LocalDate.now().minusDays(1), true);

    CheckIn ci =
        createCheckIn(en.getId(), CheckInStatus.PENDING, CheckInType.ORAL, LocalDate.now());

    LocalDate patientReportedStartDate = LocalDate.now();

    SurveyItemPayload surveyItemPayload = new SurveyItemPayload();
    surveyItemPayload.setPayload(new HashMap<>());
    surveyItemPayload.getPayload().put(CheckInService.MEDICATION_TAKEN_QUESTION_ID, "yes");
    surveyItemPayload
        .getPayload()
        .put(CheckInService.MEDICATION_TAKEN_DATE, patientReportedStartDate.toString());
    surveyItemPayload.setId(ci.getId());

    SurveyPayload surveyPayload = new SurveyPayload();
    surveyPayload.setOral(Arrays.asList(surveyItemPayload));

    ClinicConfig clinicConfig = new ClinicConfig();
    Map<String, Boolean> features = new HashMap<>();
    features.put("patient-reported-start-date", true);
    clinicConfig.setFeatures(features);

    Mockito.when(surveyConfigService.getClinicConfig(id)).thenReturn(clinicConfig);

    // Act
    service.checkIn(surveyPayload);

    // Assert
    Enrollment result = enrollmentRepository.findById(en.getId()).get();

    assertNull(result.getFirstCheckInResponseDate());
  }

  @Test
  public void testCheckIn_isFirst_False_MedicationTakenYes() {

    Enrollment en =
        enrollmentRepository.save(EnrollmentRepositoryTest.createEnrollmentWithSchedules(0, 0, 0));

    CheckIn ci =
        createCheckIn(en.getId(), CheckInStatus.PENDING, CheckInType.ORAL, LocalDate.now());

    // verify that this is 1st check-in
    CheckInData checkInData = service.getCheckInData(query(0));
    assertTrue(checkInData.getIsFirst());

    submitCheckIn(ci.getId(), "medicationTaken", "yes");

    ci = checkInRepository.findById(ci.getId()).get();

    assertNotNull(ci.getId());
    assertEquals(ci.getStatus(), CheckInStatus.COMPLETED);

    CheckIn ciTwo =
        createCheckIn(en.getId(), CheckInStatus.PENDING, CheckInType.ORAL, LocalDate.now());

    // Verify that this is not 1st check-in as they haven't submitted a medication start date yet
    CheckInData checkInDataTwo = service.getCheckInData(query(0));
    assertFalse(checkInDataTwo.getIsFirst());
  }

  @Test
  public void testCheckIn_isFirst_True_MedicationTakenNo() {

    Enrollment en =
        enrollmentRepository.save(EnrollmentRepositoryTest.createEnrollmentWithSchedules(0, 0, 0));

    CheckIn ci =
        createCheckIn(en.getId(), CheckInStatus.PENDING, CheckInType.ORAL, LocalDate.now());

    // verify that this is 1st check-in
    CheckInData checkInData = service.getCheckInData(query(0));
    assertTrue(checkInData.getIsFirst());

    submitCheckIn(ci.getId(), "medicationTaken", "no");

    ci = checkInRepository.findById(ci.getId()).get();

    assertNotNull(ci.getId());
    assertEquals(ci.getStatus(), CheckInStatus.COMPLETED);

    CheckIn ciTwo =
        createCheckIn(en.getId(), CheckInStatus.PENDING, CheckInType.ORAL, LocalDate.now());

    // Verify that this is not 1st check-in as they haven't submitted a medication start date yet
    CheckInData checkInDataTwo = service.getCheckInData(query(0));
    assertTrue(checkInDataTwo.getIsFirst());
  }

  @Test
  public void testCheckIn_FirstCheckIn_OnOffDay_WithMissed() {
    Long id = 35L;

    Enrollment en =
        enrollmentRepository.save(
            EnrollmentRepositoryTest.createEnrollmentWithSchedules(id, id, id));

    LocalDate missedCheckInDate = LocalDate.now().minusDays(1);
    CheckIn ci =
        createCheckIn(en.getId(), CheckInStatus.MISSED, CheckInType.ORAL, missedCheckInDate);

    LocalDate patientReportedStartDate = LocalDate.now().minusDays(2);

    SurveyItemPayload startedPayload = new SurveyItemPayload();
    startedPayload.setPayload(new HashMap<>());
    startedPayload.getPayload().put(CheckInService.MEDICATION_STARTED_QUESTION_ID, "yes");
    startedPayload
        .getPayload()
        .put(CheckInService.MEDICATION_TAKEN_DATE, patientReportedStartDate.toString());

    SurveyItemPayload missedPayload = new SurveyItemPayload();
    missedPayload.setId(ci.getId());
    missedPayload.setPayload(new HashMap<>());
    missedPayload.getPayload().put(CheckInService.MEDICATION_TAKEN_QUESTION_ID, "no");
    missedPayload
        .getPayload()
        .put(CheckInService.MEDICATION_TAKEN_DATE, missedCheckInDate.toString());

    List<SurveyItemPayload> surveyItemPayloads = Arrays.asList(startedPayload, missedPayload);

    SurveyPayload surveyPayload = new SurveyPayload();
    surveyPayload.setOral(surveyItemPayloads);

    ClinicConfig clinicConfig = new ClinicConfig();
    Map<String, Boolean> features = new HashMap<>();
    features.put("patient-reported-start-date", true);
    clinicConfig.setFeatures(features);

    Mockito.when(surveyConfigService.getClinicConfig(id)).thenReturn(clinicConfig);

    // Act
    service.checkIn(surveyPayload);

    // Assert
    List<CheckIn> result = checkInRepository.findByEnrollmentId(en.getId());

    assert (result.size() == 2);
  }

  @Test
  public void testCheckIn_FirstCheckIn_OnOffDay_WithoutMissed() {
    Long id = 35L;

    Enrollment en =
        enrollmentRepository.save(
            EnrollmentRepositoryTest.createEnrollmentWithSchedules(id, id, id));

    LocalDate patientReportedStartDate = LocalDate.now().minusDays(2);
    SurveyItemPayload startedPayload = new SurveyItemPayload();
    startedPayload.setPayload(new HashMap<>());
    startedPayload.getPayload().put(CheckInService.MEDICATION_STARTED_QUESTION_ID, "yes");
    startedPayload
        .getPayload()
        .put(CheckInService.MEDICATION_TAKEN_DATE, patientReportedStartDate.toString());

    SurveyPayload surveyPayload = new SurveyPayload();
    surveyPayload.setOral(Arrays.asList(startedPayload));
    surveyPayload.setEnrollmentId(en.getId());

    ClinicConfig clinicConfig = new ClinicConfig();
    Map<String, Boolean> features = new HashMap<>();
    features.put("patient-reported-start-date", true);
    clinicConfig.setFeatures(features);

    Mockito.when(surveyConfigService.getClinicConfig(id)).thenReturn(clinicConfig);

    // Act
    service.checkIn(surveyPayload);

    // Assert
    List<CheckIn> result = checkInRepository.findByEnrollmentId(en.getId());

    assert (result.size() == 1);
  }

  @Test
  public void testMedicationTaken() {

    Long id = 1L;

    Enrollment en =
        enrollmentRepository.save(
            EnrollmentRepositoryTest.createEnrollmentWithSchedules(id, id, id));
    CheckIn ci =
        createCheckIn(en.getId(), CheckInStatus.PENDING, CheckInType.ORAL, LocalDate.now());
    submitCheckIn(
        ci.getId(),
        CheckInService.MEDICATION_TAKEN_QUESTION_ID,
        CheckInService.MEDICATION_TAKEN_ANSWER_ID);

    CheckInData checkInData = service.getCheckInData(query(id));
    checkInData.getPending().forEach(pending -> assertTrue(pending.getMedicationTaken()));
  }

  @Test
  public void testMedicationStarted() {

    Long id = 8L;

    Enrollment en =
        enrollmentRepository.save(
            EnrollmentRepositoryTest.createEnrollmentWithSchedules(id, id, id));
    CheckIn ci =
        createCheckIn(en.getId(), CheckInStatus.PENDING, CheckInType.ORAL, LocalDate.now());
    submitCheckIn(
        ci.getId(),
        CheckInService.MEDICATION_STARTED_QUESTION_ID,
        CheckInService.MEDICATION_TAKEN_ANSWER_ID);

    CheckInData checkInData = service.getCheckInData(query(id));
    checkInData.getPending().forEach(pending -> assertTrue(pending.getMedicationTaken()));
  }

  @Test
  public void testMedicationStartDateChanged() {
    ClinicConfig clinicConfig = new ClinicConfig();
    Map<String, Boolean> features = new HashMap<>();
    features.put("patient-reported-start-date", true);
    clinicConfig.setFeatures(features);

    Long id = 5L;
    CheckInSchedule schedule = new CheckInSchedule();
    schedule.setCheckInFrequency(CheckInFrequency.DAILY);
    schedule.setStartDate(LocalDate.now());
    schedule.setEndDate(LocalDate.now().plusDays(1));
    schedule.setCheckInType(CheckInType.ORAL);

    Enrollment en =
        enrollmentRepository.save(
            EnrollmentRepositoryTest.createEnrollmentWithSchedules(id, id, id, schedule));
    Mockito.when(surveyConfigService.getClinicConfig(en.getClinicId())).thenReturn(clinicConfig);

    long timestamp =
        DateTimeUtils.toZonedDateTime(LocalDateTime.now().minusDays(2), en.getReminderTimeZone())
            .toInstant()
            .toEpochMilli();

    CheckIn ci =
        createCheckIn(en.getId(), CheckInStatus.PENDING, CheckInType.ORAL, LocalDate.now());
    submitCheckIn(
        ci.getId(), CheckInService.MEDICATION_STARTED_DATE_QUESTIONS_ID, String.valueOf(timestamp));

    Mockito.verify(schedulingService).schedule(any(), Mockito.eq(false));
  }

  @Test
  public void testMedicationStartDateEmpty() {
    Long id = 5L;
    CheckInSchedule schedule = new CheckInSchedule();
    schedule.setCheckInFrequency(CheckInFrequency.DAILY);
    schedule.setStartDate(LocalDate.now());
    schedule.setEndDate(LocalDate.now().plusDays(1));
    schedule.setCheckInType(CheckInType.ORAL);

    Enrollment en =
        enrollmentRepository.save(
            EnrollmentRepositoryTest.createEnrollmentWithSchedules(id, id, id, schedule));

    CheckIn ci =
        createCheckIn(en.getId(), CheckInStatus.PENDING, CheckInType.ORAL, LocalDate.now());
    submitCheckIn(
        ci.getId(), CheckInService.MEDICATION_STARTED_DATE_QUESTIONS_ID, StringUtils.EMPTY);

    Mockito.verify(schedulingService, Mockito.times(0)).schedule(any(), Mockito.anyBoolean());
  }

  @Test
  public void testCheckInData_changeScheduleIfRequested() {
    // Arrange
    Long id = 15L;
    CheckInSchedule schedule = new CheckInSchedule();
    schedule.setCheckInFrequency(CheckInFrequency.DAILY);
    schedule.setStartDate(LocalDate.now().minusDays(14));
    schedule.setEndDate(LocalDate.now().plusDays(14));
    schedule.setCheckInType(CheckInType.ORAL);

    Enrollment en =
        enrollmentRepository.save(
            EnrollmentRepositoryTest.createEnrollmentWithSchedules(id, id, id, schedule));

    CheckIn ci =
        createCheckIn(en.getId(), CheckInStatus.PENDING, CheckInType.ORAL, LocalDate.now());

    LocalDate patientReportedStartDate = LocalDate.now();

    SurveyItemPayload surveyItemPayload = new SurveyItemPayload();
    surveyItemPayload.setPayload(new HashMap<>());
    surveyItemPayload.getPayload().put(CheckInService.MEDICATION_TAKEN_QUESTION_ID, "yes");
    surveyItemPayload
        .getPayload()
        .put(CheckInService.MEDICATION_TAKEN_DATE, patientReportedStartDate.toString());
    surveyItemPayload.setId(ci.getId());

    SurveyPayload surveyPayload = new SurveyPayload();
    surveyPayload.setOral(Arrays.asList(surveyItemPayload));

    ClinicConfig clinicConfig = new ClinicConfig();
    Map<String, Boolean> features = new HashMap<>();
    features.put("patient-reported-start-date", true);
    clinicConfig.setFeatures(features);

    Mockito.when(surveyConfigService.getClinicConfig(id)).thenReturn(clinicConfig);

    // Act
    service.checkIn(surveyPayload);

    Enrollment result = enrollmentRepository.findById(en.getId()).get();

    // Assert
    assertTrue(
        patientReportedStartDate.getDayOfYear()
            == result.getPatientReportedTxStartDate().getDayOfYear());
    assertTrue(
        patientReportedStartDate.getDayOfYear() == result.getReminderStartDate().getDayOfYear());
    Mockito.verify(schedulingService, Mockito.times(1)).schedule(any(), Mockito.anyBoolean());
  }

  @Test
  public void testCheckInData_medicationStartedDateSet() {
    // Arrange
    Long id = 15L;
    CheckInSchedule schedule = new CheckInSchedule();
    schedule.setCheckInFrequency(CheckInFrequency.DAILY);
    schedule.setStartDate(LocalDate.now().minusDays(14));
    schedule.setEndDate(LocalDate.now().plusDays(14));
    schedule.setCheckInType(CheckInType.ORAL);

    Enrollment en =
        enrollmentRepository.save(
            EnrollmentRepositoryTest.createEnrollmentWithSchedules(id, id, id, schedule));

    CheckIn ci =
        createCheckIn(en.getId(), CheckInStatus.PENDING, CheckInType.ORAL, LocalDate.now());

    LocalDate patientReportedStartDate = LocalDate.now();

    SurveyItemPayload surveyItemPayload = new SurveyItemPayload();
    surveyItemPayload.setPayload(new HashMap<>());
    surveyItemPayload.getPayload().put(CheckInService.MEDICATION_TAKEN_QUESTION_ID, "yes");
    surveyItemPayload
        .getPayload()
        .put(CheckInService.MEDICATION_TAKEN_DATE, patientReportedStartDate.toString());
    surveyItemPayload.setId(ci.getId());

    SurveyPayload surveyPayload = new SurveyPayload();
    surveyPayload.setOral(Arrays.asList(surveyItemPayload));

    ClinicConfig clinicConfig = new ClinicConfig();
    Map<String, Boolean> features = new HashMap<>();
    features.put("patient-reported-start-date", true);
    clinicConfig.setFeatures(features);

    Mockito.when(surveyConfigService.getClinicConfig(id)).thenReturn(clinicConfig);

    // Act
    service.checkIn(surveyPayload);

    // Assert
    assertNotNull(
        surveyItemPayload.getPayload().get(CheckInService.MEDICATION_STARTED_DATE_QUESTIONS_ID));
  }

  @Test
  public void givenThreeMissedSinceLastCompleted_shouldReturnThoseThree() {
    Long id = random.nextLong();
    CheckInSchedule schedule = new CheckInSchedule();
    schedule.setCheckInFrequency(CheckInFrequency.DAILY);
    schedule.setStartDate(LocalDate.now().minusDays(7));
    schedule.setEndDate(LocalDate.now().plusDays(6));
    schedule.setCheckInType(CheckInType.ORAL);

    Enrollment en =
        enrollmentRepository.save(
            EnrollmentRepositoryTest.createEnrollmentWithSchedules(id, id, id, schedule));

    CheckIn ci1 =
        createCheckIn(
            en.getId(), CheckInStatus.COMPLETED, CheckInType.ORAL, LocalDate.now().minusDays(5));
    CheckIn ci2 =
        createCheckIn(
            en.getId(), CheckInStatus.MISSED, CheckInType.ORAL, LocalDate.now().minusDays(4));
    CheckIn ci3 =
        createCheckIn(
            en.getId(), CheckInStatus.MISSED, CheckInType.ORAL, LocalDate.now().minusDays(3));
    CheckIn ci4 =
        createCheckIn(
            en.getId(), CheckInStatus.MISSED, CheckInType.ORAL, LocalDate.now().minusDays(2));
    CheckIn ci5 =
        createCheckIn(
            en.getId(), CheckInStatus.PENDING, CheckInType.ORAL, LocalDate.now().minusDays(1));

    CheckInData checkInData = service.getCheckInData(query(id));
    Assert.assertNotNull("should get checkInData object", checkInData);
    Assert.assertNotNull("should get missed checkins", checkInData.getMissed());
    Assert.assertEquals("should get three missed checkins", checkInData.getMissed().size(), 3);
  }

  @Test
  public void givenThreeMissedButOnlyOneSinceLastCompleted_shouldReturnThatOne() {
    Long id = random.nextLong();
    CheckInSchedule schedule = new CheckInSchedule();
    schedule.setCheckInFrequency(CheckInFrequency.DAILY);
    schedule.setStartDate(LocalDate.now().minusDays(7));
    schedule.setEndDate(LocalDate.now().plusDays(6));
    schedule.setCheckInType(CheckInType.ORAL);

    Enrollment en =
        enrollmentRepository.save(
            EnrollmentRepositoryTest.createEnrollmentWithSchedules(id, id, id, schedule));

    CheckIn ci2 =
        createCheckIn(
            en.getId(), CheckInStatus.MISSED, CheckInType.ORAL, LocalDate.now().minusDays(5));
    CheckIn ci3 =
        createCheckIn(
            en.getId(), CheckInStatus.MISSED, CheckInType.ORAL, LocalDate.now().minusDays(4));
    CheckIn ci1 =
        createCheckIn(
            en.getId(), CheckInStatus.COMPLETED, CheckInType.ORAL, LocalDate.now().minusDays(3));
    CheckIn ci4 =
        createCheckIn(
            en.getId(), CheckInStatus.MISSED, CheckInType.ORAL, LocalDate.now().minusDays(2));
    CheckIn ci5 =
        createCheckIn(
            en.getId(), CheckInStatus.PENDING, CheckInType.ORAL, LocalDate.now().minusDays(1));

    CheckInData checkInData = service.getCheckInData(query(id));
    Assert.assertNotNull("should get checkInData object", checkInData);
    Assert.assertNotNull("should get missed checkins", checkInData.getMissed());
    Assert.assertEquals("should get three missed checkins", checkInData.getMissed().size(), 1);
  }

  @Test
  public void givenSevenMissed_shouldReturnThatSix() {
    Long id = random.nextLong();
    CheckInSchedule schedule = new CheckInSchedule();
    schedule.setCheckInFrequency(CheckInFrequency.DAILY);
    schedule.setStartDate(LocalDate.now().minusDays(0));
    schedule.setEndDate(LocalDate.now().plusDays(6));
    schedule.setCheckInType(CheckInType.ORAL);

    Enrollment en =
        enrollmentRepository.save(
            EnrollmentRepositoryTest.createEnrollmentWithSchedules(id, id, id, schedule));

    createCheckIn(en.getId(), CheckInStatus.MISSED, CheckInType.ORAL, LocalDate.now().minusDays(7));
    createCheckIn(en.getId(), CheckInStatus.MISSED, CheckInType.ORAL, LocalDate.now().minusDays(6));
    createCheckIn(en.getId(), CheckInStatus.MISSED, CheckInType.ORAL, LocalDate.now().minusDays(5));
    createCheckIn(en.getId(), CheckInStatus.MISSED, CheckInType.ORAL, LocalDate.now().minusDays(4));
    createCheckIn(en.getId(), CheckInStatus.MISSED, CheckInType.ORAL, LocalDate.now().minusDays(3));
    createCheckIn(en.getId(), CheckInStatus.MISSED, CheckInType.ORAL, LocalDate.now().minusDays(2));
    createCheckIn(en.getId(), CheckInStatus.MISSED, CheckInType.ORAL, LocalDate.now().minusDays(1));

    CheckInData checkInData = service.getCheckInData(query(id));
    Assert.assertNotNull("should get checkInData object", checkInData);
    Assert.assertNotNull("should get missed checkins", checkInData.getMissed());
    Assert.assertEquals("should get three missed checkins", checkInData.getMissed().size(), 6);
  }

  @Test
  public void backfillShouldIgnoreIds() {
    CheckIn checkin =
        createCheckIn(
            "backfillShouldIgnoreIds",
            CheckInStatus.COMPLETED,
            CheckInType.ORAL,
            LocalDate.now().minusDays(1),
            true);
    checkin.setId("please dont set me!");

    CheckIn updated = service.checkInBackfill(checkin);

    assertNotEquals(updated.getId(), "please dont set me!");
  }

  private void submitCheckIn(String id, String question, String answer) {
    SurveyItemPayload surveyItemPayload = new SurveyItemPayload();
    surveyItemPayload.setPayload(new HashMap<>());
    surveyItemPayload.getPayload().put(question, answer);
    surveyItemPayload.setId(id);

    SurveyPayload surveyPayload = new SurveyPayload();
    surveyPayload.setOral(Arrays.asList(surveyItemPayload));

    service.checkIn(surveyPayload);
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

  private EnrollmentQuery query(long i) {
    EnrollmentQuery query = new EnrollmentQuery();
    query.setLocationId(Arrays.asList(i));
    query.setClinicId(Arrays.asList(i));
    query.setPatientId(Arrays.asList(i));
    return query;
  }
}
