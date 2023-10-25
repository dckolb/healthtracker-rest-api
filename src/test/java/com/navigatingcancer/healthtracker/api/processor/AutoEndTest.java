package com.navigatingcancer.healthtracker.api.processor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.*;

import com.navigatingcancer.healthtracker.api.TestConfig;
import com.navigatingcancer.healthtracker.api.data.client.PatientInfoServiceClient;
import com.navigatingcancer.healthtracker.api.data.model.*;
import com.navigatingcancer.healthtracker.api.data.model.patientInfo.PatientInfo;
import com.navigatingcancer.healthtracker.api.data.model.schedule.TriggerPayload;
import com.navigatingcancer.healthtracker.api.data.model.schedule.TriggerPayload.TriggerType;
import com.navigatingcancer.healthtracker.api.data.model.survey.SurveyItemPayload;
import com.navigatingcancer.healthtracker.api.data.model.survey.SurveyPayload;
import com.navigatingcancer.healthtracker.api.data.repo.CheckInRepository;
import com.navigatingcancer.healthtracker.api.data.repo.EnrollmentRepository;
import com.navigatingcancer.healthtracker.api.data.repo.HealthTrackerEventsRepository;
import com.navigatingcancer.healthtracker.api.data.repo.HealthTrackerStatusRepository;
import com.navigatingcancer.healthtracker.api.data.service.CheckInService;
import com.navigatingcancer.healthtracker.api.data.service.impl.NotificationService;
import com.navigatingcancer.healthtracker.api.data.service.impl.SchedulingServiceImpl;
import com.navigatingcancer.healthtracker.api.processor.model.HealthTrackerStatusCommand;
import com.navigatingcancer.json.utils.JsonUtils;
import com.navigatingcancer.scheduler.client.domain.TriggerEvent;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
@Slf4j
@Import(TestConfig.class)
public class AutoEndTest {
  @Autowired EnrollmentRepository enrollmentRepository;

  @Autowired CheckInRepository checkinRepository;

  @Autowired CheckInService checkInService;

  @Autowired HealthTrackerStatusRepository statusRepository;

  @Autowired HealthTrackerStatusService healthTrackerStatusService;

  @MockBean private PatientInfoServiceClient patientInfoClient;

  @MockBean private NotificationService notificationService;

  @Autowired private SchedulingServiceImpl schedulingService;

  @MockBean private RabbitTemplate rabbitTemplate;

  @Autowired Executor asyncExecutor;

  @Autowired HealthTrackerEventsRepository eventsRepository;

  @Value(value = "${MN.clinicIds:291}")
  private Long[] MN_CLINIC_IDs;

  private SurveyItemPayload happySurveyItemPayload =
      createSurveyItemPayload(
          "painSeverity", "0",
          "painFrequency", "0",
          "painInterference", "0",
          "nauseaFrequency", "0",
          "nauseaSeverity", "1",
          "constipationSeverity", "0");

  private SurveyItemPayload unhappySurveyItemPayload =
      createSurveyItemPayload(
          "painSeverity", "2",
          "painFrequency", "0");

  private static final String enrollmentTZ = ZoneId.systemDefault().toString();

  @Before
  public void setup() throws NoSuchFieldException, SecurityException {

    PatientInfo patientInfo = new PatientInfo();
    patientInfo.setHighRisk(false);

    PatientInfoServiceClient.FeignClient client =
        Mockito.mock(PatientInfoServiceClient.FeignClient.class);
    Mockito.when(patientInfoClient.getApi()).thenReturn(client);
    Mockito.when(client.getPatients(Mockito.any(), Mockito.any()))
        .thenReturn(Arrays.asList(patientInfo));
  }

  private Enrollment createEnrollment(
      Long clinicId, Long locationId, Long patientId, SurveyItemPayload... items) {
    Enrollment e =
        HealthTrackerStatusServiceTest.createEnrollment(
            enrollmentRepository,
            checkinRepository,
            clinicId,
            locationId,
            patientId,
            LocalDate.now(),
            items);
    Set<TherapyType> types = new HashSet<>();
    types.add(TherapyType.IV);
    e.setTherapyTypes(types);
    e.setReminderTimeZone("America/Los_Angeles");
    enrollmentRepository.save(e);
    return e;
  }

