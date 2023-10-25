package com.navigatingcancer.healthtracker.api.processor;

import com.navigatingcancer.healthtracker.api.TestConfig;
import com.navigatingcancer.healthtracker.api.data.model.*;
import com.navigatingcancer.healthtracker.api.data.model.survey.SurveyItemPayload;
import com.navigatingcancer.healthtracker.api.data.model.survey.SurveyPayload;
import com.navigatingcancer.healthtracker.api.data.repo.CheckInRepository;
import com.navigatingcancer.healthtracker.api.data.repo.EnrollmentRepository;
import com.navigatingcancer.healthtracker.api.data.repo.HealthTrackerStatusRepository;
import com.navigatingcancer.healthtracker.api.data.service.CheckInService;
import com.navigatingcancer.healthtracker.api.processor.model.HealthTrackerStatusCommand;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
@Import(TestConfig.class)
class TriageTicketListenerTest {
  @Autowired private TriageTicketListener triageTicketListener;
  @Autowired private HealthTrackerStatusService healthTrackerStatusService;
  @Autowired private EnrollmentRepository enrollmentRepository;
  @Autowired private CheckInRepository checkInRepository;
  @Autowired private HealthTrackerStatusRepository healthTrackerStatusRepository;

  @Test
  public void onMessage_updatesStatus() {
    SurveyItemPayload symptomSurvey =
        HealthTrackerStatusServiceTest.createSurvey(
            CheckInService.SYMPTOM_FEVER_SEVERITY, CheckInService.SEVERE);

    Enrollment enrollment =
        HealthTrackerStatusServiceTest.createEnrollment(
            enrollmentRepository, checkInRepository, 5L, 5L, 5L, LocalDate.now(), symptomSurvey);

    List<SurveyItemPayload> symptomsList = new ArrayList<>();
    symptomsList.add(symptomSurvey);

    SurveyPayload payload = new SurveyPayload();
    payload.setSymptoms(symptomsList);

    HealthTrackerStatusCommand command =
        new HealthTrackerStatusCommand(enrollment.getId(), payload, List.of());
    healthTrackerStatusService.accept(command);

    HealthTrackerStatus status = healthTrackerStatusService.getOrCreateNewStatus(enrollment);
    Assert.assertEquals(status.getCategory(), HealthTrackerStatusCategory.TRIAGE);

    String raw =
        "{\"patient_id\":5,\"clinic_id\":5,\"updated_by_name\":\"John"
            + " Doe\",\"updated_by_security_identity_id\":2,\"status\":\"watch_carefully\"}";
    Message message = new Message(raw.getBytes(), new MessageProperties());
    triageTicketListener.onMessage(message);

    status = healthTrackerStatusService.getOrCreateNewStatus(enrollment);
    Assert.assertEquals(HealthTrackerStatusCategory.WATCH_CAREFULLY, status.getCategory());

    // Verify status log enrty
    Enrollment enr2 = enrollmentRepository.findById(enrollment.getId()).get();
    EnrollmentStatusLog log = enr2.getStatusLogs().get(0);
    Assert.assertEquals(EnrollmentStatus.STATUS_CHANGE, log.getStatus());
    Assert.assertEquals(TriageTicketListener.TRIAGE_STATUS_CHANGE_REASON, log.getReason());
    Assert.assertEquals("2", log.getClinicianId());
    Assert.assertEquals("John Doe", log.getClinicianName());
  }

  @Test
  public void onMessage_recordsTriageMarkedAsError() {
    SurveyItemPayload symptomSurvey =
        HealthTrackerStatusServiceTest.createSurvey(
            CheckInService.SYMPTOM_FEVER_SEVERITY, CheckInService.SEVERE);

    Enrollment enrollment =
        HealthTrackerStatusServiceTest.createEnrollment(
            enrollmentRepository, checkInRepository, 5L, 5L, 5L, LocalDate.now(), symptomSurvey);

    List<SurveyItemPayload> symptomsList = new ArrayList<>();
    symptomsList.add(symptomSurvey);

    SurveyPayload payload = new SurveyPayload();
    payload.content.setSymptoms(symptomsList);

    HealthTrackerStatusCommand command =
        new HealthTrackerStatusCommand(enrollment.getId(), payload, List.of());
    healthTrackerStatusService.accept(command);

    HealthTrackerStatus status = healthTrackerStatusService.getOrCreateNewStatus(enrollment);
    Assert.assertEquals(status.getCategory(), HealthTrackerStatusCategory.TRIAGE);

    String raw =
        "{\"patient_id\":5,\"clinic_id\":5,\"updated_by_name\":\"John"
            + " Doe\",\"updated_by_security_identity_id\":2,\"status\":\"no_action_needed\",\"mark_as_error\":true}";
    Message message = new Message(raw.getBytes(), new MessageProperties());
    triageTicketListener.onMessage(message);

    status = healthTrackerStatusService.getOrCreateNewStatus(enrollment);
    Assert.assertEquals(HealthTrackerStatusCategory.NO_ACTION_NEEDED, status.getCategory());

    // Verify status log enrty
    Enrollment enr2 = enrollmentRepository.findById(enrollment.getId()).get();
    EnrollmentStatusLog log = enr2.getStatusLogs().get(0);
    Assert.assertEquals(EnrollmentStatus.STATUS_CHANGE, log.getStatus());
    Assert.assertEquals(TriageTicketListener.TRIAGE_MARKED_AS_ERROR_REASON, log.getReason());
    Assert.assertEquals("2", log.getClinicianId());
    Assert.assertEquals("John Doe", log.getClinicianName());
  }

  @Test
  public void onMessage_doesNotThrowForNullProReviewId() {
    SurveyItemPayload symptomSurvey =
        HealthTrackerStatusServiceTest.createSurvey(
            CheckInService.SYMPTOM_FEVER_SEVERITY, CheckInService.SEVERE);

    Enrollment enrollment =
        HealthTrackerStatusServiceTest.createEnrollment(
            enrollmentRepository, checkInRepository, 5L, 5L, 5L, LocalDate.now(), symptomSurvey);

    HealthTrackerStatus status = healthTrackerStatusService.getOrCreateNewStatus(enrollment);
    status.setProReviewId(null);
    healthTrackerStatusRepository.save(status);
    String raw =
        "{\"patient_id\":5,\"clinic_id\":5,\"updated_by_name\":\"John"
            + " Doe\",\"updated_by_security_identity_id\":2,\"status\":\"no_action_needed\",\"mark_as_error\":true}";
    Message message = new Message(raw.getBytes(), new MessageProperties());
    triageTicketListener.onMessage(message);
  }
}
