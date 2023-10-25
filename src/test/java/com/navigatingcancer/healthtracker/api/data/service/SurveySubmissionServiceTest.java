package com.navigatingcancer.healthtracker.api.data.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.navigatingcancer.healthtracker.api.TestConfig;
import com.navigatingcancer.healthtracker.api.data.client.gc.GcApiClient;
import com.navigatingcancer.healthtracker.api.data.model.CheckIn;
import com.navigatingcancer.healthtracker.api.data.model.Enrollment;
import com.navigatingcancer.healthtracker.api.data.model.PracticeCheckIn;
import com.navigatingcancer.healthtracker.api.data.model.survey.*;
import com.navigatingcancer.healthtracker.api.data.repo.CheckInRepository;
import com.navigatingcancer.healthtracker.api.data.repo.EnrollmentRepository;
import com.navigatingcancer.healthtracker.api.data.repo.surveyInstance.SurveyInstanceRepository;
import com.navigatingcancer.healthtracker.api.rest.exception.BadDataException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
@Import(TestConfig.class)
public class SurveySubmissionServiceTest {

  @Autowired private SurveySubmissionService surveySubmissionService;
  @MockBean private CheckInService checkInService;
  @MockBean private CheckInRepository checkInRepository;
  @MockBean private EnrollmentRepository enrollmentRepository;
  @MockBean private SurveyInstanceRepository surveyInstanceRepository;
  @MockBean private GcApiClient gcApiClient;

  private SurveySubmission submission;
  private CheckIn checkIn;
  private Enrollment enrollment;
  private SurveyInstance surveyInstance;
  @Captor ArgumentCaptor<SurveyPayload> surveyPayloadArgumentCaptor;

  @Before
  public void before() {
    submission = new SurveySubmission();
    submission.setCheckInId("testId");
    checkIn = new CheckIn();
    checkIn.setEnrollmentId("testEnrollment");
    enrollment = new Enrollment();
    surveyInstance = new SurveyInstance();
    surveyInstance.setSurveyId("testId");
    Mockito.when(checkInRepository.findById(Mockito.any())).thenReturn(Optional.of(checkIn));
    Mockito.when(enrollmentRepository.findById(Mockito.any())).thenReturn(Optional.of(enrollment));
    Mockito.when(surveyInstanceRepository.findById(Mockito.any()))
        .thenReturn(Optional.of(surveyInstance));
  }

  @Test
  public void process_shouldProcessOralSurvey() {
    surveyInstance.setSurveyId(SurveyId.ORAL_ADHERENCE_PX);
    submission.setSurveyPayload(Collections.singletonMap("testOral", "yes"));
    surveySubmissionService.process(submission);
    verify(checkInService).checkIn(surveyPayloadArgumentCaptor.capture());
    List<SurveyItemPayload> orals = surveyPayloadArgumentCaptor.getValue().getOral();
    assertEquals(orals.size(), 1);
    assertEquals(orals.get(0).getPayload().get("testOral"), "yes");
  }

  @Test
  public void process_shouldProcessSymptomSurvey() {
    surveyInstance.setSurveyId(SurveyId.HEALTH_TRACKER_PX);
    Map<String, Object> payload = new HashMap<String, Object>();
    payload.put("declineACall", false);
    payload.put("testSymptom", "ouch");
    submission.setSurveyPayload(payload);
    surveySubmissionService.process(submission);
    verify(checkInService).checkIn(surveyPayloadArgumentCaptor.capture());
    List<SurveyItemPayload> symptoms = surveyPayloadArgumentCaptor.getValue().getSymptoms();
    assertEquals(1, symptoms.size());
    assertEquals("ouch", symptoms.get(0).getPayload().get("testSymptom"));
    assertEquals(false, symptoms.get(0).getPayload().get("declineACall"));
  }