  private Enrollment createEnrollment(
      Long clinicId,
      Long locationId,
      Long patientId,
      LocalDate from,
      LocalDate to,
      Integer daysInCycle,
      Integer cycles,
      LocalDate fromDate) {
    Enrollment enrollment =
        HealthTrackerStatusServiceTest.createEnrollment(
            enrollmentRepository, checkinRepository, clinicId, locationId, patientId, from, to);
    Set<TherapyType> types = new HashSet<>();
    types.add(TherapyType.IV);
    enrollment.setTherapyTypes(types);
    enrollment.setCycles(cycles);
    enrollment.setDaysInCycle(daysInCycle);
    enrollment.setTxStartDate(fromDate);
    enrollment.setReminderTimeZone(enrollmentTZ);
    Assert.assertNotNull(enrollment.getSchedules());
    Assert.assertNotEquals(0, enrollment.getSchedules());
    CheckInSchedule checkInSchedule = enrollment.getSchedules().get(0);
    checkInSchedule.setCheckInType(CheckInType.SYMPTOM);
    return enrollmentRepository.save(enrollment);
  }

  private static SurveyItemPayload createSurveyItemPayload(String... items) {
    SurveyItemPayload item = new SurveyItemPayload();
    item.setPayload(new HashMap<>());
    for (int i = 0; i < items.length; i = i + 2) {
      item.getPayload().put(items[i], items[i + 1]);
    }
    return item;
  }

  private static SurveyPayload createSurveyPayload(
      CheckIn checkin, List<SurveyItemPayload> symptoms) {
    SurveyPayload surveyPayload = new SurveyPayload();
    if (checkin != null) {
      symptoms.stream().forEach(s -> s.setId(checkin.getId()));
    }
    surveyPayload.content.setSymptoms(symptoms);
    return surveyPayload;
  }

  private void sendScheduleEventInEnrollmentTimeZone(
      Enrollment enrollment, LocalDate scheduledDate, TriggerType tp) {
    Date dt =
        Date.from(
            scheduledDate.atStartOfDay(ZoneId.of(enrollment.getReminderTimeZone())).toInstant());
    Date dt2 =
        Date.from(scheduledDate.plusDays(1l).atStartOfDay(ZoneId.of(enrollmentTZ)).toInstant());
    TriggerPayload triggerPayload =
        new TriggerPayload(enrollment.getId(), CheckInType.SYMPTOM, LocalTime.of(9, 0), tp);
    TriggerEvent triggerEvent = new TriggerEvent(JsonUtils.toJson(triggerPayload), dt, dt2);
    log.debug("sendScheduleEvent is sending date ofr {}", dt);
    schedulingService.accept(triggerEvent);
  }

  private void sendScheduleEvent(Enrollment enrollment, LocalDate scheduledDate, TriggerType tp) {
    Date dt = Date.from(scheduledDate.atStartOfDay(ZoneId.of(enrollmentTZ)).toInstant());
    Date dt2 =
        Date.from(scheduledDate.plusDays(1l).atStartOfDay(ZoneId.of(enrollmentTZ)).toInstant());
    TriggerPayload triggerPayload =
        new TriggerPayload(enrollment.getId(), CheckInType.SYMPTOM, LocalTime.of(9, 0), tp);
    TriggerEvent triggerEvent = new TriggerEvent(JsonUtils.toJson(triggerPayload), dt, dt2);
    log.debug("sendScheduleEvent is sending date ofr {}", dt);
    schedulingService.accept(triggerEvent);
  }

  private void doCheckIn(
      Enrollment enrollment, LocalDate scheduledDate, SurveyPayload surveyPayload) {
    // Make a checkin in call, that should set the checkin object state to complete
    List<CheckIn> checkIns = checkInService.checkIn(surveyPayload);
    // make process status call explicitly
    healthTrackerStatusService.processStatus(
        enrollment.getId(),
        surveyPayload,
        checkIns.stream().map(AbstractDocument::getId).collect(Collectors.toList()));
  }

