package com.navigatingcancer.healthtracker.api.events;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.navigatingcancer.healthtracker.api.TestConfig;
import com.navigatingcancer.healthtracker.api.data.auth.Identity;
import com.navigatingcancer.healthtracker.api.data.client.PatientInfoServiceClient;
import com.navigatingcancer.healthtracker.api.data.model.CheckIn;
import com.navigatingcancer.healthtracker.api.data.model.CheckInFrequency;
import com.navigatingcancer.healthtracker.api.data.model.CheckInSchedule;
import com.navigatingcancer.healthtracker.api.data.model.CheckInType;
import com.navigatingcancer.healthtracker.api.data.model.Enrollment;
import com.navigatingcancer.healthtracker.api.data.model.HealthTrackerEvent;
import com.navigatingcancer.healthtracker.api.data.model.HealthTrackerStatus;
import com.navigatingcancer.healthtracker.api.data.model.HealthTrackerStatusCategory;
import com.navigatingcancer.healthtracker.api.data.model.patientInfo.PatientInfo;
import com.navigatingcancer.healthtracker.api.data.model.schedule.TriggerPayload;
import com.navigatingcancer.healthtracker.api.data.model.survey.SurveyItemPayload;
import com.navigatingcancer.healthtracker.api.data.model.survey.SurveyPayload;
import com.navigatingcancer.healthtracker.api.data.repo.CheckInRepository;
import com.navigatingcancer.healthtracker.api.data.repo.EnrollmentRepository;
import com.navigatingcancer.healthtracker.api.data.repo.EnrollmentRepositoryTest;
import com.navigatingcancer.healthtracker.api.data.repo.HealthTrackerEventsRepository;
import com.navigatingcancer.healthtracker.api.data.repo.HealthTrackerStatusRepository;
import com.navigatingcancer.healthtracker.api.data.service.CheckInService;
import com.navigatingcancer.healthtracker.api.data.service.SchedulingServiceITest;
import com.navigatingcancer.healthtracker.api.data.service.impl.SchedulingServiceImpl;
import com.navigatingcancer.healthtracker.api.processor.HealthTrackerStatusService;
import com.navigatingcancer.healthtracker.api.processor.HealthTrackerStatusServiceTest;
import com.navigatingcancer.healthtracker.api.processor.model.HealthTrackerStatusCommand;
import com.navigatingcancer.scheduler.client.domain.TriggerEvent;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
@Import(TestConfig.class)
public class ApplicationEventsTest {

  @Autowired HealthTrackerEventsPublisher publisher;

  @Autowired Executor asyncExecutor;

  @Autowired HealthTrackerEventsRepository eventsRepository;

  @Autowired private EnrollmentRepository enrollmentRepository;

  @Autowired private CheckInRepository checkInRepository;

  @Autowired private ObjectMapper objectMapper;

  @Autowired private SchedulingServiceImpl schedulingService;

  @Autowired private HealthTrackerStatusRepository healthTrackerStatusRepository;

  @Autowired HealthTrackerStatusService healthTrackerStatusService;

  @MockBean PatientInfoServiceClient patientInfoClient;

  @Before
  public void setup() throws Exception {
    PatientInfoServiceClient.FeignClient mockPatientInfo =
        Mockito.mock(PatientInfoServiceClient.FeignClient.class);
    given(patientInfoClient.getApi()).willReturn(mockPatientInfo);
    PatientInfo patientInfo = new PatientInfo();
    patientInfo.setHighRisk(false);
    given(mockPatientInfo.getPatients(any(), any())).willReturn(Arrays.asList(patientInfo));
  }

  private void flushWorkerQueue() throws Exception {
    Assert.assertTrue("message", asyncExecutor instanceof ThreadPoolTaskExecutor);
    ((ThreadPoolTaskExecutor) asyncExecutor)
        .getThreadPoolExecutor()
        .awaitTermination(2, TimeUnit.SECONDS);
  }

  private static final Long getRandomLong(int max) {
    return (long) (Math.random() * max);
  }

