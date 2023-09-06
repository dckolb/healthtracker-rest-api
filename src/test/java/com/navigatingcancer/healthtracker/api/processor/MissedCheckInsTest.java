package com.navigatingcancer.healthtracker.api.processor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.navigatingcancer.healthtracker.api.TestConfig;
import com.navigatingcancer.healthtracker.api.data.model.*;
import com.navigatingcancer.healthtracker.api.data.model.patientrecord.EnrollmentUpdatePayload;
import com.navigatingcancer.healthtracker.api.data.model.survey.SurveyItemPayload;
import com.navigatingcancer.healthtracker.api.data.repo.CheckInRepository;
import com.navigatingcancer.healthtracker.api.data.repo.EnrollmentRepository;
import com.navigatingcancer.healthtracker.api.data.repo.HealthTrackerStatusRepository;
import com.navigatingcancer.healthtracker.api.data.service.impl.PatientRecordService;
import com.navigatingcancer.healthtracker.api.data.service.impl.SchedulingServiceImpl;
import com.navigatingcancer.healthtracker.api.processor.model.HealthTrackerStatusCommand;
import com.navigatingcancer.json.utils.JsonUtils;
import com.navigatingcancer.notification.client.service.NotificationServiceClient;
import com.navigatingcancer.patientinfo.PatientInfoClient;
import com.navigatingcancer.patientinfo.domain.PatientInfo;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
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
@Import(TestConfig.class)
public class MissedCheckInsTest {
  @Autowired EnrollmentRepository enrollmentRepository;

  @Autowired CheckInRepository checkinRepository;

  @Autowired HealthTrackerStatusRepository statusRepository;

  @Autowired HealthTrackerStatusService healthTrackerStatusService;

  @MockBean private PatientInfoClient patientInfoClient;

  @MockBean private NotificationServiceClient notificationServiceClient;

  @MockBean private SchedulingServiceImpl schedulingService;

  @MockBean private RabbitTemplate rabbitTemplate;

  @Autowired private PatientRecordService patientRecordService;

  @Before
  public void setup() {

    PatientInfo patientInfo = new PatientInfo();
    patientInfo.setHighRisk(true);

    PatientInfoClient.FeignClient client = Mockito.mock(PatientInfoClient.FeignClient.class);
    Mockito.when(patientInfoClient.getApi()).thenReturn(client);
    Mockito.when(client.getPatients(Mockito.any(), Mockito.any()))
        .thenReturn(Arrays.asList(patientInfo));
  }

  private Enrollment createEnrollment(
      Long clinicId, Long locationId, Long patientId, SurveyItemPayload... items) {
    return HealthTrackerStatusServiceTest.createEnrollment(
        enrollmentRepository,
        checkinRepository,
        clinicId,
        locationId,
        patientId,
        LocalDate.now(),
        items);
  }

  private CheckIn createCheckIn(
      String enrollmentId, LocalDate scheduledDate, SurveyItemPayload item, CheckInStatus status) {
    CheckIn checkIn = new CheckIn();
    checkIn.setCheckInType(CheckInType.ORAL);
    checkIn.setScheduleDate(scheduledDate);
    checkIn.setSurveyPayload(item);
    checkIn.setStatus(status);
    checkIn.setEnrollmentId(enrollmentId);

    checkinRepository.save(checkIn);
    return checkIn;
  }

  private CheckIn createCheckInMissed(String enrollmentId, LocalDate scheduledDate) {
    return createCheckIn(enrollmentId, scheduledDate, null, CheckInStatus.MISSED);
  }

  private CheckIn createCheckInPending(String enrollmentId, LocalDate scheduledDate) {
    return createCheckIn(enrollmentId, scheduledDate, null, CheckInStatus.PENDING);
  }

  private CheckIn createCheckInComplete(String enrollmentId, LocalDate scheduledDate) {
    return createCheckIn(enrollmentId, scheduledDate, null, CheckInStatus.COMPLETED);
  }