  private void flushWorkerQueue() throws Exception {
    Assert.assertTrue("message", asyncExecutor instanceof ThreadPoolTaskExecutor);
    ((ThreadPoolTaskExecutor) asyncExecutor)
        .getThreadPoolExecutor()
        .awaitTermination(2, TimeUnit.SECONDS);
  }

  @Test
  public void proctcaeSeverityLessThan1AndNoOtherSideEffects_AutoEnd() throws Exception {
    Assert.assertTrue(
        "MN clinc IDs must be defined for the test",
        MN_CLINIC_IDs != null && MN_CLINIC_IDs.length > 0);

    // Enrollment with one complete checkin
    Enrollment enrollment = createEnrollment(MN_CLINIC_IDs[0], 1L, 1L, happySurveyItemPayload);

    doNothing().when(this.rabbitTemplate).convertAndSend(isA(String.class), isA(String.class));

    // Last day survey is the same as it was passed to createEnrollment
    SurveyPayload surveyPayload =
        createSurveyPayload(null, Collections.singletonList(happySurveyItemPayload));
    HealthTrackerStatusCommand command =
        new HealthTrackerStatusCommand(enrollment.getId(), surveyPayload, List.of());
    this.healthTrackerStatusService.accept(command);

    // capture what's being sent to rabbit
    ArgumentCaptor<String> argumentCaptorPayload = ArgumentCaptor.forClass(String.class);
    verify(this.rabbitTemplate, times(1))
        .convertAndSend(any(String.class), argumentCaptorPayload.capture());

    // verify status is properly set
    HealthTrackerStatus status = this.statusRepository.getById(enrollment.getId());

    // Assert
    Assert.assertNotNull(status);
    Assert.assertEquals(HealthTrackerStatusCategory.WATCH_CAREFULLY, status.getCategory());
    Assert.assertEquals(true, status.getEndCurrentCycle());

    // Make sure the logs are written
    flushWorkerQueue();
    // Verify cycle auto-ended event was created
    List<HealthTrackerEvent> eventsFound = eventsRepository.getEnrollmentEvents(enrollment.getId());
    Assert.assertNotNull(eventsFound);
    Assert.assertTrue(eventsFound.size() > 0);
    Optional<HealthTrackerEvent> ev =
        eventsFound.stream()
            .filter(e -> e.getType() == HealthTrackerEvent.Type.CYCLE_ENDED)
            .findFirst();
    Assert.assertTrue(ev.isPresent());
    Assert.assertEquals("Check-ins have auto-ended for this cycle", ev.get().getReason());
  }

  @Test
  public void nonIV_DoNotAutoEnd() {
    Assert.assertTrue(
        "MN clinc IDs must be defined for the test",
        MN_CLINIC_IDs != null && MN_CLINIC_IDs.length > 0);

    // Enrollment with one complete checkin
    Enrollment enrollment = createEnrollment(MN_CLINIC_IDs[0], 1L, 1L, happySurveyItemPayload);
    Set<TherapyType> types = new HashSet<>();
    types.add(TherapyType.ORAL);
    enrollment.setTherapyTypes(types);
    enrollmentRepository.save(enrollment);

    doNothing().when(this.rabbitTemplate).convertAndSend(isA(String.class), isA(String.class));

    // Last day survey is the same as it was passed to createEnrollment
    SurveyPayload surveyPayload =
        createSurveyPayload(null, Collections.singletonList(happySurveyItemPayload));
    HealthTrackerStatusCommand command =
        new HealthTrackerStatusCommand(enrollment.getId(), surveyPayload, List.of());
    this.healthTrackerStatusService.accept(command);

    // verify status is properly set
    HealthTrackerStatus status = this.statusRepository.getById(enrollment.getId());

    // Assert
    Assert.assertNotNull(status);
    Assert.assertEquals(HealthTrackerStatusCategory.WATCH_CAREFULLY, status.getCategory());
    Assert.assertEquals(false, status.getEndCurrentCycle());
  }

