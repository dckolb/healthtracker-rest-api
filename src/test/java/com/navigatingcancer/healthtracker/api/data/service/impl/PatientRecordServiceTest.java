package com.navigatingcancer.healthtracker.api.data.service.impl;

import static org.mockito.Mockito.doNothing;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.navigatingcancer.healthtracker.api.TestConfig;
import com.navigatingcancer.healthtracker.api.data.auth.Identity;
import com.navigatingcancer.healthtracker.api.data.model.Enrollment;
import com.navigatingcancer.healthtracker.api.data.model.EnrollmentStatus;
import com.navigatingcancer.healthtracker.api.data.model.HealthTrackerStatus;
import com.navigatingcancer.healthtracker.api.data.model.patientInfo.PatientInfo;
import com.navigatingcancer.healthtracker.api.processor.model.ProFormatManager;
import com.navigatingcancer.healthtracker.api.processor.model.SymptomDetails;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(
    classes = {PatientRecordService.class, ProFormatManager.class, TestConfig.class})
public class PatientRecordServiceTest {

  @Autowired private PatientRecordService patientRecordService;

  @MockBean RabbitTemplate rabbitTemplate;

  @MockBean Identity identity;

  @Test
  public void testSentProToEhr() throws Exception {
    ArgumentCaptor<String> valueCapture = ArgumentCaptor.forClass(String.class);
    doNothing().when(rabbitTemplate).convertAndSend(Mockito.any(), valueCapture.capture());

    String enrollmentId = UUID.randomUUID().toString();
    patientRecordService.publishProSentToEhr(
        enrollmentId, "Test", 1L, 2L, "fakeproReviewId", "Health Tracker PRO");

    Map<String, Object> payload = extractPayload(valueCapture);
    Assert.assertNotNull(payload.get("message_id"));
    Assert.assertEquals(enrollmentId, payload.get("enrollment_id"));
    Assert.assertEquals(1, payload.get("clinic_id"));
    Assert.assertEquals(2, payload.get("patient_id"));
    Assert.assertEquals("fakeproReviewId", payload.get("pro_review_id"));
    Assert.assertEquals("Health Tracker PRO", payload.get("document_title"));
  }

  @Test
  public void whenEnrollment_expectUTC() throws Exception {
    Enrollment e = new Enrollment();
    e.setId(UUID.randomUUID().toString());
    e.setClinicId(1L);
    e.setPatientId(2L);
    e.setStatus(EnrollmentStatus.STOPPED);

    ArgumentCaptor<String> valueCapture = ArgumentCaptor.forClass(String.class);
    doNothing().when(rabbitTemplate).convertAndSend(Mockito.any(), valueCapture.capture());

    patientRecordService.publishEnrollmentCreated(e, identity);

    Map<String, Object> payload = extractPayload(valueCapture);

    String rawDate = (String) payload.get("record_timestamp");

    DateFormat df1 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    Date resultDate = df1.parse(rawDate);

    // if here, successfully parsed zoned date time
    Assert.assertNotNull(((Date) resultDate).getTime());
  }

  @Test
  public void whenMissed_expectMissedDescription() throws Exception {
    Enrollment e = new Enrollment();
    e.setId(UUID.randomUUID().toString());
    e.setClinicId(1L);
    e.setPatientId(2L);
    e.setStatus(EnrollmentStatus.ACTIVE);

    ArgumentCaptor<String> valueCapture = ArgumentCaptor.forClass(String.class);
    doNothing().when(rabbitTemplate).convertAndSend(Mockito.any(), valueCapture.capture());

    patientRecordService.publishMissedCheckIn(e, 5);

    String str = valueCapture.getValue();

    Assert.assertTrue(str.contains("missed check-in"));
  }

  @Test
  public void whenEndCurrentCycle_expectEndCurrentCycle() throws Exception {
    Enrollment e = new Enrollment();
    e.setId(UUID.randomUUID().toString());
    e.setClinicId(1L);
    e.setPatientId(2L);
    e.setStatus(EnrollmentStatus.ACTIVE);

    HealthTrackerStatus status = createStatus();
    status.setEndCurrentCycle(true);

    ArgumentCaptor<String> valueCapture = ArgumentCaptor.forClass(String.class);
    doNothing().when(rabbitTemplate).convertAndSend(Mockito.any(), valueCapture.capture());

    patientRecordService.publishProData(e, status, null);

    Map<String, Object> payload = extractPayload(valueCapture);

    Boolean endCurrentCycle = (Boolean) payload.get("end_current_cycle");

    Assert.assertTrue(endCurrentCycle);
  }

  @Test
  public void whenNotEndCurrentCycle_expectNotEndCurrentCycle() throws Exception {
    Enrollment e = new Enrollment();
    e.setId(UUID.randomUUID().toString());
    e.setClinicId(1L);
    e.setPatientId(2L);
    e.setStatus(EnrollmentStatus.ACTIVE);

    HealthTrackerStatus status = createStatus();
    status.setEndCurrentCycle(false);

    ArgumentCaptor<String> valueCapture = ArgumentCaptor.forClass(String.class);
    doNothing().when(rabbitTemplate).convertAndSend(Mockito.any(), valueCapture.capture());

    patientRecordService.publishProData(e, status, null);

    Map<String, Object> payload = extractPayload(valueCapture);

    Boolean endCurrentCycle = (Boolean) payload.get("end_current_cycle");

    Assert.assertFalse(endCurrentCycle);
  }

  private HealthTrackerStatus createStatus() {
    PatientInfo patientInfo = new PatientInfo();

    HealthTrackerStatus status = new HealthTrackerStatus();
    status.setPatientInfo(patientInfo);

    List<SymptomDetails> symptomDetails = new ArrayList<>();
    symptomDetails.add(new SymptomDetails());
    status.setSymptomDetails(symptomDetails);

    return status;
  }

  private Map<String, Object> extractPayload(ArgumentCaptor<String> valueCapture)
      throws JsonProcessingException {
    String payloadJson = valueCapture.getValue();
    ObjectMapper om = new ObjectMapper();
    return om.readerFor(Map.class).readValue(payloadJson);
  }
}
