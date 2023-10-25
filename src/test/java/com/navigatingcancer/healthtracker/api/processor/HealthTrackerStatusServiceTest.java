package com.navigatingcancer.healthtracker.api.processor;

import static org.mockito.Mockito.never;

import com.navigatingcancer.healthtracker.api.TestConfig;
import com.navigatingcancer.healthtracker.api.data.client.PatientInfoServiceClient;
import com.navigatingcancer.healthtracker.api.data.model.*;
import com.navigatingcancer.healthtracker.api.data.model.patientInfo.PatientInfo;
import com.navigatingcancer.healthtracker.api.data.model.survey.SurveyItemPayload;
import com.navigatingcancer.healthtracker.api.data.model.survey.SurveyPayload;
import com.navigatingcancer.healthtracker.api.data.repo.CheckInRepository;
import com.navigatingcancer.healthtracker.api.data.repo.EnrollmentRepository;
import com.navigatingcancer.healthtracker.api.data.repo.HealthTrackerEventsRepository;
import com.navigatingcancer.healthtracker.api.data.repo.HealthTrackerStatusRepository;
import com.navigatingcancer.healthtracker.api.data.repo.proReview.ProReviewRepository;
import com.navigatingcancer.healthtracker.api.data.service.CheckInService;
import com.navigatingcancer.healthtracker.api.data.service.impl.PatientRecordService;
import com.navigatingcancer.healthtracker.api.data.service.impl.SchedulingServiceImpl;
import com.navigatingcancer.healthtracker.api.processor.model.HealthTrackerStatusCommand;
import com.navigatingcancer.notification.client.service.NotificationServiceClient;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
@Import(TestConfig.class)
public class HealthTrackerStatusServiceTest {

  @Autowired EnrollmentRepository enrollmentRepository;

  @Autowired CheckInRepository checkinRepository;

  @Autowired HealthTrackerStatusService healthTrackerStatusService;

  @Autowired HealthTrackerStatusRepository healthTrackerStatusRepository;

  @Autowired HealthTrackerEventsRepository eventsRepository;

  @MockBean private PatientInfoServiceClient patientInfoClient;

  @MockBean private NotificationServiceClient notificationServiceClient;

  @MockBean private SchedulingServiceImpl schedulingService;

  @MockBean private RabbitTemplate rabbitTemplate;

  @Autowired private PatientRecordService patientRecordService;
  @Autowired private ProReviewRepository proReviewRepository;

  private Long patientId = 5L;

  @Before
  public void setup() {
    enrollmentRepository.deleteAll();
    checkinRepository.deleteAll();
    healthTrackerStatusRepository.deleteAll();
    proReviewRepository.deleteAll();

    PatientInfo patientInfo = new PatientInfo();
    patientInfo.setHighRisk(true);
    patientInfo.setId(patientId);

    PatientInfoServiceClient.FeignClient client =
        Mockito.mock(PatientInfoServiceClient.FeignClient.class);
    Mockito.when(patientInfoClient.getApi()).thenReturn(client);
    Mockito.when(client.getPatients(Mockito.any(), Mockito.any()))
        .thenReturn(Arrays.asList(patientInfo));
  }

  public static SurveyItemPayload createSurvey(String... items) {
    SurveyItemPayload item = new SurveyItemPayload();
    item.setPayload(new HashMap<>());
    for (int i = 0; i < items.length; i = i + 2) {
      item.getPayload().put(items[i], items[i + 1]);
    }
    return item;
  }

  public static Enrollment createEnrollment(
      EnrollmentRepository enrollmentRepository,
      CheckInRepository checkinRepository,
      Long clinicId,
      Long locationId,
      Long patientId,
      LocalDate scheduledDate,
      SurveyItemPayload... items) {
    return createEnrollment(
        enrollmentRepository,
        checkinRepository,
        clinicId,
        locationId,
        patientId,
        scheduledDate,
        null,
        items);
  }