  @Test
  public void withOnlyMissedCheckinsNoAutoEnd() {
    Assert.assertTrue(
        "MN clinc IDs must be defined for the test",
        MN_CLINIC_IDs != null && MN_CLINIC_IDs.length > 0);

    Integer daysInCycle = 7;
    Integer cycles = 3;
    LocalDate fromDate = LocalDate.of(2019, 02, 03);
    LocalDate toDate = fromDate.plusDays(daysInCycle * cycles);

    // Enrollment without checkins
    Enrollment enrollment =
        createEnrollment(MN_CLINIC_IDs[0], 1L, 1L, fromDate, toDate, daysInCycle, cycles, fromDate);

    // Prepare one checkin, do not report anything
    sendScheduleEvent(enrollment, fromDate.plusDays(1), TriggerType.SYSTEM);
    sendScheduleEvent(enrollment, fromDate.plusDays(1), TriggerType.REMINDER);
    sendScheduleEvent(enrollment, fromDate.plusDays(1), TriggerType.STATUS);

    // verify status is not auto-end
    HealthTrackerStatus status = this.statusRepository.getById(enrollment.getId());
    Assert.assertNotNull(status);
    Assert.assertTrue(!status.getEndCurrentCycle());

    // Same check with one more day, still no checkins, no auto-close
    sendScheduleEvent(enrollment, fromDate.plusDays(2), TriggerType.SYSTEM);
    sendScheduleEvent(enrollment, fromDate.plusDays(2), TriggerType.REMINDER);
    sendScheduleEvent(enrollment, fromDate.plusDays(2), TriggerType.STATUS);
    status = this.statusRepository.getById(enrollment.getId());
    Assert.assertNotNull(status);
    Assert.assertTrue(!status.getEndCurrentCycle());
  }

  @Test
  public void testScheduleAutoStopOnFirstDay() {
    Assert.assertTrue(
        "MN clinc IDs must be defined for the test",
        MN_CLINIC_IDs != null && MN_CLINIC_IDs.length > 0);
    // Create enrollment with a schedule for X days N times
    Integer daysInCycle = 7;
    Integer cycles = 3;
    LocalDate fromDate = LocalDate.now();
    LocalDate toDate = fromDate.plusDays(daysInCycle * cycles);
    LocalDate date2 = fromDate.plusDays(daysInCycle);

    Enrollment enrollment =
        createEnrollment(MN_CLINIC_IDs[0], 1L, 1L, fromDate, toDate, daysInCycle, cycles, fromDate);
    Assert.assertFalse(enrollment.getSchedules() == null || enrollment.getSchedules().isEmpty());

    // Start the schedule
    sendScheduleEvent(enrollment, fromDate, TriggerType.SYSTEM);
    // Make sure we actually created a checkin
    List<CheckIn> checkins =
        checkinRepository.findByStatus(enrollment.getId(), CheckInStatus.PENDING);
    Assert.assertTrue(checkins != null && checkins.size() == 1);
    CheckIn checkin = checkins.get(0);

    // Make sure cycle not stopped
    HealthTrackerStatus status = this.statusRepository.getById(enrollment.getId());
    Assert.assertTrue(status == null || !status.getEndCurrentCycle());

    // Report no or mild symptoms on the first day
    SurveyPayload surveyPayload =
        createSurveyPayload(checkin, Collections.singletonList(happySurveyItemPayload));
    doCheckIn(enrollment, fromDate, surveyPayload);

    // Make sure cycle stopped
    status = this.statusRepository.getById(enrollment.getId());
    Assert.assertNotNull(status);
    Assert.assertEquals(HealthTrackerStatusCategory.WATCH_CAREFULLY, status.getCategory());
    Assert.assertTrue(status.getEndCurrentCycle());

    // Make sure SYMPTOM reminders are not getting sent to the patient
    sendScheduleEvent(enrollment, fromDate.plusDays(1), TriggerType.REMINDER);
    int reminderMessageSentCount = 0;
    verify(notificationService, times(reminderMessageSentCount))
        .sendNotification(any(), any(), any(), any());
    // Make sure status still the same
    status = this.statusRepository.getById(enrollment.getId());
    Assert.assertEquals(HealthTrackerStatusCategory.WATCH_CAREFULLY, status.getCategory());
    Assert.assertTrue(status.getEndCurrentCycle());

    // Make sure cycle is not resumed prematurely with status message
    sendScheduleEvent(enrollment, fromDate, TriggerType.STATUS);
    status = this.statusRepository.getById(enrollment.getId());
    Assert.assertTrue(status.getEndCurrentCycle());

    // Make sure cycle is not resumed prematurely with the next day messages
    sendScheduleEvent(enrollment, fromDate.plusDays(1), TriggerType.SYSTEM);
    status = this.statusRepository.getById(enrollment.getId());
    Assert.assertTrue(status.getEndCurrentCycle());
    sendScheduleEvent(enrollment, fromDate.plusDays(2), TriggerType.REMINDER);
    verify(notificationService, times(reminderMessageSentCount))
        .sendNotification(any(), any(), any(), any());

    // Start the second cycle. see that the cycle resumes
    sendScheduleEvent(enrollment, date2, TriggerType.SYSTEM);
    status = this.statusRepository.getById(enrollment.getId());
    Assert.assertTrue(status == null || !status.getEndCurrentCycle());

    // Make sure on the first day of the 2nd cycle a reminder is sent
    sendScheduleEvent(enrollment, date2, TriggerType.REMINDER);
    verify(notificationService, times(++reminderMessageSentCount))
        .sendNotification(any(), any(), any(), any());

    // Report no symptoms again
    doCheckIn(enrollment, date2, surveyPayload);

    // Make sure cycle stopped
    status = this.statusRepository.getById(enrollment.getId());
    Assert.assertNotNull(status);
    Assert.assertEquals(HealthTrackerStatusCategory.WATCH_CAREFULLY, status.getCategory());
    Assert.assertTrue(status.getEndCurrentCycle());

    // Make sure cycle is not resumed prematurely with status message
    sendScheduleEvent(enrollment, date2, TriggerType.STATUS);
    status = this.statusRepository.getById(enrollment.getId());
    Assert.assertTrue(status.getEndCurrentCycle());

    // Make sure SYMPTOM reminders are not getting sent
    sendScheduleEvent(enrollment, date2.plusDays(1), TriggerType.REMINDER);
    verify(notificationService, times(reminderMessageSentCount))
        .sendNotification(any(), any(), any(), any());

    // Make sure the status is still closed
    status = this.statusRepository.getById(enrollment.getId());
    Assert.assertNotNull(status);
    Assert.assertEquals(HealthTrackerStatusCategory.WATCH_CAREFULLY, status.getCategory());
    Assert.assertTrue(status.getEndCurrentCycle());
  }

