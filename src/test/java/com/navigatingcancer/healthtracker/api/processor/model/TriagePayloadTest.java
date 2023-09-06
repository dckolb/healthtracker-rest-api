package com.navigatingcancer.healthtracker.api.processor.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.navigatingcancer.healthtracker.api.data.model.Enrollment;
import com.navigatingcancer.healthtracker.api.data.model.HealthTrackerStatus;
import com.navigatingcancer.healthtracker.api.data.model.HealthTrackerStatusCategory;
import com.navigatingcancer.healthtracker.api.data.model.survey.SurveyItemPayload;
import com.navigatingcancer.healthtracker.api.data.model.survey.SurveyPayload;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

public class TriagePayloadTest {

  @Test
  public void setSeverity_verySevere() {
    TriagePayload.Reason reason = new TriagePayload.Reason();
    reason.setSeverity("VeRy SeVeRe");
    assertEquals("Convert Very Severe to very-severe", "very-severe", reason.getSeverity());
  }

  @Test
  public void setSeverity_none() {
    TriagePayload.Reason reason = new TriagePayload.Reason();
    reason.setSeverity("None");
    assertNull("Don't store none", reason.getSeverity());
  }

  @Test
  public void setSeverity_somethingElse() {
    TriagePayload.Reason reason = new TriagePayload.Reason();
    reason.setSeverity("something else");
    assertEquals("Store any other severity as the same", "something else", reason.getSeverity());
  }

  @Test
  public void testCreateTriageIfNeeded() {
    // Arrange
    List<SurveyItemPayload> orals = new ArrayList<>();
    SurveyItemPayload itemPayload = new SurveyItemPayload();
    Map<String, Object> payload = new HashMap<>();
    payload.put("medicationStarted", "yes");
    payload.put("medicationTakenDate", LocalDate.now().toString());
    itemPayload.setPayload(payload);
    orals.add(itemPayload);
    SurveyPayload survey = new SurveyPayload();
    survey.content.setOral(orals);
    HealthTrackerStatus status = new HealthTrackerStatus();
    status.setSurveyPayload(survey);
    status.setCategory(HealthTrackerStatusCategory.TRIAGE);
    Enrollment enrollment = new Enrollment();

    // Act
    TriagePayload triage = TriagePayload.createTriageIfNeeded(status, enrollment);

    // Assert
    Assert.assertNotNull(triage);
  }
}