  public static Enrollment createEnrollment(
      EnrollmentRepository enrollmentRepository,
      CheckInRepository checkinRepository,
      Long clinicId,
      Long locationId,
      Long patientId,
      LocalDate scheduledDate,
      LocalDate schedulEndDate,
      SurveyItemPayload... items) {
    Enrollment enrollment = new Enrollment();
    enrollment.setClinicId(clinicId);
    enrollment.setLocationId(locationId);
    enrollment.setPatientId(patientId);
    enrollment.setMedication("Xtandi");
    enrollment.setStatus(EnrollmentStatus.ACTIVE);
    enrollment.setReminderTimeZone("America/Los_Angeles");
    enrollment.setSchedules(new ArrayList<CheckInSchedule>());

    CheckInSchedule checkInSchedule = new CheckInSchedule();
    checkInSchedule.setCheckInType(CheckInType.ORAL);
    checkInSchedule.setStartDate(scheduledDate);
    checkInSchedule.setEndDate(schedulEndDate);
    checkInSchedule.setCheckInFrequency(CheckInFrequency.DAILY);
    enrollment.getSchedules().add(checkInSchedule);

    enrollment = enrollmentRepository.save(enrollment);

    for (SurveyItemPayload item : items) {
      CheckIn checkIn = new CheckIn();
      checkIn.setCheckInType(CheckInType.ORAL);
      checkIn.setScheduleDate(scheduledDate);
      checkIn.setSurveyPayload(item);
      checkIn.setStatus(CheckInStatus.COMPLETED);
      checkIn.setEnrollmentId(enrollment.getId());

      checkinRepository.save(checkIn);

      scheduledDate = scheduledDate.minusDays(1);
    }

    return enrollment;
  }

  /**
   * helper function to create and persist an Enrollment with no checkins
   *
   * @param enrollmentRepository
   * @param clinicId
   * @param locationId
   * @param patientId
   * @param medication
   * @param status
   * @param timezone
   * @param schedules
   * @return
   */
  public static Enrollment createEnrollment(
      EnrollmentRepository enrollmentRepository,
      Long clinicId,
      Long locationId,
      Long patientId,
      String medication,
      EnrollmentStatus status,
      String timezone,
      LocalDate startDate,
      Integer daysInCycle,
      Integer cycles,
      List<CheckInSchedule> schedules) {
    Enrollment enrollment = new Enrollment();
    enrollment.setId(ObjectId.get().toHexString());
    enrollment.setClinicId(clinicId);
    enrollment.setLocationId(locationId);
    enrollment.setPatientId(patientId);
    enrollment.setMedication(medication);
    enrollment.setStatus(status);
    enrollment.setReminderTimeZone(timezone);
    enrollment.setSchedules(schedules);
    enrollment.setReminderStartDate(startDate);
    enrollment.setDaysInCycle(daysInCycle);
    enrollment.setCycles(cycles);
    enrollment = enrollmentRepository.save(enrollment);

    return enrollment;
  }

  /**
   * helper method to create and persist a CheckIn
   *
   * @param checkInRepository
   * @param type
   * @param status
   * @param scheduledDate
   * @param enrollmentId
   * @param payload
   * @return
   */
  public static CheckIn createCheckIn(
      CheckInRepository checkInRepository,
      CheckInType type,
      CheckInStatus status,
      LocalDate scheduledDate,
      String enrollmentId,
      SurveyItemPayload payload) {
    CheckIn checkIn = new CheckIn();
    checkIn.setCheckInType(type);
    checkIn.setScheduleDate(scheduledDate);
    checkIn.setSurveyPayload(payload);
    checkIn.setStatus(status);
    checkIn.setEnrollmentId(enrollmentId);
    checkInRepository.save(checkIn);
    return checkIn;
  }

  public static CheckInSchedule createCheckInSchedule(
      CheckInType type,
      LocalDate startDate,
      LocalDate endDate,
      CheckInFrequency frequency,
      List<Integer> cycleDays,
      List<Integer> weeklyDays) {
    CheckInSchedule checkInSchedule = new CheckInSchedule();
    checkInSchedule.setCheckInType(type);
    checkInSchedule.setStartDate(startDate);
    checkInSchedule.setEndDate(endDate);
    checkInSchedule.setCheckInFrequency(frequency);
    checkInSchedule.setCycleDays(cycleDays);
    checkInSchedule.setWeeklyDays(weeklyDays);
    return checkInSchedule;
  }

  private CheckIn createCheckIn(
      String enrollmentId, LocalDate scheduledDate, SurveyItemPayload item) {
    CheckIn checkIn = new CheckIn();
    checkIn.setCheckInType(CheckInType.ORAL);
    checkIn.setScheduleDate(scheduledDate);
    checkIn.setSurveyPayload(item);
    checkIn.setStatus(
        item != null && !item.getPayload().isEmpty()
            ? CheckInStatus.COMPLETED
            : CheckInStatus.MISSED);
    checkIn.setEnrollmentId(enrollmentId);

    return checkinRepository.save(checkIn);
  }