  @Test
  public void testScheduleAutoStopIfMissedCheckins() {
    Assert.assertTrue(
        "MN clinc IDs must be defined for the test",
        MN_CLINIC_IDs != null && MN_CLINIC_IDs.length > 0);
    // Create enrollment with a schedule for X days N times
    Integer daysInCycle = 7;
    Integer cycles = 3;
    LocalDate fromDate = LocalDate.now();
    LocalDate toDate = fromDate.plusDays(daysInCycle * cycles);
    LocalDate date2 = fromDate.plusDays(daysInCycle);

    Enrollment enrollment =
        createEnrollment(MN_CLINIC_IDs[0], 1L, 1L, fromDate, toDate, daysInCycle, cycles, fromDate);
    Assert.assertFalse(enrollment.getSchedules() == null || enrollment.getSchedules().isEmpty());

    // Start the schedule
    sendScheduleEvent(enrollment, fromDate, TriggerType.SYSTEM);
    // Make sure cycle not stopped
    HealthTrackerStatus status = this.statusRepository.getById(enrollment.getId());
    Assert.assertTrue(status == null || !status.getEndCurrentCycle());

    // Skip a few days
    sendScheduleEvent(enrollment, fromDate.plusDays(1), TriggerType.SYSTEM);
    sendScheduleEvent(enrollment, fromDate.plusDays(1), TriggerType.REMINDER);
    sendScheduleEvent(enrollment, fromDate.plusDays(1), TriggerType.STATUS);
    sendScheduleEvent(enrollment, fromDate.plusDays(2), TriggerType.SYSTEM);
    sendScheduleEvent(enrollment, fromDate.plusDays(2), TriggerType.REMINDER);
    sendScheduleEvent(enrollment, fromDate.plusDays(2), TriggerType.STATUS);
    sendScheduleEvent(enrollment, fromDate.plusDays(3), TriggerType.SYSTEM);
    sendScheduleEvent(enrollment, fromDate.plusDays(3), TriggerType.REMINDER);
    sendScheduleEvent(enrollment, fromDate.plusDays(3), TriggerType.STATUS);

    // Make sure missed checkins are reported
    List<CheckIn> missedCheckins =
        checkinRepository.findByStatus(enrollment.getId(), CheckInStatus.MISSED);
    Assert.assertTrue(missedCheckins != null && missedCheckins.size() == 4);

    // Make sure cycle not stopped
    status = this.statusRepository.getById(enrollment.getId());
    Assert.assertNotNull(status);
    Assert.assertTrue(!status.getEndCurrentCycle());

    // Report no symptoms
    sendScheduleEvent(enrollment, fromDate.plusDays(4), TriggerType.SYSTEM);
    sendScheduleEvent(enrollment, fromDate.plusDays(4), TriggerType.REMINDER);
    List<CheckIn> checkins =
        checkinRepository.findByStatus(enrollment.getId(), CheckInStatus.PENDING);
    Assert.assertTrue(checkins != null && checkins.size() == 1);
    CheckIn checkin = checkins.get(0);
    SurveyPayload surveyPayload =
        createSurveyPayload(checkin, Collections.singletonList(happySurveyItemPayload));
    doCheckIn(enrollment, fromDate, surveyPayload);

    // Make sure cycle stopped
    status = this.statusRepository.getById(enrollment.getId());
    Assert.assertNotNull(status);
    Assert.assertEquals(HealthTrackerStatusCategory.WATCH_CAREFULLY, status.getCategory());
    Assert.assertTrue(status.getEndCurrentCycle());
  }