  @Test
  public void whenMissThreeCheckIns_shouldSayThreeMissedCheckins_andShouldBeActionNeeded() {
    Enrollment enrollment = createEnrollment(1L, 1L, 1L);
    createCheckInMissed(enrollment.getId(), LocalDate.now().minusDays(3));
    createCheckInMissed(enrollment.getId(), LocalDate.now().minusDays(2));
    createCheckInMissed(enrollment.getId(), LocalDate.now().minusDays(1));
    createCheckInPending(enrollment.getId(), LocalDate.now());

    doNothing().when(this.rabbitTemplate).convertAndSend(isA(String.class), isA(String.class));

    // capture rabbit arg
    ArgumentCaptor<String> argumentCaptorPayload = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<HealthTrackerStatus> argumentCaptorStatus =
        ArgumentCaptor.forClass(HealthTrackerStatus.class);

    HealthTrackerStatusCommand command = new HealthTrackerStatusCommand(enrollment.getId(), null);
    this.healthTrackerStatusService.accept(command);

    // capture what's being sent to rabbit
    verify(this.rabbitTemplate, times(1))
        .convertAndSend(any(String.class), argumentCaptorPayload.capture());

    // verify status is properly set
    HealthTrackerStatus status = this.statusRepository.getById(enrollment.getId());
    Assert.assertNotNull(status);
    Assert.assertEquals(HealthTrackerStatusCategory.ACTION_NEEDED, status.getCategory());

    List<String> payloads = argumentCaptorPayload.getAllValues();
    String payload = payloads.get(0);
    EnrollmentUpdatePayload updatePayload =
        JsonUtils.fromJson(payload, EnrollmentUpdatePayload.class);
    Assert.assertNotNull(updatePayload);
    Assert.assertEquals(
        "3 missed check-ins", updatePayload.getSystemReportedMissedCheckInDescription());
  }

  @Test
  public void whenMissTwoAndTreeCheckIns_shouldSayTwoMissedCheckins_andShouldBeActionNeeded() {
    Enrollment enrollment = createEnrollment(1L, 1L, 2L);
    LocalDate ld = LocalDate.now();
    long dateOffset = 0l;
    createCheckInPending(enrollment.getId(), ld.minusDays(dateOffset++));
    createCheckInMissed(enrollment.getId(), ld.minusDays(dateOffset++));
    createCheckInMissed(enrollment.getId(), ld.minusDays(dateOffset++));
    createCheckInComplete(enrollment.getId(), ld.minusDays(dateOffset++));
    createCheckInMissed(enrollment.getId(), ld.minusDays(dateOffset++));
    createCheckInMissed(enrollment.getId(), ld.minusDays(dateOffset++));
    createCheckInMissed(enrollment.getId(), ld.minusDays(dateOffset++));
    createCheckInComplete(enrollment.getId(), ld.minusDays(dateOffset++));

    doNothing().when(this.rabbitTemplate).convertAndSend(isA(String.class), isA(String.class));

    // capture rabbit arg
    ArgumentCaptor<String> argumentCaptorPayload = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<HealthTrackerStatus> argumentCaptorStatus =
        ArgumentCaptor.forClass(HealthTrackerStatus.class);

    HealthTrackerStatusCommand command = new HealthTrackerStatusCommand(enrollment.getId(), null);
    this.healthTrackerStatusService.accept(command);

    // capture what's being sent to rabbit
    verify(this.rabbitTemplate, times(1))
        .convertAndSend(any(String.class), argumentCaptorPayload.capture());

    // verify status is properly set
    HealthTrackerStatus status = this.statusRepository.getById(enrollment.getId());
    Assert.assertNotNull(status);
    Assert.assertEquals(HealthTrackerStatusCategory.ACTION_NEEDED, status.getCategory());

    List<String> payloads = argumentCaptorPayload.getAllValues();
    String payload = payloads.get(0);
    EnrollmentUpdatePayload updatePayload =
        JsonUtils.fromJson(payload, EnrollmentUpdatePayload.class);
    Assert.assertNotNull(updatePayload);
    Assert.assertEquals(
        "2 missed check-ins", updatePayload.getSystemReportedMissedCheckInDescription());
  }
}