  public static Enrollment createOralEnrollment(
      EnrollmentRepository enrollmentRepository,
      long clinicId,
      long locationId,
      long patientId,
      LocalDate startDate,
      LocalDate endDate) {
    Enrollment enrollment = new Enrollment();
    enrollment.setClinicId(clinicId);
    enrollment.setLocationId(locationId);
    enrollment.setPatientId(patientId);
    enrollment.setMedication("Xtandi");
    enrollment.setStatus(EnrollmentStatus.ACTIVE);
    enrollment.setReminderTimeZone("America/Los_Angeles");
    enrollment.setTxStartDate(startDate);
    enrollment.setSchedules(new ArrayList<>());

    CheckInSchedule checkInSchedule = new CheckInSchedule();
    checkInSchedule.setCheckInType(CheckInType.ORAL);
    checkInSchedule.setStartDate(startDate);
    checkInSchedule.setEndDate(endDate);
    checkInSchedule.setCheckInFrequency(CheckInFrequency.DAILY);
    enrollment.getSchedules().add(checkInSchedule);

    enrollment = enrollmentRepository.save(enrollment);

    return enrollment;
  }

  public static List<CheckIn> createOralCheckInsForEnrollment(
      CheckInRepository checkinRepository, Enrollment enrollment, SurveyItemPayload... items) {
    LocalDate startDate = enrollment.getStartDate();
    List<CheckIn> checkIns = new ArrayList<>();
    for (SurveyItemPayload item : items) {
      CheckIn checkIn = new CheckIn();
      checkIn.setCheckInType(CheckInType.ORAL);
      checkIn.setScheduleDate(startDate);
      checkIn.setSurveyPayload(item);
      checkIn.setStatus(CheckInStatus.COMPLETED);
      checkIn.setEnrollmentId(enrollment.getId());

      checkIns.add(checkinRepository.save(checkIn));

      startDate = startDate.minusDays(1);
    }

    return checkIns;
  }

  private Enrollment createEnrollment(
      Long clinicId, Long locationId, Long patientId, SurveyItemPayload... items) {
    return createEnrollment(
        enrollmentRepository,
        checkinRepository,
        clinicId,
        locationId,
        patientId,
        LocalDate.now(),
        items);
  }

  private Enrollment createStatus(
      Long clinicId, Long locationId, Long patientId, SurveyItemPayload... items) {
    Enrollment enrollment = createEnrollment(clinicId, locationId, patientId, items);
    HealthTrackerStatusCommand command =
        new HealthTrackerStatusCommand(
            enrollment.getId(), null, List.of(ObjectId.get().toHexString()));
    healthTrackerStatusService.accept(command);
    return enrollment;
  }

  private void verifyClinic(Long clinic) {
    List<HealthTrackerStatus> statuses =
        healthTrackerStatusService.getOrCreateNewStatus(
            Arrays.asList(clinic), Arrays.asList(clinic), Arrays.asList(patientId));
    Assert.assertEquals(1, statuses.size());
    statuses.forEach(
        s -> {
          Assert.assertEquals(clinic, s.getClinicId());
          Assert.assertEquals(clinic, s.getLocationId());
          Assert.assertEquals(patientId, s.getPatientInfo().getId());
        });
  }

  @Test
  public void testHealthResultPersistedAndQuery() {

    Long clinic1 = 1234L;
    Long clinic2 = 2L;

    createStatus(clinic1, clinic1, clinic1, createSurvey());
    createStatus(clinic2, clinic2, clinic1, createSurvey());

    verifyClinic(clinic1);
    verifyClinic(clinic2);
  }

  @Test
  public void testHealthResultPersistedIfNoCheckins() {

    Long clinic = 3L;
    createStatus(clinic, clinic, clinic);

    verifyClinic(clinic);
  }