  @Test
  public void testScheduleDoesNotAutoStopIfSymptomsWereReported() {
    Assert.assertTrue(
        "MN clinc IDs must be defined for the test",
        MN_CLINIC_IDs != null && MN_CLINIC_IDs.length > 0);
    // Create enrollment with a schedule for X days N times
    Integer daysInCycle = 7;
    Integer cycles = 3;
    LocalDate fromDate = LocalDate.now();
    LocalDate toDate = fromDate.plusDays(daysInCycle * cycles);
    LocalDate date2 = fromDate.plusDays(daysInCycle);

    Enrollment enrollment =
        createEnrollment(MN_CLINIC_IDs[0], 1L, 1L, fromDate, toDate, daysInCycle, cycles, fromDate);
    Assert.assertFalse(enrollment.getSchedules() == null || enrollment.getSchedules().isEmpty());

    // Start the schedule
    sendScheduleEvent(enrollment, fromDate, TriggerType.SYSTEM);
    // Make sure cycle not stopped
    HealthTrackerStatus status = this.statusRepository.getById(enrollment.getId());
    Assert.assertTrue(status == null || !status.getEndCurrentCycle());

    // Report symptoms
    List<CheckIn> checkins =
        checkinRepository.findByStatus(enrollment.getId(), CheckInStatus.PENDING);
    Assert.assertTrue(checkins != null && checkins.size() == 1);
    SurveyPayload surveyPayload =
        createSurveyPayload(checkins.get(0), Collections.singletonList(unhappySurveyItemPayload));
    doCheckIn(enrollment, fromDate, surveyPayload);

    // Make sure did not stop
    status = this.statusRepository.getById(enrollment.getId());
    Assert.assertNotNull(status);
    Assert.assertFalse(status.getEndCurrentCycle());

    // Report no symptoms
    sendScheduleEvent(enrollment, fromDate.plusDays(1), TriggerType.SYSTEM);
    sendScheduleEvent(enrollment, fromDate.plusDays(1), TriggerType.REMINDER);
    checkins = checkinRepository.findByStatus(enrollment.getId(), CheckInStatus.PENDING);
    Assert.assertTrue(checkins != null && checkins.size() == 1);
    CheckIn checkin = checkins.get(0);
    surveyPayload = createSurveyPayload(checkin, Collections.singletonList(happySurveyItemPayload));
    doCheckIn(enrollment, fromDate, surveyPayload);

    // Make sure cycle not stopped
    status = this.statusRepository.getById(enrollment.getId());
    Assert.assertNotNull(status);
    Assert.assertFalse(status.getEndCurrentCycle());
  }

