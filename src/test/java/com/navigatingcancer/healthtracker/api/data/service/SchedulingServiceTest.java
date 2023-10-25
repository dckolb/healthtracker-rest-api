package com.navigatingcancer.healthtracker.api.data.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.navigatingcancer.healthtracker.api.TestConfig;
import com.navigatingcancer.healthtracker.api.data.client.PatientInfoServiceClient;
import com.navigatingcancer.healthtracker.api.data.model.CheckIn;
import com.navigatingcancer.healthtracker.api.data.model.CheckInData;
import com.navigatingcancer.healthtracker.api.data.model.CheckInFrequency;
import com.navigatingcancer.healthtracker.api.data.model.CheckInSchedule;
import com.navigatingcancer.healthtracker.api.data.model.CheckInStatus;
import com.navigatingcancer.healthtracker.api.data.model.CheckInType;
import com.navigatingcancer.healthtracker.api.data.model.Enrollment;
import com.navigatingcancer.healthtracker.api.data.model.EnrollmentStatus;
import com.navigatingcancer.healthtracker.api.data.model.patientInfo.PatientInfo;
import com.navigatingcancer.healthtracker.api.data.model.schedule.TriggerPayload;
import com.navigatingcancer.healthtracker.api.data.model.schedule.TriggerPayload.TriggerType;
import com.navigatingcancer.healthtracker.api.data.repo.CheckInRepository;
import com.navigatingcancer.healthtracker.api.data.repo.EnrollmentRepository;
import com.navigatingcancer.healthtracker.api.data.repo.EnrollmentRepositoryTest;
import com.navigatingcancer.healthtracker.api.data.service.impl.PatientRecordService;
import com.navigatingcancer.healthtracker.api.data.service.impl.SchedulingServiceImpl;
import com.navigatingcancer.json.utils.JsonUtils;
import com.navigatingcancer.notification.client.service.NotificationServiceClient;
import com.navigatingcancer.scheduler.client.domain.SchedulePayload;
import com.navigatingcancer.scheduler.client.domain.TriggerEvent;
import com.navigatingcancer.scheduler.client.service.SchedulerServiceClient;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Date;
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
public class SchedulingServiceTest {

  @MockBean private SchedulerServiceClient schedulerServiceClient;

  @MockBean private NotificationServiceClient notificationServiceClient;

  @Autowired private SchedulingServiceImpl schedulingService;

  @Autowired private EnrollmentRepository enrollmentRepository;

  @Autowired private CheckInRepository checkInRepository;

  @MockBean PatientRecordService patientRecordService;

  @MockBean RabbitTemplate rabbitTemplate;

  @Autowired private CheckInService checkinService;

  @MockBean private PatientInfoServiceClient patientInfoClient;

  @Before
  public void setup() {
    SchedulerServiceClient.FeignClient mock =
        Mockito.mock(SchedulerServiceClient.FeignClient.class);

    given(schedulerServiceClient.getApi()).willReturn(mock);
    given(schedulerServiceClient.getApi(any())).willReturn(mock);

    Mockito.doNothing().when(notificationServiceClient).send(any());

    PatientInfo patientInfo = new PatientInfo();
    patientInfo.setHighRisk(true);
    PatientInfoServiceClient.FeignClient client =
        Mockito.mock(PatientInfoServiceClient.FeignClient.class);
    Mockito.when(patientInfoClient.getApi()).thenReturn(client);
    Mockito.when(client.getPatients(Mockito.any(), Mockito.any()))
        .thenReturn(Arrays.asList(patientInfo));
  }