  @Test
  public void whenEventsSend_eventObjectCreatedInDb() throws Exception {
    HealthTrackerStatusCategory from = HealthTrackerStatusCategory.ACTION_NEEDED;
    HealthTrackerStatusCategory to = HealthTrackerStatusCategory.WATCH_CAREFULLY;
    final String eid = "eid123";
    final Long clinicId = 1l;
    final Long patientId = getRandomLong(100000);
    Identity i = new Identity();
    i.setClinicianId("123");
    i.setClinicianName("C C");
    final String reason = "test";
    publisher.publishStatusChange(eid, clinicId, patientId, reason, from, to, List.of("ciid"), i);

    // Make sure the logs are written
    flushWorkerQueue();

    List<HealthTrackerEvent> eventsFound = eventsRepository.getEnrollmentEvents(eid);
    Assert.assertNotNull(eventsFound);
    Assert.assertEquals(1, eventsFound.size());
    HealthTrackerEvent event = eventsFound.get(0);
    Assert.assertEquals(eid, event.getEnrollmentId());
    Assert.assertEquals(patientId, event.getPatientId());
    Assert.assertEquals(clinicId, event.getClinicId());
    Assert.assertEquals(i.getClinicianName(), event.getBy());
    Assert.assertNotNull(event.getEvent());
    Assert.assertEquals(HealthTrackerEvent.Type.STATUS_CHANGED, event.getType());
    Assert.assertEquals(HealthTrackerEvent.Type.STATUS_CHANGED.getMessage(), event.getEvent());
    Assert.assertTrue(event.getNote().contains(from.name()));
    Assert.assertTrue(event.getNote().contains(to.name()));
  }

  @Test
  public void whenNotCheckedIn_missedCheckinEventObjectCreatedInDb() throws Exception {
    var testDay = LocalDate.now();

    CheckInSchedule schedule = new CheckInSchedule();
    schedule.setEndDate(testDay.plusDays(3));
    schedule.setStartDate(testDay.minusDays(1));
    schedule.setCheckInType(CheckInType.ORAL);
    schedule.setCheckInFrequency(CheckInFrequency.DAILY);

    Enrollment en =
        enrollmentRepository.save(
            EnrollmentRepositoryTest.createEnrollmentWithSchedules(7, 7, 7, schedule));

    CheckIn ci = SchedulingServiceITest.createCheckIn(en.getId(), testDay.minusDays(1), null);
    checkInRepository.save(ci);

    TriggerPayload triggerPayload =
        new TriggerPayload(
            en.getId(),
            CheckInType.ORAL,
            LocalTime.of(23, 59, 59),
            TriggerPayload.TriggerType.STATUS);
    String payload = this.objectMapper.writeValueAsString(triggerPayload);
    Date statusEventTime =
        Date.from(
            testDay
                .plusDays(1)
                .atStartOfDay()
                .minusMinutes(1)
                .atZone(ZoneId.systemDefault())
                .toInstant());
    TriggerEvent te = new TriggerEvent(payload, statusEventTime, null);

    schedulingService.accept(te);

    // inspect status
    HealthTrackerStatus status = healthTrackerStatusRepository.getById(en.getId());

    Assert.assertNotNull(status);
    Assert.assertEquals(
        "missed checkins should be set to 1", Integer.valueOf(1), status.getMissedCheckIns());
    Assert.assertNull(status.getCategory());

    // Make sure the logs are written
    flushWorkerQueue();

    // Verify missed checkin event created
    List<HealthTrackerEvent> eventsFound = eventsRepository.getEnrollmentEvents(en.getId());
    Assert.assertNotNull(eventsFound);
    Assert.assertTrue("there are events created", eventsFound.size() > 0);
    List<HealthTrackerEvent> missedCheckins =
        eventsFound.stream()
            .filter(c -> c.getType() == HealthTrackerEvent.Type.CHECK_IN_MISSED)
            .collect(Collectors.toList());
    Assert.assertNotNull(missedCheckins);
    Assert.assertEquals(1, missedCheckins.size());
    Assert.assertEquals(Long.valueOf(1l), missedCheckins.get(0).getMissedCheckinsCount());
  }

  private Enrollment createEnrollment(
      Long clinicId, Long locationId, Long patientId, SurveyItemPayload... items) {
    return HealthTrackerStatusServiceTest.createEnrollment(
        enrollmentRepository,
        checkInRepository,
        clinicId,
        locationId,
        patientId,
        LocalDate.now(),
        items);
  }