  @Test
  public void testIfScheduleAutoStopNoMoreCheckins() {
    Assert.assertTrue(
        "MN clinc IDs must be defined for the test",
        MN_CLINIC_IDs != null && MN_CLINIC_IDs.length > 0);
    // Create enrollment with a schedule for X days N times
    Integer daysInCycle = 7;
    Integer cycles = 3;
    LocalDate now = LocalDate.now();
    LocalDate fromDate = now.plusDays(0);
    LocalDate checkinDate = fromDate.plusDays(1);
    LocalDate toDate = fromDate.plusDays(daysInCycle * cycles);

    Enrollment enrollment =
        createEnrollment(MN_CLINIC_IDs[0], 1L, 1L, fromDate, toDate, daysInCycle, cycles, fromDate);
    Assert.assertFalse(enrollment.getSchedules() == null || enrollment.getSchedules().isEmpty());

    // Start the schedule
    sendScheduleEvent(enrollment, checkinDate, TriggerType.SYSTEM);
    // Make sure cycle not stopped
    HealthTrackerStatus status = this.statusRepository.getById(enrollment.getId());
    Assert.assertNotNull(status);
    Assert.assertTrue(!status.getEndCurrentCycle());

    // Report no symptoms
    sendScheduleEvent(enrollment, checkinDate, TriggerType.REMINDER);
    List<CheckIn> checkins = checkinRepository.findByEnrollmentId(enrollment.getId());
    Assert.assertTrue(checkins != null && checkins.size() == 1);
    CheckIn checkin = checkins.get(0);
    Assert.assertEquals(CheckInStatus.PENDING, checkin.getStatus());
    SurveyPayload surveyPayload =
        createSurveyPayload(checkin, Collections.singletonList(happySurveyItemPayload));
    doCheckIn(enrollment, checkinDate, surveyPayload);

    // Make sure cycle stopped
    status = this.statusRepository.getById(enrollment.getId());
    Assert.assertNotNull(status);
    Assert.assertEquals(HealthTrackerStatusCategory.WATCH_CAREFULLY, status.getCategory());
    Assert.assertTrue(status.getEndCurrentCycle());

    // Try to create new checkin
    sendScheduleEvent(enrollment, checkinDate.plusDays(0), TriggerType.SYSTEM);
    sendScheduleEvent(enrollment, checkinDate.plusDays(1), TriggerType.SYSTEM);

    // Make sure no new checkins were created
    List<CheckIn> moreCheckins = checkinRepository.findByEnrollmentId(enrollment.getId());
    Assert.assertTrue(
        "No more new checkins created if auto-stopped", checkins.size() == moreCheckins.size());
  }

  // setup a MN enrollment for cycle two on day five (first checkin in second)
  // after first cycle auto ended
  @Test
  public void givenScheduleThatStartsOnDay5_whenAutoEnded_shouldStillStartUpNextCycle() {
    Assert.assertTrue(
        "MN clinc IDs must be defined for the test",
        MN_CLINIC_IDs != null && MN_CLINIC_IDs.length > 0);
    Integer daysInCycle = 28;
    Integer cycles = 3;
    LocalDate today = LocalDate.now(ZoneId.of("America/Chicago"));
    LocalDate fromDate = today.minusDays(33);
    LocalDate endDate = fromDate.plusDays(daysInCycle * cycles);
    LocalDate autoEndDate = fromDate.plusDays(5);

    // create a MN type schedule, cycle days 5-15
    CheckInSchedule schedule =
        HealthTrackerStatusServiceTest.createCheckInSchedule(
            CheckInType.SYMPTOM,
            fromDate,
            endDate,
            CheckInFrequency.CUSTOM,
            Arrays.asList(5, 6, 7, 8, 9, 10, 11, 12, 13, 15),
            null);

    Enrollment enrollment =
        HealthTrackerStatusServiceTest.createEnrollment(
            enrollmentRepository,
            MN_CLINIC_IDs[0],
            1l,
            1l,
            null,
            EnrollmentStatus.ACTIVE,
            "America/Chicago",
            fromDate,
            daysInCycle,
            cycles,
            Arrays.asList(schedule));
    SurveyItemPayload payload = createSurveyItemPayload();
    CheckIn checkIn =
        HealthTrackerStatusServiceTest.createCheckIn(
            checkinRepository,
            CheckInType.SYMPTOM,
            CheckInStatus.COMPLETED,
            autoEndDate,
            enrollment.getId(),
            payload);
    // create status of autoended
    Assert.assertFalse(enrollment.getSchedules() == null || enrollment.getSchedules().isEmpty());
    // get status and set autoend
    HealthTrackerStatus status = this.healthTrackerStatusService.getOrCreateNewStatus(enrollment);
    status = this.statusRepository.setEndCurrentCycle(enrollment.getId(), true);

    Assert.assertTrue("status should be auto end", status.getEndCurrentCycle());

    List<CheckIn> initialCheckIns = checkinRepository.findByEnrollmentId(enrollment.getId());
    Assert.assertEquals("should have one checkin", 1, initialCheckIns.size());
    // Start the schedule
    sendScheduleEventInEnrollmentTimeZone(enrollment, today, TriggerType.SYSTEM);

    List<CheckIn> checkIns = checkinRepository.findByEnrollmentId(enrollment.getId());
    Assert.assertEquals("should have two checkin", 2, checkIns.size());

    status = this.statusRepository.getById(enrollment.getId());
    Assert.assertFalse("status should not be auto ended", status.getEndCurrentCycle());
  }