  @Test
  public void testAdherencePercent() {
    Long clinic = 6L;

    createStatus(
        clinic,
        clinic,
        clinic,
        createSurvey(
            CheckInService.MEDICATION_TAKEN_QUESTION_ID, CheckInService.MEDICATION_TAKEN_ANSWER_ID),
        createSurvey(
            CheckInService.MEDICATION_TAKEN_QUESTION_ID,
            CheckInService.MEDICATION_NOT_TAKEN_ANSWER_ID),
        createSurvey(
            CheckInService.MEDICATION_TAKEN_QUESTION_ID,
            CheckInService.MEDICATION_NOT_TAKEN_ANSWER_ID),
        createSurvey(
            CheckInService.MEDICATION_TAKEN_QUESTION_ID,
            CheckInService.MEDICATION_NOT_TAKEN_ANSWER_ID));
    List<HealthTrackerStatus> statuses =
        healthTrackerStatusService.getOrCreateNewStatus(
            Arrays.asList(clinic), Arrays.asList(clinic), null);
    Assert.assertEquals(1, statuses.size());
    statuses.forEach(
        status -> {
          Assert.assertEquals((int) status.getAdherencePercent(), 25);
        });
  }

  @Test
  public void testAdherencePercent_RegressionSingleCheckIn() {
    Long clinic = 57L;

    createStatus(
        clinic,
        clinic,
        clinic,
        createSurvey(
            CheckInService.MEDICATION_TAKEN_QUESTION_ID,
            CheckInService.MEDICATION_TAKEN_ANSWER_ID));
    List<HealthTrackerStatus> statuses =
        healthTrackerStatusService.getOrCreateNewStatus(
            Arrays.asList(clinic), Arrays.asList(clinic), null);
    Assert.assertEquals(1, statuses.size());
    statuses.forEach(
        status -> {
          Assert.assertEquals((int) status.getAdherencePercent(), 100);
        });
  }

  @Test
  public void testAcceptWithOralAndSymptomCheckin() {
    Long clinic = 63L;

    createStatus(
        clinic,
        clinic,
        clinic,
        createSurvey(
            CheckInService.MEDICATION_TAKEN_QUESTION_ID, CheckInService.MEDICATION_TAKEN_ANSWER_ID),
        createSurvey(
            CheckInService.SYMPTOM_CHECKIN_QUESTION_ID,
            CheckInService.SYMPTOM_NONE_REPORTED_ANSWER_ID));
    List<HealthTrackerStatus> statuses =
        healthTrackerStatusService.getOrCreateNewStatus(
            Arrays.asList(clinic), Arrays.asList(clinic), null);
    Assert.assertEquals(1, statuses.size());
    statuses.forEach(
        status -> {
          Assert.assertEquals((int) status.getAdherencePercent(), 100);
        });
  }

  @Ignore // TODO: Test failes, fix it or remove it
  public void testAcceptWithTwoOralCheckins() {
    Long clinic = 63L;

    createStatus(
        clinic,
        clinic,
        clinic,
        createSurvey(
            CheckInService.MEDICATION_TAKEN_QUESTION_ID, CheckInService.MEDICATION_TAKEN_ANSWER_ID),
        createSurvey(
            CheckInService.MEDICATION_TAKEN_QUESTION_ID,
            CheckInService.MEDICATION_TAKEN_ANSWER_ID));
    List<HealthTrackerStatus> statuses =
        healthTrackerStatusService.getOrCreateNewStatus(
            Arrays.asList(clinic), Arrays.asList(clinic), null);
    Assert.assertEquals(1, statuses.size());
    statuses.forEach(
        status -> {
          Assert.assertEquals((int) status.getAdherencePercent(), 100);
        });
  }

  @Test
  public void testPROSentWithSymptoms() {
    // If no data, no rabbit message is going out
    Long clinic1 = 1L;
    Long location1 = 2L;
    Long patient1 = 3L;

    // if there are symptoms that must be reported
    Enrollment enrollment = createEnrollment(clinic1, location1, patient1);
    SurveyItemPayload sip =
        createSurvey(
            CheckInService.SYMPTOM_CHECKIN_QUESTION_ID, CheckInService.MEDICATION_TAKEN_ANSWER_ID,
            CheckInService.SYMPTOM_SWELLING_SEVERITY, CheckInService.SEVERE);
    SurveyPayload sp = new SurveyPayload();
    sp.content.setSymptoms(Arrays.asList(sip));
    HealthTrackerStatusCommand command =
        new HealthTrackerStatusCommand(enrollment.getId(), sp, List.of());
    healthTrackerStatusService.accept(command);

    ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
    Mockito.verify(rabbitTemplate).convertAndSend(Mockito.any(), argument.capture());
  }