  @Test
  public void testSchedule() {

    LocalDate startDate = LocalDate.now().minusDays(2);

    CheckInSchedule schedule = new CheckInSchedule();
    schedule.setStartDate(startDate);
    schedule.setCheckInFrequency(CheckInFrequency.DAILY);
    schedule.setCheckInType(CheckInType.ORAL);

    Enrollment en =
        enrollmentRepository.save(
            EnrollmentRepositoryTest.createEnrollmentWithSchedules(5, 5, 5, schedule));

    LocalDate endDate = startDate.plusDays(en.getCycles() * en.getDaysInCycle() - 1);

    ArgumentCaptor<SchedulePayload> argument = ArgumentCaptor.forClass(SchedulePayload.class);
    schedulingService.schedule(en, true);
    verify(schedulerServiceClient.getApi()).schedule(any(), argument.capture());

    // must create 2 triggers: system, status & reminders
    SchedulePayload payload = argument.getValue();
    Assert.assertEquals(7, payload.getItems().size());
    Assert.assertEquals(startDate, payload.getItems().get(0).getStartDate());
    Assert.assertEquals(startDate, payload.getItems().get(1).getStartDate());
    Assert.assertEquals(startDate, payload.getItems().get(2).getStartDate());
    // Enrollment status check on end date + 1
    Assert.assertEquals(endDate.plusDays(1), payload.getItems().get(3).getStartDate());

    Assert.assertNotNull(payload.getItems().get(0).getData());
    Assert.assertNotNull(payload.getItems().get(1).getData());
    Assert.assertNotNull(payload.getItems().get(2).getData());
  }

  @Test
  public void testReSchedule() {

    CheckInSchedule schedule = new CheckInSchedule();
    LocalDate nowDate = LocalDate.now();
    LocalDate plannedStartDate = nowDate.minusDays(3);
    LocalDate actualStartDate = plannedStartDate;
    schedule.setStartDate(plannedStartDate);
    schedule.setCheckInFrequency(CheckInFrequency.DAILY);
    schedule.setCheckInType(CheckInType.ORAL);

    Enrollment en =
        enrollmentRepository.save(
            EnrollmentRepositoryTest.createEnrollmentWithSchedules(55, 55, 55, schedule));

    LocalDate endDate = plannedStartDate.plusDays(en.getCycles() * en.getDaysInCycle() - 1);

    ArgumentCaptor<SchedulePayload> argument = ArgumentCaptor.forClass(SchedulePayload.class);
    schedulingService.schedule(en, false);
    verify(schedulerServiceClient.getApi()).stopJob(any(), any());
    verify(schedulerServiceClient.getApi()).schedule(any(), argument.capture());

    // must create 2 triggers: system, status & reminders
    SchedulePayload payload = argument.getValue();
    Assert.assertEquals(7, payload.getItems().size());
    Assert.assertEquals(actualStartDate, payload.getItems().get(0).getStartDate());
    Assert.assertEquals(actualStartDate, payload.getItems().get(1).getStartDate());
    Assert.assertEquals(actualStartDate, payload.getItems().get(2).getStartDate());
    // Enrollment status check on end date + 1
    Assert.assertEquals(endDate.plusDays(1), payload.getItems().get(3).getStartDate());

    Assert.assertNotNull(payload.getItems().get(0).getData());
    Assert.assertNotNull(payload.getItems().get(1).getData());
    Assert.assertNotNull(payload.getItems().get(2).getData());
  }

  @Test
  public void testNoScheduleForInactive() {
    CheckInSchedule schedule = new CheckInSchedule();
    schedule.setStartDate(LocalDate.of(2019, 6, 1));
    schedule.setCheckInFrequency(CheckInFrequency.DAILY);
    schedule.setCheckInType(CheckInType.ORAL);

    Enrollment en =
        enrollmentRepository.save(
            EnrollmentRepositoryTest.createEnrollmentWithSchedules(
                55, 55, 55, EnrollmentStatus.STOPPED, schedule));

    schedulingService.schedule(en, true);

    ArgumentCaptor<SchedulePayload> argument = ArgumentCaptor.forClass(SchedulePayload.class);
    schedulingService.schedule(en, false);
    verify(schedulerServiceClient.getApi()).stopJob(any(), any());
    verify(schedulerServiceClient.getApi(), times(0)).schedule(any(), argument.capture());
  }

  @Test
  public void testTriggerEvent() {
    CheckInSchedule schedule = new CheckInSchedule();
    schedule.setCheckInFrequency(CheckInFrequency.DAILY);
    schedule.setCheckInType(CheckInType.ORAL);

    Enrollment en =
        enrollmentRepository.save(
            EnrollmentRepositoryTest.createEnrollmentWithSchedules(7, 7, 7, schedule));

    TriggerPayload triggerPayload =
        new TriggerPayload(en.getId(), CheckInType.ORAL, LocalTime.now(), TriggerType.SYSTEM);
    TriggerEvent triggerEvent =
        new TriggerEvent(JsonUtils.toJson(triggerPayload), new Date(), null);

    assertEquals(0L, checkInRepository.findByEnrollmentId(en.getId()).size());

    schedulingService.accept(triggerEvent);

    // Verify that checkin is created after processing trigger event
    assertEquals(1L, checkInRepository.findByEnrollmentId(en.getId()).size());
  }