  @Test
  public void process_shouldProcessSymptomSurvey_handleDeclineACallStrings_false() {
    surveyInstance.setSurveyId(SurveyId.HEALTH_TRACKER_PX);

    submission.setSurveyPayload(Map.of("declineACall", List.of("false")));

    surveySubmissionService.process(submission);
    verify(checkInService).checkIn(surveyPayloadArgumentCaptor.capture());

    List<SurveyItemPayload> symptoms = surveyPayloadArgumentCaptor.getValue().getSymptoms();
    Assert.assertFalse(symptoms.get(0).isDeclineACall());
  }

  @Test
  public void process_shouldProcessSymptomSurvey_handleDeclineACallStrings_true() {
    surveyInstance.setSurveyId(SurveyId.HEALTH_TRACKER_PX);

    submission.setSurveyPayload(Map.of("declineACall", List.of("true")));

    surveySubmissionService.process(submission);
    verify(checkInService).checkIn(surveyPayloadArgumentCaptor.capture());

    List<SurveyItemPayload> symptoms = surveyPayloadArgumentCaptor.getValue().getSymptoms();
    Assert.assertTrue(symptoms.get(0).isDeclineACall());
  }

  @Test
  public void process_shouldProcessOralSurvey_handleMissedCheckIns() {
    surveyInstance.setSurveyId(SurveyId.ORAL_ADHERENCE_PX);
    submission.setSurveyPayload(
        Map.of(
            SurveySubmission.MISSED_CHECK_INS_QUESTION_ID,
            List.of(
                Map.of("id", "a missed check-in", "payload", Map.of("medicationTaken", true)))));

    surveySubmissionService.process(submission);
    verify(checkInService).checkIn(surveyPayloadArgumentCaptor.capture());

    List<SurveyItemPayload> adherencePayloads = surveyPayloadArgumentCaptor.getValue().getOral();
    assertEquals(2, adherencePayloads.size());
    assertEquals(submission.getCheckInId(), adherencePayloads.get(0).getId());
    assertEquals("a missed check-in", adherencePayloads.get(1).getId());
  }

  @Test
  public void process_shouldProcessNccnDistress() {
    surveyInstance.setSurveyId(SurveyId.NCCN_2022_DISTRESS);
    Map<String, Object> payload = new HashMap<String, Object>();
    payload.put("otherConcerns", "testConcern");
    payload.put(
        "emotionalConcerns",
        List.of("emotional_anxiety", "emotional_depression", "emotional_loss_of_enjoyment"));

    submission.setSurveyPayload(payload);
    submission.setCheckInId("testId");
    surveySubmissionService.process(submission);

    verify(checkInRepository, times(1)).updateFieldsById(eq("testId"), Mockito.any());
    verify(gcApiClient, times(1))
        .submitSurvey(Mockito.any(), eq(SurveyId.NCCN_2022_DISTRESS), Mockito.any());
  }

  @Test
  public void process_shouldProcessPracticeSurvey() {
    SurveySubmission submission = new SurveySubmission();
    submission.setCheckInId(SurveySubmission.PRACTICE_CHECKIN_ID);
    surveySubmissionService.process(submission);
    verify(checkInService, times(1)).persistPracticeCheckInData(Mockito.any(PracticeCheckIn.class));
  }

  @Test
  public void process_throwsBadDataIfCheckInNotFound() {
    Mockito.when(checkInRepository.findById(Mockito.any())).thenReturn(Optional.empty());
    Assert.assertThrows(BadDataException.class, () -> surveySubmissionService.process(submission));
  }

  @Test
  public void process_throwsBadDataIfEnrollmentNotFound() {
    Mockito.when(enrollmentRepository.findById(Mockito.any())).thenReturn(Optional.empty());
    Assert.assertThrows(BadDataException.class, () -> surveySubmissionService.process(submission));
  }

  @Test
  public void process_throwsBadDataIfSurveyInstanceNotFound() {
    Mockito.when(surveyInstanceRepository.findById(Mockito.any())).thenReturn(Optional.empty());
    Assert.assertThrows(BadDataException.class, () -> surveySubmissionService.process(submission));
  }

  @Test
  public void process_throwsBadDataIfSurveyIdUnknown() {
    Assert.assertThrows(BadDataException.class, () -> surveySubmissionService.process(submission));
  }
}