  @Test
  public void testPROSentWithOral() {
    // If no data, no rabbit message is going out
    Long clinic1 = 1L;
    Long location1 = 2L;
    Long patient1 = 3L;

    // if there are symptoms that must be reported
    Enrollment enrollment = createEnrollment(clinic1, location1, patient1);
    SurveyItemPayload sip =
        createSurvey(
            CheckInService.MEDICATION_TAKEN_QUESTION_ID, CheckInService.MEDICATION_TAKEN_ANSWER_ID);
    SurveyPayload sp = new SurveyPayload();
    sp.content.setOral(List.of(sip));
    HealthTrackerStatusCommand command =
        new HealthTrackerStatusCommand(enrollment.getId(), sp, List.of());
    healthTrackerStatusService.accept(command);

    ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
    Mockito.verify(rabbitTemplate).convertAndSend(Mockito.any(), argument.capture());
  }

  @Test
  public void testPRONotSent() {
    // If no data, no rabbit message is going out
    Long clinic1 = 1L;
    Long location1 = 2L;
    Long patient1 = 3L;

    createStatus(
        clinic1,
        location1,
        patient1,
        createSurvey(
            CheckInService.MEDICATION_TAKEN_QUESTION_ID,
            CheckInService.MEDICATION_NOT_TAKEN_ANSWER_ID));

    ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
    Mockito.verify(rabbitTemplate, never()).convertAndSend(Mockito.any(), argument.capture());
  }

  @Test
  public void givenPendingCheckins_shouldIgnoreForProcessing() {
    // If no data, no rabbit message is going out
    Long clinic1 = 15L;
    Long location1 = 25L;
    Long patient1 = 35L;

    // if there are symptoms that must be reported
    Enrollment enrollment = createEnrollment(clinic1, location1, patient1);

    SurveyItemPayload sip =
        createSurvey(CheckInService.PAIN_FEVER_SEVERITY, CheckInService.VERY_SEVERE);
    SurveyPayload sp = new SurveyPayload();
    sp.content.setSymptoms(Arrays.asList(sip));
    HealthTrackerStatusCommand command =
        new HealthTrackerStatusCommand(enrollment.getId(), sp, List.of());
    CheckIn ci =
        DefaultDroolsServiceTest.createCheckInsFromSurveyPayload(
            CheckInType.SYMPTOM, LocalDate.now().minusDays(1), sip);
    ci.setEnrollmentId(enrollment.getId());
    ci = this.checkinRepository.insert(ci);

    CheckIn pending = new CheckIn();
    pending.setEnrollmentId(enrollment.getId());
    pending.setStatus(CheckInStatus.PENDING);
    pending.setCheckInType(CheckInType.SYMPTOM);
    pending.setScheduleDate(LocalDate.now());
    pending.setScheduleTime(LocalTime.of(9, 0));
    this.checkinRepository.insert(pending);

    healthTrackerStatusService.accept(command);

    // ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
    // Mockito.verify(rabbitTemplate).convertAndSend(Mockito.any(), argument.capture());

    HealthTrackerStatus htStatus = this.healthTrackerStatusRepository.getById(enrollment.getId());
    Assert.assertEquals(HealthTrackerStatusCategory.TRIAGE, htStatus.getCategory());
  }

  @Test
  public void whenMissThreeCheckIns_shouldSayThreeMissedCheckins() {
    Enrollment enrollment = createEnrollment(1L, 1L, 1L);
    CheckIn c2 = createCheckIn(enrollment.getId(), LocalDate.now().minusDays(2), null);
    CheckIn c3 = createCheckIn(enrollment.getId(), LocalDate.now().minusDays(1), null);
    CheckIn c4 = createCheckIn(enrollment.getId(), LocalDate.now(), null);

    HealthTrackerStatusCommand command =
        new HealthTrackerStatusCommand(enrollment.getId(), null, List.of());
    this.healthTrackerStatusService.accept(command);

    HealthTrackerStatus status = this.healthTrackerStatusRepository.getById(enrollment.getId());

    Assert.assertNotNull(status);
    Assert.assertEquals(HealthTrackerStatusCategory.ACTION_NEEDED, status.getCategory());
  }