  @Test
  public void givenScheduleThatStartsOnDay5_whenAutoEnded_shouldSkipWhenInSameCycle() {
    Assert.assertTrue(
        "MN clinc IDs must be defined for the test",
        MN_CLINIC_IDs != null && MN_CLINIC_IDs.length > 0);
    Integer daysInCycle = 28;
    Integer cycles = 3;
    LocalDate today = LocalDate.now(ZoneId.of("America/Chicago"));
    LocalDate fromDate = today.minusDays(12);
    LocalDate endDate = fromDate.plusDays(daysInCycle * cycles);
    LocalDate autoEndDate = fromDate.plusDays(5);

    // create a MN type schedule, cycle days 5-15
    CheckInSchedule schedule =
        HealthTrackerStatusServiceTest.createCheckInSchedule(
            CheckInType.SYMPTOM,
            fromDate,
            endDate,
            CheckInFrequency.CUSTOM,
            Arrays.asList(5, 6, 7, 8, 9, 10, 11, 12, 13, 15),
            null);

    Enrollment enrollment =
        HealthTrackerStatusServiceTest.createEnrollment(
            enrollmentRepository,
            MN_CLINIC_IDs[0],
            1l,
            1l,
            null,
            EnrollmentStatus.ACTIVE,
            "America/Chicago",
            fromDate,
            daysInCycle,
            cycles,
            Arrays.asList(schedule));
    SurveyItemPayload payload = createSurveyItemPayload();
    CheckIn checkIn =
        HealthTrackerStatusServiceTest.createCheckIn(
            checkinRepository,
            CheckInType.SYMPTOM,
            CheckInStatus.COMPLETED,
            autoEndDate,
            enrollment.getId(),
            payload);
    // create status of autoended
    Assert.assertFalse(enrollment.getSchedules() == null || enrollment.getSchedules().isEmpty());
    // get status and set autoend
    HealthTrackerStatus status = this.healthTrackerStatusService.getOrCreateNewStatus(enrollment);
    status = this.statusRepository.setEndCurrentCycle(enrollment.getId(), true);

    Assert.assertTrue("status should be auto end", status.getEndCurrentCycle());

    List<CheckIn> initialCheckIns = checkinRepository.findByEnrollmentId(enrollment.getId());
    Assert.assertEquals("should have one checkin", 1, initialCheckIns.size());
    // Start the schedule
    sendScheduleEventInEnrollmentTimeZone(enrollment, today, TriggerType.SYSTEM);

    List<CheckIn> checkIns = checkinRepository.findByEnrollmentId(enrollment.getId());
    Assert.assertEquals("should still have one checkin", 1, checkIns.size());

    status = this.statusRepository.getById(enrollment.getId());
    Assert.assertTrue("status should  be auto ended", status.getEndCurrentCycle());
  }
}