  @Test
  public void testPROSentWithSymptomsCreatesRulesEvent() throws Exception {
    // If no data, no rabbit message is going out
    Long clinic1 = 1L;
    Long location1 = 2L;
    Long patient1 = getRandomLong(100000);

    // if there are symptoms that must be reported
    Enrollment enrollment = createEnrollment(clinic1, location1, patient1);
    CheckIn ci =
        SchedulingServiceITest.createCheckIn(
            enrollment.getId(), LocalDate.now().minusDays(1), null);
    ci.setCheckInType(CheckInType.SYMPTOM);
    ci = checkInRepository.insert(ci);
    SurveyItemPayload sip =
        HealthTrackerStatusServiceTest.createSurvey(
            CheckInService.SYMPTOM_CHECKIN_QUESTION_ID, CheckInService.MEDICATION_TAKEN_ANSWER_ID,
            CheckInService.SYMPTOM_SWELLING_SEVERITY, CheckInService.SEVERE);
    sip.setId(ci.getId());
    SurveyPayload sp = new SurveyPayload();
    sp.content.setSymptoms(Arrays.asList(sip));
    HealthTrackerStatusCommand command =
        new HealthTrackerStatusCommand(enrollment.getId(), sp, List.of(ci.getId()));
    healthTrackerStatusService.accept(command);

    // Make sure the logs are written
    flushWorkerQueue();

    // Verify rule status change event created
    List<HealthTrackerEvent> eventsFound = eventsRepository.getEnrollmentEvents(enrollment.getId());
    Assert.assertNotNull(eventsFound);
    Assert.assertEquals(1, eventsFound.size());
    HealthTrackerEvent event = eventsFound.get(0);
    Assert.assertEquals(HealthTrackerEvent.Type.STATUS_SET, event.getType());
    Assert.assertNotNull(event.getSideEffects());
    Assert.assertEquals(1, event.getSideEffects().size());
    var se = event.getSideEffects().get(0);
    Assert.assertEquals("swelling", se.getSymptomType());
    Assert.assertEquals("Severe", se.getSeverity());
  }

  @Test
  public void whenLastDayAndNotCheckedIn_missedCheckinEventObjectCreatedInDb() throws Exception {

    var testDay = LocalDate.now();

    // Enrollment that ends today
    CheckInSchedule schedule = new CheckInSchedule();
    schedule.setEndDate(testDay);
    schedule.setStartDate(testDay.minusDays(1));
    schedule.setCheckInType(CheckInType.ORAL);
    schedule.setCheckInFrequency(CheckInFrequency.DAILY);

    Enrollment en =
        enrollmentRepository.save(
            EnrollmentRepositoryTest.createEnrollmentWithSchedules(7, 7, 7, schedule));

    // Have one check-in
    CheckIn ci = SchedulingServiceITest.createCheckIn(en.getId(), testDay, null);
    checkInRepository.save(ci);

    // Prepare status check message
    TriggerPayload triggerPayload =
        new TriggerPayload(
            en.getId(),
            CheckInType.ORAL,
            LocalTime.of(23, 59, 59),
            TriggerPayload.TriggerType.STATUS);
    String payload = this.objectMapper.writeValueAsString(triggerPayload);
    Date statusEventTime =
        Date.from(
            testDay
                .plusDays(1)
                .atStartOfDay()
                .minusMinutes(1)
                .atZone(ZoneId.systemDefault())
                .toInstant());
    TriggerEvent te = new TriggerEvent(payload, statusEventTime, null);
    // Process status check message
    schedulingService.accept(te);

    // Make sure the logs are written
    flushWorkerQueue();

    // Verify missed checkin event created
    List<HealthTrackerEvent> eventsFound = eventsRepository.getEnrollmentEvents(en.getId());
    Assert.assertNotNull(eventsFound);
    Assert.assertTrue("there are events created", eventsFound.size() > 0);
    List<HealthTrackerEvent> missedCheckins =
        eventsFound.stream()
            .filter(c -> c.getType() == HealthTrackerEvent.Type.CHECK_IN_MISSED)
            .collect(Collectors.toList());
    Assert.assertNotNull(missedCheckins);
    Assert.assertEquals(1, missedCheckins.size());
    Assert.assertEquals(Long.valueOf(1l), missedCheckins.get(0).getMissedCheckinsCount());
  }
}