  @Ignore // TODO: Test failes, fix it or remove it
  public void givenDifferentPatientReportedStartDate_medicationTakenDate() throws Exception {
    Long clinic = 63L;
    LocalDate enrollmentStartDate = LocalDate.now();
    LocalDate medicationStartedDate = enrollmentStartDate.minusDays(1L);

    Enrollment enrollment =
        createOralEnrollment(
            enrollmentRepository, clinic, clinic, clinic, enrollmentStartDate, null);

    // create checkIn with startDate changed
    SurveyItemPayload sip1 =
        createSurvey(
            CheckInService.MEDICATION_TAKEN_DATE,
            medicationStartedDate.toString(),
            CheckInService.MEDICATION_TAKEN_QUESTION_ID,
            CheckInService.MEDICATION_TAKEN_ANSWER_ID);

    var checkIns = createOralCheckInsForEnrollment(checkinRepository, enrollment, sip1);
    var checkInIds = checkIns.stream().map(AbstractDocument::getId).collect(Collectors.toList());

    // call htstatus.processStatus()
    HealthTrackerStatusCommand command =
        new HealthTrackerStatusCommand(enrollment.getId(), null, checkInIds);
    healthTrackerStatusService.accept(command);

    // verify htstatus.category == ACTION_NEEDED
    List<HealthTrackerStatus> statuses =
        healthTrackerStatusService.getOrCreateNewStatus(
            Arrays.asList(clinic), Arrays.asList(clinic), Arrays.asList(clinic));

    Assert.assertEquals(1, statuses.size());
    statuses.forEach(
        status -> {
          Assert.assertEquals(
              "Status should be ACTION_NEEDED due to start date changed",
              status.getCategory(),
              HealthTrackerStatusCategory.ACTION_NEEDED);
        });
  }

  @Ignore // TODO: Test failes, fix it or remove it
  public void givenDifferentPatientReportedStartDate_medicationStartedDate() throws Exception {
    Long clinic = 64L;
    LocalDate enrollmentStartDate = LocalDate.now();
    LocalDate medicationStartedDate = enrollmentStartDate.minusDays(1L);

    Enrollment enrollment =
        createOralEnrollment(
            enrollmentRepository, clinic, clinic, clinic, enrollmentStartDate, null);

    // create checkIn with startDate changed
    SurveyItemPayload sip1 =
        createSurvey(
            CheckInService.MEDICATION_STARTED_DATE_QUESTIONS_ID,
            medicationStartedDate.toString(),
            CheckInService.MEDICATION_TAKEN_QUESTION_ID,
            CheckInService.MEDICATION_TAKEN_ANSWER_ID);

    var checkIns = createOralCheckInsForEnrollment(checkinRepository, enrollment, sip1);
    var checkInIds = checkIns.stream().map(AbstractDocument::getId).collect(Collectors.toList());
    // call htstatus.processStatus()
    HealthTrackerStatusCommand command =
        new HealthTrackerStatusCommand(enrollment.getId(), null, checkInIds);
    healthTrackerStatusService.accept(command);

    // verify htstatus.category == ACTION_NEEDED
    List<HealthTrackerStatus> statuses =
        healthTrackerStatusService.getOrCreateNewStatus(
            Arrays.asList(clinic), Arrays.asList(clinic), Arrays.asList(clinic));

    Assert.assertEquals(1, statuses.size());
    statuses.forEach(
        status -> {
          Assert.assertEquals(
              "Status should be ACTION_NEEDED due to start date changed",
              status.getCategory(),
              HealthTrackerStatusCategory.ACTION_NEEDED);
        });
  }

  @Ignore // TODO: Test failes, fix it or remove it
  public void testOralCheckInWithStartDateChange_WillCreateActionNeededStatus() throws Exception {
    Long clinic = 62L;
    LocalDate enrollmentStartDate = LocalDate.now();
    LocalDate medicationStartedDate = enrollmentStartDate.minusDays(1L);

    Enrollment enrollment =
        createOralEnrollment(
            enrollmentRepository, clinic, clinic, clinic, enrollmentStartDate, null);

    // create checkIn with startDate changed
    SurveyItemPayload sip1 =
        createSurvey(
            CheckInService.MEDICATION_STARTED_DATE_QUESTIONS_ID,
            medicationStartedDate.toString(),
            CheckInService.MEDICATION_TAKEN_QUESTION_ID,
            CheckInService.MEDICATION_TAKEN_ANSWER_ID);

    var checkIns = createOralCheckInsForEnrollment(checkinRepository, enrollment, sip1);
    var checkInIds = checkIns.stream().map(AbstractDocument::getId).collect(Collectors.toList());

    // call htstatus.processStatus()
    HealthTrackerStatusCommand command =
        new HealthTrackerStatusCommand(enrollment.getId(), null, checkInIds);
    healthTrackerStatusService.accept(command);

    // verify htstatus.category == ACTION_NEEDED
    List<HealthTrackerStatus> statuses =
        healthTrackerStatusService.getOrCreateNewStatus(
            Arrays.asList(clinic), Arrays.asList(clinic), Arrays.asList(clinic));

    Assert.assertEquals(1, statuses.size());
    statuses.forEach(
        status -> {
          Assert.assertEquals(
              "Status should be ACTION_NEEDED due to start date changed",
              status.getCategory(),
              HealthTrackerStatusCategory.ACTION_NEEDED);
        });
  }

