package com.navigatingcancer.healthtracker.api.data.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.navigatingcancer.healthtracker.api.data.model.*;
import com.navigatingcancer.healthtracker.api.data.model.schedule.TriggerPayload;
import com.navigatingcancer.healthtracker.api.data.model.survey.SurveyItemPayload;
import com.navigatingcancer.healthtracker.api.data.repo.CheckInRepository;
import com.navigatingcancer.healthtracker.api.data.repo.EnrollmentRepository;
import com.navigatingcancer.healthtracker.api.data.repo.EnrollmentRepositoryTest;
import com.navigatingcancer.healthtracker.api.data.repo.HealthTrackerStatusRepository;
import com.navigatingcancer.healthtracker.api.data.service.impl.SchedulingServiceImpl;
import com.navigatingcancer.healthtracker.api.processor.model.HealthTrackerStatusCommand;
import com.navigatingcancer.notification.client.service.NotificationServiceClient;
import com.navigatingcancer.patientinfo.PatientInfoClient;
import com.navigatingcancer.patientinfo.domain.PatientInfo;
import com.navigatingcancer.scheduler.client.domain.TriggerEvent;
import com.navigatingcancer.scheduler.client.service.SchedulerServiceClient;
import com.navigatingcancer.sqs.SqsHelper;
import com.navigatingcancer.sqs.SqsProducer;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.*;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class SchedulingServiceITest {
  @MockBean private SchedulerServiceClient schedulerServiceClient;

  @MockBean private NotificationServiceClient notificationServiceClient;

  @Autowired private SchedulingServiceImpl schedulingService;

  @Autowired private EnrollmentRepository enrollmentRepository;

  @Autowired private CheckInRepository checkInRepository;

  @Autowired private HealthTrackerStatusRepository healthTrackerStatusRepository;

  @Autowired private ObjectMapper objectMapper;

  @MockBean SqsHelper sqsHelper;

  @MockBean RabbitTemplate rabbitTemplate;

  @MockBean PatientInfoClient patientInfoClient;

  private SqsProducer<HealthTrackerStatusCommand> mockProducer;

  @Before
  public void setup() throws Exception {
    MockitoAnnotations.initMocks(this);
    SchedulerServiceClient.FeignClient mock =
        Mockito.mock(SchedulerServiceClient.FeignClient.class);

    mockProducer = Mockito.mock(SqsProducer.class);

    given(sqsHelper.createProducer(any(Class.class), any(String.class))).willReturn(mockProducer);

    given(schedulerServiceClient.getApi()).willReturn(mock);
    given(schedulerServiceClient.getApi(any())).willReturn(mock);

    Mockito.doNothing().when(sqsHelper).startConsumer(any(), any());
    Mockito.doReturn("url").when(sqsHelper).getQueueUrl(any());

    PatientInfoClient.FeignClient mockPatientInfo =
        Mockito.mock(PatientInfoClient.FeignClient.class);
    given(patientInfoClient.getApi()).willReturn(mockPatientInfo);
    PatientInfo patientInfo = new PatientInfo();
    patientInfo.setHighRisk(false);
    given(mockPatientInfo.getPatients(any(), any())).willReturn(Arrays.asList(patientInfo));
    Mockito.doNothing().when(notificationServiceClient).send(any());
    Mockito.doReturn(mockProducer).when(sqsHelper).createProducer(Mockito.any(), Mockito.any());
  }

  @Test
  public void givenOneMissedCheckin_statusShouldBeNoActionNeeded_andPCPShouldSeeOneMissedCheckin()
      throws Exception {
    CheckInSchedule schedule = new CheckInSchedule();
    schedule.setEndDate(LocalDate.now().plusDays(3));
    schedule.setStartDate(LocalDate.now().minusDays(1));
    schedule.setCheckInType(CheckInType.ORAL);
    schedule.setCheckInFrequency(CheckInFrequency.DAILY);

    Enrollment en =
        enrollmentRepository.save(
            EnrollmentRepositoryTest.createEnrollmentWithSchedules(7, 7, 7, schedule));

    CheckIn ci = createCheckIn(en.getId(), LocalDate.now().minusDays(1), null);
    checkInRepository.save(ci);

    TriggerPayload triggerPayload =
        new TriggerPayload(
            en.getId(), CheckInType.ORAL, LocalTime.now(), TriggerPayload.TriggerType.STATUS);
    String payload = this.objectMapper.writeValueAsString(triggerPayload);
    TriggerEvent te =
        new TriggerEvent(
            payload,
            java.util.Date.from(
                LocalDate.now()
                    .minusDays(1)
                    .atStartOfDay()
                    .atZone(ZoneId.systemDefault())
                    .toInstant()),
            java.util.Date.from(
                LocalDate.now()
                    .plusDays(2)
                    .atStartOfDay()
                    .atZone(ZoneId.systemDefault())
                    .toInstant()));

    ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);

    this.schedulingService.accept(te);

    // capture what is sent to PCP
    verify(rabbitTemplate, times(1))
        .convertAndSend(any(String.class), stringArgumentCaptor.capture());

    List<String> stringPayloads = stringArgumentCaptor.getAllValues();
    String missedPayload = stringPayloads.get(0);
    Assert.assertTrue(missedPayload.contains("system_reported_missed_check_in"));

    Assert.assertTrue(missedPayload.contains("1 missed check-in"));

    // inspect status
    HealthTrackerStatus status = healthTrackerStatusRepository.getById(en.getId());

    Assert.assertNotNull(status);
    Assert.assertEquals(
        "missed checkins should be set to 1", 1l, status.getMissedCheckIns().longValue());
    Assert.assertNull(status.getCategory());
  }

  @Test
  public void givenThreeMissedCheckins_statusShouldBeActionNeeded_andPCPShouldSeeOneMissedCheckin()
      throws Exception {
    CheckInSchedule schedule = new CheckInSchedule();
    schedule.setEndDate(LocalDate.now().plusDays(3));
    schedule.setStartDate(LocalDate.now().minusDays(3));
    schedule.setCheckInType(CheckInType.ORAL);
    schedule.setCheckInFrequency(CheckInFrequency.DAILY);

    Enrollment en =
        enrollmentRepository.save(
            EnrollmentRepositoryTest.createEnrollmentWithSchedules(7, 7, 765, schedule));

    CheckIn ci = createCheckIn(en.getId(), LocalDate.now().minusDays(1), null);
    checkInRepository.save(ci);
    ci = createCheckIn(en.getId(), LocalDate.now().minusDays(2), null);
    checkInRepository.save(ci);
    ci = createCheckIn(en.getId(), LocalDate.now().minusDays(3), null);
    checkInRepository.save(ci);

    TriggerPayload triggerPayload =
        new TriggerPayload(
            en.getId(), CheckInType.ORAL, LocalTime.now(), TriggerPayload.TriggerType.STATUS);
    String payload = this.objectMapper.writeValueAsString(triggerPayload);
    TriggerEvent te =
        new TriggerEvent(
            payload,
            java.util.Date.from(
                LocalDate.now()
                    .minusDays(1)
                    .atStartOfDay()
                    .atZone(ZoneId.systemDefault())
                    .toInstant()),
            java.util.Date.from(
                LocalDate.now()
                    .plusDays(2)
                    .atStartOfDay()
                    .atZone(ZoneId.systemDefault())
                    .toInstant()));

    ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);

    this.schedulingService.accept(te);

    // capture what is sent to PCP
    verify(rabbitTemplate, times(1))
        .convertAndSend(any(String.class), stringArgumentCaptor.capture());

    List<String> stringPayloads = stringArgumentCaptor.getAllValues();
    String missedPayload = stringPayloads.get(0);
    Assert.assertTrue(missedPayload.contains("system_reported_missed_check_in"));

    Assert.assertTrue(missedPayload.contains("3 missed check-in"));

    // inspect status
    HealthTrackerStatus status = healthTrackerStatusRepository.getById(en.getId());

    Assert.assertNotNull(status);
    Assert.assertEquals(
        "missed checkins should be set to 3", 3l, status.getMissedCheckIns().longValue());
    Assert.assertEquals(HealthTrackerStatusCategory.ACTION_NEEDED, status.getCategory());
  }

  public static CheckIn createCheckIn(
      String enrollmentId, LocalDate scheduledDate, SurveyItemPayload item) {
    CheckIn checkIn = new CheckIn();
    checkIn.setCheckInType(CheckInType.ORAL);
    checkIn.setScheduleDate(scheduledDate);
    checkIn.setSurveyPayload(item);
    checkIn.setStatus(
        item != null && !item.getPayload().isEmpty()
            ? CheckInStatus.COMPLETED
            : CheckInStatus.PENDING);
    checkIn.setEnrollmentId(enrollmentId);

    return checkIn;
  }

  @Test
  public void testForDuplicateCheckins() throws JsonProcessingException, InterruptedException {
    CheckInSchedule schedule = new CheckInSchedule();
    schedule.setEndDate(LocalDate.now().plusDays(3));
    schedule.setStartDate(LocalDate.now().minusDays(3));
    schedule.setCheckInType(CheckInType.ORAL);
    schedule.setCheckInFrequency(CheckInFrequency.DAILY);
    Enrollment en =
        enrollmentRepository.save(
            EnrollmentRepositoryTest.createEnrollmentWithSchedules(79, 7, 7665, schedule));

    TriggerPayload triggerPayload =
        new TriggerPayload(
            en.getId(), CheckInType.ORAL, LocalTime.now(), TriggerPayload.TriggerType.SYSTEM);
    String payload = this.objectMapper.writeValueAsString(triggerPayload);
    TriggerEvent te =
        new TriggerEvent(
            payload,
            new Date(),
            java.util.Date.from(
                LocalDate.now()
                    .plusDays(2)
                    .atStartOfDay()
                    .atZone(ZoneId.systemDefault())
                    .toInstant()));

    ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);

    List<Callable<Integer>> calls = new ArrayList<>();
    Callable<Integer> c1 =
        () -> {
          this.schedulingService.accept(te);
          return 1;
        };
    calls.add(c1);
    Callable<Integer> c2 =
        () -> {
          this.schedulingService.accept(te);
          return 1;
        };

    calls.add(c2);
    ExecutorService es = Executors.newSingleThreadExecutor();
    // calling at the same time always yields one checkin
    // ExecutorService es = Executors.newFixedThreadPool(2);

    List<Future<Integer>> futures = es.invokeAll(calls);

    for (Future<Integer> f : futures) {
      try {
        System.out.println(f.get());
      } catch (ExecutionException e) {
        System.out.println("error");
      }
    }
    List<CheckIn> checkIns = this.checkInRepository.findByEnrollmentId(en.getId());
    Assert.assertEquals("should get one", 1l, checkIns.size());
  }
}
