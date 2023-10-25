package com.navigatingcancer.healthtracker.api.processor.model;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.navigatingcancer.healthtracker.api.TestConfig;
import com.navigatingcancer.healthtracker.api.data.client.PatientInfoServiceClient;
import com.navigatingcancer.healthtracker.api.data.model.CheckIn;
import com.navigatingcancer.healthtracker.api.data.model.CheckInType;
import com.navigatingcancer.healthtracker.api.data.model.Enrollment;
import com.navigatingcancer.healthtracker.api.data.model.HealthTrackerStatus;
import com.navigatingcancer.healthtracker.api.data.model.HealthTrackerStatusCategory;
import com.navigatingcancer.healthtracker.api.data.model.patientInfo.PatientInfo;
import com.navigatingcancer.healthtracker.api.data.model.survey.SurveyItemPayload;
import com.navigatingcancer.healthtracker.api.data.model.survey.SurveyPayload;
import com.navigatingcancer.healthtracker.api.data.repo.CheckInRepository;
import com.navigatingcancer.healthtracker.api.data.repo.EnrollmentRepository;
import com.navigatingcancer.healthtracker.api.data.repo.HealthTrackerStatusRepository;
import com.navigatingcancer.healthtracker.api.data.service.CheckInService;
import com.navigatingcancer.healthtracker.api.processor.DefaultDroolsServiceTest;
import com.navigatingcancer.healthtracker.api.processor.HealthTrackerStatusService;
import com.navigatingcancer.healthtracker.api.processor.HealthTrackerStatusServiceTest;
import com.navigatingcancer.healthtracker.api.processor.TriageTicketListener;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
@Import(TestConfig.class)
@TestPropertySource(
    properties = {
      "triage.saga.enabled=true"
    }) // Test operates with triage ticket saga feature turned on
public class StatusServiceTriageTicketSagaTest {

  @Autowired EnrollmentRepository enrollmentRepository;

  @Autowired CheckInRepository checkinRepository;

  @Autowired TriageTicketListener triageTicketListener;
  @Autowired HealthTrackerStatusService healthTrackerStatusService;

  @Autowired HealthTrackerStatusRepository healthTrackerStatusRepository;

  @MockBean private PatientInfoServiceClient patientInfoClient;

  @MockBean private RabbitTemplate rabbitTemplate;

  @Autowired private ObjectMapper objectMapper;

  private Long patientId = 5L;

  // random numbers generator
  private static final Random rng = new Random(123l);

  @Before
  public void setup() {
    PatientInfo patientInfo = new PatientInfo();
    patientInfo.setHighRisk(true);
    patientInfo.setId(patientId);

    PatientInfoServiceClient.FeignClient client =
        Mockito.mock(PatientInfoServiceClient.FeignClient.class);
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

  @Test
  public void testTriageTicketSaga() throws JsonProcessingException {
    Long clinic1 = rng.nextLong();
    Long location1 = rng.nextLong();
    Long patient1 = rng.nextLong();

    Enrollment enrollment = createEnrollment(clinic1, location1, patient1);

    // report severe symptoms that must create triage ticket
    SurveyItemPayload sip =
        HealthTrackerStatusServiceTest.createSurvey(
            CheckInService.PAIN_FEVER_SEVERITY, CheckInService.VERY_SEVERE);
    SurveyPayload sp = new SurveyPayload();
    sp.content.setSymptoms(Arrays.asList(sip));
    CheckIn ci =
        DefaultDroolsServiceTest.createCheckInsFromSurveyPayload(
            CheckInType.SYMPTOM, LocalDate.now().minusDays(1), sip);
    ci.setEnrollmentId(enrollment.getId());
    ci = this.checkinRepository.insert(ci);

    HealthTrackerStatusCommand command =
        new HealthTrackerStatusCommand(enrollment.getId(), sp, List.of(ci.getId()));
    // process status
    healthTrackerStatusService.accept(command);

    // Make sure there was the triage ticket request sent to GC
    ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
    // There supposed to be a PRO report and triage ticket request
    verify(rabbitTemplate, times(2)).convertAndSend(any(String.class), argument.capture());

    // Make sure there is a tiage ticket request there
    List<String> rabbitStringPayloads = argument.getAllValues();
    String triageSeverity = "\"alert_level\":\"immediate\"";
    boolean hasTriage =
        rabbitStringPayloads.get(0).contains(triageSeverity)
            || rabbitStringPayloads.get(1).contains(triageSeverity);
    Assert.assertTrue("rabbit must have a triage ticket request in the queue", hasTriage);

    // get back status
    HealthTrackerStatus htStatus = this.healthTrackerStatusRepository.getById(enrollment.getId());
    // make sure the status is action needed while we are waiting for GC ACK
    Assert.assertEquals(HealthTrackerStatusCategory.ACTION_NEEDED, htStatus.getCategory());

    // simulate GC ACK
    Map<String, Object> messageMap =
        Map.of(
            "patient_id", patient1,
            "clinic_id", clinic1,
            "updated_by_security_identity_id", patient1,
            "updated_by_name", "John Doe",
            "status", "triage");

    Message message =
        new Message(objectMapper.writeValueAsBytes(messageMap), new MessageProperties());
    triageTicketListener.onMessage(message);

    // make sure we switch to TRIAGE
    HealthTrackerStatus htStatus2 = this.healthTrackerStatusRepository.getById(enrollment.getId());
    Assert.assertEquals(HealthTrackerStatusCategory.TRIAGE, htStatus2.getCategory());
  }
}