  @Test
  public void testSecondOralCheckIn_WillClearActionNeededStatus() throws Exception {
    Long clinic = 61L;
    LocalDate enrollmentStartDate = LocalDate.now();
    LocalDate medicationStartedDate = enrollmentStartDate.minusDays(1L);

    // create 1st checkIn with startDate changed
    SurveyItemPayload sip1 =
        createSurvey(
            CheckInService.MEDICATION_STARTED_DATE_QUESTIONS_ID,
            medicationStartedDate.toString(),
            CheckInService.MEDICATION_TAKEN_QUESTION_ID,
            CheckInService.MEDICATION_TAKEN_ANSWER_ID);

    // create 2nd checkIn without date changed
    SurveyItemPayload sip2 =
        createSurvey(
            CheckInService.MEDICATION_TAKEN_QUESTION_ID,
            CheckInService.MEDICATION_NOT_TAKEN_ANSWER_ID);

    // call htstatus.processStatus()
    createStatus(clinic, clinic, clinic, sip1, sip2);

    // verify htstatus.category != ACTION_NEEDED
    List<HealthTrackerStatus> statuses =
        healthTrackerStatusService.getOrCreateNewStatus(
            Arrays.asList(clinic), Arrays.asList(clinic), null);

    Assert.assertEquals(1, statuses.size());
    statuses.forEach(
        status -> {
          Assert.assertNotEquals(
              "Status should no longer be ACTION_NEEDED due to 2nd checkIn",
              status.getCategory(),
              HealthTrackerStatusCategory.ACTION_NEEDED);
        });
  }

  // Access to executor
  @Autowired Executor asyncExecutor;

  @Test
  public void testStatusCategoryChange() throws Exception {
    Long clinic = 62L;

    Enrollment enr =
        createStatus(
            clinic,
            clinic,
            clinic,
            createSurvey(
                CheckInService.MEDICATION_TAKEN_QUESTION_ID,
                CheckInService.MEDICATION_TAKEN_ANSWER_ID));

    HealthTrackerStatus status = healthTrackerStatusService.getById(enr.getId());
    Assert.assertNotNull(status);

    var checkins = checkinRepository.findByEnrollmentId(enr.getId());
    Assert.assertFalse("some test checkins create", checkins.isEmpty());
    var ciid = checkins.get(0).getId();

    // Try one change
    var cat2 = HealthTrackerStatusCategory.WATCH_CAREFULLY;
    HealthTrackerStatus status2 =
        healthTrackerStatusService.setCategory(enr.getId(), cat2, List.of(ciid));
    Assert.assertEquals(cat2, status2.getCategory());

    // Try one more change
    var cat3 = HealthTrackerStatusCategory.ACTION_NEEDED;
    status2 = healthTrackerStatusService.setCategory(enr.getId(), cat3, List.of(ciid));
    Assert.assertEquals(cat3, status2.getCategory());

    // Make sure the logs are written
    Assert.assertTrue("message", asyncExecutor instanceof ThreadPoolTaskExecutor);
    ((ThreadPoolTaskExecutor) asyncExecutor)
        .getThreadPoolExecutor()
        .awaitTermination(2, TimeUnit.SECONDS);
    // Find object in the DB, make sure the right number of logs is there
    Optional<Enrollment> enr2 = enrollmentRepository.findById(enr.getId());
    Assert.assertTrue(enr2.isPresent());
    // Status was changed twice. There should be 2 log entries.
    Assert.assertNotNull(enr2.get().getStatusLogs());
    Assert.assertEquals(2, enr2.get().getStatusLogs().size());
    // Verify events written
    var events = eventsRepository.getEnrollmentEvents(enr.getId());
    Assert.assertTrue("events created", events.size() > 0);
    // verify related checkin in events. Get just one event record for the verification purposes
    // that's enough
    var statusChangeEvent =
        events.stream()
            .filter(ci -> ci.getType() == HealthTrackerEvent.Type.STATUS_CHANGED)
            .findFirst();
    Assert.assertTrue("thereis at least one status change event", statusChangeEvent.isPresent());
    Assert.assertNotNull(statusChangeEvent.get().getRelatedCheckins());
    Assert.assertEquals(1, statusChangeEvent.get().getRelatedCheckins().size());
    Assert.assertEquals(ciid, statusChangeEvent.get().getRelatedCheckins().get(0).getId());
  }

