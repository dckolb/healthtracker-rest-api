package com.navigatingcancer.healthtracker.api.processor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.navigatingcancer.healthtracker.api.TestConfig;
import com.navigatingcancer.healthtracker.api.data.client.PatientInfoServiceClient;
import com.navigatingcancer.healthtracker.api.data.model.*;
import com.navigatingcancer.healthtracker.api.data.model.patientInfo.PatientInfo;
import com.navigatingcancer.healthtracker.api.data.model.survey.SurveyItemPayload;
import com.navigatingcancer.healthtracker.api.data.model.survey.SurveyPayload;
import com.navigatingcancer.healthtracker.api.data.repo.CheckInRepository;
import com.navigatingcancer.healthtracker.api.data.repo.EnrollmentRepository;
import com.navigatingcancer.healthtracker.api.data.repo.HealthTrackerStatusRepository;
import com.navigatingcancer.healthtracker.api.data.service.CheckInService;
import com.navigatingcancer.healthtracker.api.data.service.impl.PatientRecordService;
import com.navigatingcancer.healthtracker.api.data.service.impl.SchedulingServiceImpl;
import com.navigatingcancer.healthtracker.api.processor.model.HealthTrackerStatusCommand;
import com.navigatingcancer.healthtracker.api.processor.model.TriagePayload;
import com.navigatingcancer.json.utils.JsonUtils;
import com.navigatingcancer.notification.client.service.NotificationServiceClient;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
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
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
@Slf4j
@Import(TestConfig.class)
public class MultiDayStatusTest {

  @Autowired EnrollmentRepository enrollmentRepository;

  @Autowired CheckInRepository checkinRepository;

  @Autowired HealthTrackerStatusService healthTrackerStatusService;

  @Autowired private HealthTrackerStatusRepository healthTrackerStatusRepository;

  @MockBean private PatientInfoServiceClient patientInfoClient;

  @MockBean private NotificationServiceClient notificationServiceClient;

  @MockBean private SchedulingServiceImpl schedulingService;

  @MockBean private RabbitTemplate rabbitTemplate;

  @MockBean private PatientRecordService patientRecordService;

  @Before
  public void setup() {

    PatientInfo patientInfo = new PatientInfo();
    patientInfo.setHighRisk(true);

    PatientInfoServiceClient.FeignClient client =
        Mockito.mock(PatientInfoServiceClient.FeignClient.class);
    Mockito.when(patientInfoClient.getApi()).thenReturn(client);
    Mockito.when(client.getPatients(Mockito.any(), Mockito.any()))
        .thenReturn(Arrays.asList(patientInfo));

    doNothing().when(patientRecordService).publishProData(any(), any(), any());
  }

  private Enrollment createEnrollment(
      Long clinicId, Long locationId, Long patientId, LocalDate startDate) {
    Enrollment enrollment = new Enrollment();
    enrollment.setClinicId(clinicId);
    enrollment.setLocationId(locationId);
    enrollment.setPatientId(patientId);
    enrollment.setMedication("Xtandi");
    enrollment.setTxStartDate(startDate);

    return enrollmentRepository.save(enrollment);
  }

  private CheckIn addCheckInToEnrollment(
      Enrollment enrollment, LocalDate date, SurveyItemPayload item) {
    CheckIn checkIn = new CheckIn();
    checkIn.setCheckInType(CheckInType.ORAL);
    checkIn.setScheduleDate(date);
    checkIn.setSurveyPayload(item);
    checkIn.setStatus(
        item != null && !item.getPayload().isEmpty()
            ? CheckInStatus.COMPLETED
            : CheckInStatus.MISSED);
    checkIn.setEnrollmentId(enrollment.getId());

    return checkinRepository.save(checkIn);
  }

  private CheckIn addMissedCheckInToEnrollment(Enrollment enrollment, LocalDate date) {
    return addCheckInToEnrollment(enrollment, date, null);
  }

  @Test
  public void givenVerySevereYesterday_whenMildToday_shouldGiveWatchCarefully() {
    Long id = 324l;
    Enrollment enrollment = createEnrollment(id, id, id, LocalDate.now().minusDays(3));

    CheckIn checkIn1 =
        addCheckInToEnrollment(
            enrollment,
            LocalDate.now().minusDays(1),
            HealthTrackerStatusServiceTest.createSurvey(
                CheckInService.SYMPTOM_FEVER_SEVERITY, CheckInService.VERY_SEVERE));

    CheckIn checkIn2 =
        addCheckInToEnrollment(
            enrollment,
            LocalDate.now(),
            HealthTrackerStatusServiceTest.createSurvey(
                CheckInService.SYMPTOM_FEVER_SEVERITY, CheckInService.MILD));
    HealthTrackerStatusCommand command =
        new HealthTrackerStatusCommand(
            enrollment.getId(), new SurveyPayload(), List.of(checkIn2.getId()));
    healthTrackerStatusService.accept(command);

    HealthTrackerStatus status = healthTrackerStatusService.getOrCreateNewStatus(enrollment);

    Assert.assertEquals(
        "status should not be triage", "WATCH_CAREFULLY", status.getCategory().name());
  }