  @Test
  public void testPendingCheckinsMarkedAsMissed() {
    Enrollment en =
        enrollmentRepository.save(EnrollmentRepositoryTest.createEnrollmentWithSchedules(7, 7, 7));

    LocalDate scheduledDate = LocalDate.of(2019, 10, 10);
    LocalTime fireTime = LocalTime.MAX;
    Date scheduledFireTime = Timestamp.valueOf(LocalDateTime.of(scheduledDate, fireTime));

    createPendingCheckIn(en.getId(), scheduledDate.minusDays(1));
    createPendingCheckIn(en.getId(), scheduledDate);
    createPendingCheckIn(en.getId(), scheduledDate.plusDays(1));

    TriggerPayload triggerPayload =
        new TriggerPayload(en.getId(), CheckInType.ORAL, LocalTime.now(), TriggerType.STATUS);
    TriggerEvent triggerEvent =
        new TriggerEvent(JsonUtils.toJson(triggerPayload), scheduledFireTime, null);
    schedulingService.accept(triggerEvent);

    List<CheckIn> checkIns =
        checkInRepository.findByEnrollmentIdOrderByScheduleDateDesc(en.getId());

    assertTrue(checkIns.size() == 3L);
    assertTrue(checkIns.get(0).getStatus() == CheckInStatus.PENDING);
    assertTrue(checkIns.get(1).getStatus() == CheckInStatus.MISSED);
    assertTrue(checkIns.get(2).getStatus() == CheckInStatus.MISSED);
  }

  @Test
  public void testNextCheckinDateSet() {
    CheckInSchedule schedule = new CheckInSchedule();
    schedule.setCheckInFrequency(CheckInFrequency.DAILY);
    schedule.setCheckInType(CheckInType.ORAL);

    Enrollment en =
        enrollmentRepository.save(
            EnrollmentRepositoryTest.createEnrollmentWithSchedules(7, 7, 7, schedule));

    LocalDate scheduledDate = LocalDate.now().plusDays(1);
    LocalTime fireTime = LocalTime.MAX;
    Date scheduledFireTime = Timestamp.valueOf(LocalDateTime.of(scheduledDate, fireTime));
    LocalDate scheduledNexDate = scheduledDate.plusDays(3); // Next day may be a few days ahead
    Date scheduledNextFireTime = Timestamp.valueOf(LocalDateTime.of(scheduledNexDate, fireTime));

    TriggerPayload triggerPayload =
        new TriggerPayload(en.getId(), CheckInType.ORAL, LocalTime.MAX, TriggerType.SYSTEM);
    TriggerEvent triggerEvent =
        new TriggerEvent(
            JsonUtils.toJson(triggerPayload), scheduledFireTime, scheduledNextFireTime);
    schedulingService.accept(triggerEvent);

    triggerPayload =
        new TriggerPayload(en.getId(), CheckInType.SYMPTOM, LocalTime.MAX, TriggerType.SYSTEM);
    triggerEvent = new TriggerEvent(JsonUtils.toJson(triggerPayload), scheduledNextFireTime, null);
    schedulingService.accept(triggerEvent);

    schedulingService.accept(triggerEvent);
    CheckInData checkinData = checkinService.getCheckInData(en.getId());
    Assert.assertNotNull(checkinData);
    CheckIn nextCi = checkinData.getNext();
    Assert.assertNotNull(nextCi);
    Assert.assertEquals(scheduledDate, nextCi.getScheduleDate());
  }

  private void createPendingCheckIn(String enrollmentId, LocalDate scheduledDate) {
    CheckIn checkIn = new CheckIn();

    checkIn.setCheckInType(CheckInType.ORAL);
    checkIn.setScheduleDate(scheduledDate);
    checkIn.setStatus(CheckInStatus.PENDING);
    checkIn.setEnrollmentId(enrollmentId);

    checkInRepository.save(checkIn);
  }
}