  @Test
  public void testProReviewIsCreated() {
    SurveyItemPayload symptomSurvey =
        HealthTrackerStatusServiceTest.createSurvey(
            CheckInService.SYMPTOM_FEVER_SEVERITY, CheckInService.MILD);

    Enrollment enrollment = createEnrollment(5L, 5L, 5L, symptomSurvey);

    List<SurveyItemPayload> symptomsList = new ArrayList<>();
    symptomsList.add(symptomSurvey);

    SurveyPayload payload = new SurveyPayload();
    payload.content.setSymptoms(symptomsList);

    CheckIn checkIn =
        createCheckIn(enrollment.getId(), LocalDate.now().minusDays(2), symptomsList.get(0));

    HealthTrackerStatusCommand command =
        new HealthTrackerStatusCommand(enrollment.getId(), payload, List.of(checkIn.getId()));
    healthTrackerStatusService.accept(command);

    HealthTrackerStatus status = healthTrackerStatusService.getOrCreateNewStatus(enrollment);
    String reviewId = status.getProReviewId();
    Assert.assertTrue(reviewId != null && !reviewId.isEmpty());

    var proReview = proReviewRepository.findById(reviewId).get();
    Assert.assertEquals(proReview.getCheckInIds(), List.of(checkIn.getId()));
  }

  @Test
  public void sequentialSurveySubmissions_updatesStatusAndProReview() throws InterruptedException {
    SurveyItemPayload symptomSurvey =
        HealthTrackerStatusServiceTest.createSurvey(
            CheckInService.SYMPTOM_FEVER_SEVERITY, CheckInService.MILD);

    Enrollment enrollment = createStatus(5L, 5L, 5L, symptomSurvey);
    SurveyPayload firstPayload = new SurveyPayload();
    firstPayload.content.setSymptoms(List.of(symptomSurvey));
    CheckIn firstCheckIn =
        createCheckIn(enrollment.getId(), LocalDate.now().minusDays(2), symptomSurvey);

    SurveyItemPayload oralSurvey =
        HealthTrackerStatusServiceTest.createSurvey(
            CheckInService.MEDICATION_TAKEN_QUESTION_ID, "yes");

    SurveyPayload secondPayload = new SurveyPayload();
    secondPayload.content.setOral(List.of(oralSurvey));
    CheckIn secondCheckIn =
        createCheckIn(enrollment.getId(), LocalDate.now().minusDays(2), oralSurvey);

    // submit both at the same time to check for race conditions
    Callable<Integer> c1 =
        () -> {
          healthTrackerStatusService.processStatus(
              enrollment.getId(), firstPayload, List.of(firstCheckIn.getId()));
          return 1;
        };

    Callable<Integer> c2 =
        () -> {
          healthTrackerStatusService.processStatus(
              enrollment.getId(), secondPayload, List.of(secondCheckIn.getId()));
          return 1;
        };

    ExecutorService es = Executors.newFixedThreadPool(2);
    List<Future<Integer>> futures = es.invokeAll(List.of(c1, c2));
    for (Future<Integer> f : futures) {
      try {
        System.out.println(f.get());
      } catch (ExecutionException e) {
        System.out.println("error");
      }
    }

    HealthTrackerStatus status = healthTrackerStatusRepository.findById(enrollment.getId()).get();
    ProReview proReview = proReviewRepository.findById(status.getProReviewId()).get();

    Assert.assertEquals(2, proReview.getCheckInIds().size());
    Assert.assertEquals(1, proReview.getSurveyPayload().getOral().size());
    Assert.assertEquals(1, proReview.getSurveyPayload().getSymptoms().size());
    Assert.assertEquals(1, status.getSurveyPayload().getOral().size());
    Assert.assertEquals(1, status.getSurveyPayload().getSymptoms().size());
  }
}