  @Test
  public void givenMildYesterday_whenSeverToday_shouldGiveTriage() {
    Long id = 3294l;
    Enrollment enrollment = createEnrollment(id, id, id, LocalDate.now().minusDays(3));

    CheckIn checkIn1 =
        addCheckInToEnrollment(
            enrollment,
            LocalDate.now().minusDays(1),
            HealthTrackerStatusServiceTest.createSurvey(
                CheckInService.SYMPTOM_FEVER_SEVERITY, CheckInService.MILD));

    SurveyItemPayload symptomSurvey =
        HealthTrackerStatusServiceTest.createSurvey(
            CheckInService.SYMPTOM_FEVER_SEVERITY,
            CheckInService.MILD,
            CheckInService.PAIN_FEVER_SEVERITY,
            CheckInService.PAIN_SEVERE,
            CheckInService.OTHER_FEVER_SEVERITY,
            CheckInService.SEVERE);

    CheckIn checkIn2 = addCheckInToEnrollment(enrollment, LocalDate.now(), symptomSurvey);

    List<SurveyItemPayload> symptomsList = new ArrayList<>();
    symptomsList.add(symptomSurvey);

    SurveyPayload payload = new SurveyPayload();
    payload.content.setSymptoms(symptomsList);

    ArgumentCaptor valueCapture = ArgumentCaptor.forClass(Object.class);
    doNothing().when(rabbitTemplate).convertAndSend(any(String.class), valueCapture.capture());

    HealthTrackerStatusCommand command =
        new HealthTrackerStatusCommand(
            enrollment.getId(), payload, List.of(checkIn1.getId(), checkIn2.getId()));
    healthTrackerStatusService.accept(command);

    HealthTrackerStatus status = healthTrackerStatusService.getOrCreateNewStatus(enrollment);

    Assert.assertEquals("status should  be triage", "TRIAGE", status.getCategory().name());
    String obj = (String) valueCapture.getValue();

    verify(rabbitTemplate, times(1)).convertAndSend(any(String.class), any(Object.class));
    TriagePayload processed = JsonUtils.fromJson(obj, TriagePayload.class);
    Assert.assertEquals(3, processed.getReasons().size());
  }

  @Test
  public void givenModerateYesterday_whenSeverToday_shouldGiveTriage() {
    Long id = 3294l;
    Enrollment enrollment = createEnrollment(id, id, id, LocalDate.now().minusDays(1));

    CheckIn checkIn1 =
        addCheckInToEnrollment(
            enrollment,
            LocalDate.now().minusDays(1),
            HealthTrackerStatusServiceTest.createSurvey(
                CheckInService.SYMPTOM_SWELLING_SEVERITY, CheckInService.MODERATE));

    SurveyItemPayload symptomSurvey =
        HealthTrackerStatusServiceTest.createSurvey(
            CheckInService.SYMPTOM_FEVER_SEVERITY,
            CheckInService.MILD,
            CheckInService.PAIN_FEVER_SEVERITY,
            CheckInService.PAIN_SEVERE,
            CheckInService.OTHER_FEVER_SEVERITY,
            CheckInService.SEVERE);

    CheckIn checkIn2 = addCheckInToEnrollment(enrollment, LocalDate.now(), symptomSurvey);

    List<SurveyItemPayload> symptomsList = new ArrayList<>();
    symptomsList.add(symptomSurvey);

    SurveyPayload payload = new SurveyPayload();
    payload.content.setSymptoms(symptomsList);

    ArgumentCaptor valueCapture = ArgumentCaptor.forClass(Object.class);
    doNothing().when(rabbitTemplate).convertAndSend(any(String.class), valueCapture.capture());

    HealthTrackerStatusCommand command =
        new HealthTrackerStatusCommand(
            enrollment.getId(), payload, List.of(checkIn1.getId(), checkIn2.getId()));
    healthTrackerStatusService.accept(command);

    HealthTrackerStatus status = healthTrackerStatusService.getOrCreateNewStatus(enrollment);

    Assert.assertEquals("status should  be triage", "TRIAGE", status.getCategory().name());
    String obj = (String) valueCapture.getValue();

    verify(rabbitTemplate, times(1)).convertAndSend(any(String.class), any(Object.class));
    TriagePayload processed = JsonUtils.fromJson(obj, TriagePayload.class);
    Assert.assertEquals(
        "did not receive expected number of reasons", 3, processed.getReasons().size());
  }

  @Test
  public void givenMissedCheckIn_shouldNotMakeTicket() {
    Long id = 3295l;
    var testStartDate = LocalDate.now().minusDays(3);
    int daysOffset = 0;
    var enrollment = createEnrollment(id, id, id, testStartDate.plusDays(daysOffset++));

    // Day 0 - missed
    CheckIn checkIn1 =
        addMissedCheckInToEnrollment(enrollment, testStartDate.plusDays(daysOffset++));

    // Day 1 - symptoms & triage
    CheckIn checkIn2 =
        addCheckInToEnrollment(
            enrollment,
            testStartDate.plusDays(daysOffset++),
            HealthTrackerStatusServiceTest.createSurvey(
                CheckInService.SYMPTOM_FEVER_SEVERITY, CheckInService.MILD,
                CheckInService.PAIN_FEVER_SEVERITY, CheckInService.PAIN_SEVERE));

    // Process status
    var command =
        new HealthTrackerStatusCommand(
            enrollment.getId(), new SurveyPayload(), List.of(checkIn2.getId()));
    healthTrackerStatusService.accept(
        command); // user checkins status is processed via SQS message passing
    // There are symptoms, should get a new triage ticket
    var status = healthTrackerStatusRepository.getById(enrollment.getId());
    Assert.assertEquals(
        "status should be triage", HealthTrackerStatusCategory.TRIAGE, status.getCategory());

    // Day 2 - missed
    CheckIn checkIn3 =
        addMissedCheckInToEnrollment(enrollment, testStartDate.plusDays(daysOffset++));

    // Process status
    healthTrackerStatusService.processStatus(
        enrollment.getId(),
        null,
        List.of(checkIn3.getId())); // status check is processed with a function call

    // Last day checkin was missed, no more symptoms reported, should not get a new triage ticket
    status = healthTrackerStatusRepository.getById(enrollment.getId());
    Assert.assertNotEquals(
        "status should not be triage", HealthTrackerStatusCategory.TRIAGE, status.getCategory());
  }

  @Test
  public void givenPassedTriage_shouldNotMakeTicketOnStatusCheck() {
    Long id = 3296l;
    var testStartDate = LocalDate.now().minusDays(3);
    int daysOffset = 0;
    var enrollment = createEnrollment(id, id, id, testStartDate.plusDays(daysOffset++));

    // Day 1 - symptoms & triage
    CheckIn checkIn =
        addCheckInToEnrollment(
            enrollment,
            testStartDate.plusDays(daysOffset++),
            HealthTrackerStatusServiceTest.createSurvey(
                CheckInService.SYMPTOM_FEVER_SEVERITY, CheckInService.MILD,
                CheckInService.PAIN_FEVER_SEVERITY, CheckInService.PAIN_SEVERE));

    // Process status
    var command =
        new HealthTrackerStatusCommand(
            enrollment.getId(), new SurveyPayload(), List.of(checkIn.getId()));
    healthTrackerStatusService.accept(
        command); // user checkins status is processed via SQS message passing
    // There are symptoms, should get a new triage ticket
    var status = healthTrackerStatusRepository.getById(enrollment.getId());
    Assert.assertEquals(
        "status should be triage", HealthTrackerStatusCategory.TRIAGE, status.getCategory());

    // If the ticket was cleared
    status.setCategory(null);
    healthTrackerStatusRepository.save(status);

    // Process status
    healthTrackerStatusService.processStatus(
        enrollment.getId(), null, List.of()); // status check is processed with a function call
    // Is is only status check, should not raise triage ticket
    status = healthTrackerStatusRepository.getById(enrollment.getId());
    Assert.assertNotEquals(
        "status should not be triage", HealthTrackerStatusCategory.TRIAGE, status.getCategory());
  }

  @Ignore
  // TODO. As of now (April 2020) it is considered OK to clear the triage status on the PRO board
  // even if the actual triage ticket was not resolved.
  // We may want to resurrect this test in the future if we'll decide to keep triage state intact
  // unless it is explicitly cleared by the provider.
  public void givenStatusInTriage_shouldNotCleanUpStatusOnStatusCheck() {
    Long id = 3297l;
    var testStartDate = LocalDate.now().minusDays(3);
    int daysOffset = 0;
    var enrollment = createEnrollment(id, id, id, testStartDate.plusDays(daysOffset++));

    // Day 1 - symptoms & triage
    CheckIn checkIn =
        addCheckInToEnrollment(
            enrollment,
            testStartDate.plusDays(daysOffset++),
            HealthTrackerStatusServiceTest.createSurvey(
                CheckInService.SYMPTOM_FEVER_SEVERITY, CheckInService.MILD,
                CheckInService.PAIN_FEVER_SEVERITY, CheckInService.PAIN_SEVERE));

    // Process status
    var command =
        new HealthTrackerStatusCommand(
            enrollment.getId(), new SurveyPayload(), List.of(checkIn.getId()));
    healthTrackerStatusService.accept(
        command); // user checkins status is processed via SQS message passing
    // There are symptoms, should get a new triage ticket
    var status = healthTrackerStatusRepository.getById(enrollment.getId());
    Assert.assertEquals(
        "status should be triage", HealthTrackerStatusCategory.TRIAGE, status.getCategory());

    // Process status
    healthTrackerStatusService.processStatus(enrollment.getId(), null, List.of());
    // If triage ticket was not closed status check should not clear it
    status = healthTrackerStatusRepository.getById(enrollment.getId());
    Assert.assertEquals(
        "status should be triage", HealthTrackerStatusCategory.TRIAGE, status.getCategory());
  }
}
