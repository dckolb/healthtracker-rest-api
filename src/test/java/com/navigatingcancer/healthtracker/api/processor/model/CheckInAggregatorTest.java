package com.navigatingcancer.healthtracker.api.processor.model;

import com.navigatingcancer.healthtracker.api.data.model.CheckIn;
import com.navigatingcancer.healthtracker.api.data.model.CheckInResult;
import com.navigatingcancer.healthtracker.api.data.model.CheckInType;
import com.navigatingcancer.healthtracker.api.data.model.HealthTrackerStatus;
import com.navigatingcancer.healthtracker.api.data.model.patientInfo.PatientInfo;
import com.navigatingcancer.healthtracker.api.processor.DefaultDroolsServiceTest;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.Assert;
import org.junit.Test;

public class CheckInAggregatorTest {

  private CheckInAggregator createCheckInAggregator(List<CheckIn> checkIns) {
    PatientInfo pi = new PatientInfo();

    HealthTrackerStatus healthTrackerStatus = new HealthTrackerStatus();
    healthTrackerStatus.setPatientInfo(pi);

    return new CheckInAggregator(checkIns, healthTrackerStatus, false);
  }

  private CheckInAggregator createCheckInAggregator(boolean isHighRisk, String... itemPayloads) {

    List<CheckIn> checkIns =
        DefaultDroolsServiceTest.createCheckInsFromSurveyPayload(
            DefaultDroolsServiceTest.createSurvey(itemPayloads));
    PatientInfo pi = new PatientInfo();
    pi.setHighRisk(isHighRisk);

    HealthTrackerStatus healthTrackerStatus = new HealthTrackerStatus();
    healthTrackerStatus.setPatientInfo(pi);

    return new CheckInAggregator(checkIns, healthTrackerStatus, false);
  }

  @Test
  public void countAnswerAtLeastModerate_withInvalidInputs() {
    List<CheckIn> checkIns = new ArrayList<>();
    HealthTrackerStatus htStatus = new HealthTrackerStatus();

    CheckInAggregator cia = new CheckInAggregator(checkIns, htStatus, false);

    Assert.assertEquals(
        "0 for invalid input", 0, cia.countAnswerForMandatoryProCtcaeSymptoms(-1, "1,2,3"));
    Assert.assertEquals(
        "0 for invalid input", 0, cia.countAnswerForMandatoryProCtcaeSymptoms(1, ""));
  }

  @Test
  public void countAnswerAtLeastModerate_withLessThanModerateResponse() {
    CheckInAggregator cia = createCheckInAggregator(false, "nauseaSeverity", "1");
    Assert.assertEquals(
        "0 for response < 2", 0, cia.countAnswerForMandatoryProCtcaeSymptoms(0, "2,3,4"));
  }

  @Test
  public void countAnswerAtLeastModerate_withEqualToModerateResponse() {
    CheckInAggregator cia = createCheckInAggregator(false, "nauseaSeverity", "2");
    Assert.assertEquals(
        "1 count for response == 2", 1, cia.countAnswerForMandatoryProCtcaeSymptoms(0, "2,3,4"));
  }

  @Test
  public void addProCtcaeSymptom_withLessThanModerateResponse() {
    CheckInAggregator cia = createCheckInAggregator(false, "nauseaSeverity", "1");
    cia.addProCtcaeSymptom(0, "2,3,4");

    Set<CheckInResult> rules = cia.getHtStatus().getRuleResults();

    Assert.assertEquals("No rules are created", 0, rules.size());
  }

  @Test
  public void addProCtcaeSymptom_withModerateResponse() {
    CheckInAggregator cia = createCheckInAggregator(false, "nauseaSeverity", "2");
    cia.addProCtcaeSymptom(0, "2,3,4");
    Set<CheckInResult> rules = cia.getHtStatus().getRuleResults();

    Assert.assertEquals("1 rule is created", 1, rules.size());

    for (CheckInResult rule : rules) {
      Assert.assertEquals("Rule type is symptom", CheckInResult.ResultType.symptom, rule.getType());
      Assert.assertEquals("Rule title is 'Moderate nausea'", "Moderate nausea", rule.getTitle());
      Assert.assertTrue("Note is empty", rule.getNote().isEmpty());
      Assert.assertEquals(
          "Rule triage reason type is 'symptom'", "symptom", rule.getTriageReasonType());
      Assert.assertEquals(
          "Rule triage severity is 'Moderate'", "moderate", rule.getTriageSeverity());
      Assert.assertEquals(
          "Rule triage symptom type is 'nausea'", "nausea", rule.getTriageSymptomType());
    }
  }

  @Test
  public void addProCtcaeSymptom_withVerySevereResponse() {
    CheckInAggregator cia = createCheckInAggregator(false, "nauseaSeverity", "4");
    cia.addProCtcaeSymptom(0, "2,3,4");
    Set<CheckInResult> rules = cia.getHtStatus().getRuleResults();

    Assert.assertEquals("1 rule is created", 1, rules.size());

    for (CheckInResult rule : rules) {
      Assert.assertEquals("Rule type is symptom", CheckInResult.ResultType.symptom, rule.getType());
      Assert.assertEquals("Rule title is 'Moderate nausea'", "Very severe nausea", rule.getTitle());
      Assert.assertTrue("Note is empty", rule.getNote().isEmpty());
      Assert.assertEquals(
          "Rule triage reason type is 'symptom'", "symptom", rule.getTriageReasonType());
      Assert.assertEquals(
          "Rule triage severity is 'Very Severe'", "severe", rule.getTriageSeverity());
      Assert.assertEquals(
          "Rule triage symptom type is 'nausea'", "nausea", rule.getTriageSymptomType());
    }
  }

  @Test
  public void addProCtcaeSymptom_withAlmostConstantFrequency() {
    CheckInAggregator cia = createCheckInAggregator(false, "constipationFrequency", "4");
    cia.addProCtcaeSymptom(0, "2,3,4");
    Set<CheckInResult> rules = cia.getHtStatus().getRuleResults();

    Assert.assertEquals("1 rule is created", 1, rules.size());

    for (CheckInResult rule : rules) {
      Assert.assertEquals("Rule type is valid", CheckInResult.ResultType.symptom, rule.getType());
      Assert.assertEquals(
          "Rule title is valid", "Constipation occurs almost constantly", rule.getTitle());
      Assert.assertTrue("Note is empty", rule.getNote().isEmpty());
      Assert.assertEquals(
          "Rule triage reason type is valid", "symptom", rule.getTriageReasonType());
      Assert.assertEquals("Rule triage severity is valid", "severe", rule.getTriageSeverity());
      Assert.assertEquals(
          "Rule triage symptom type is valid", "constipation", rule.getTriageSymptomType());
    }
  }

  @Test
  public void addProCtcaeSymptom_withMultipleTriggeringAnswers() {
    CheckInAggregator cia =
        createCheckInAggregator(
            false,
            "constipationFrequency",
            "4",
            "nauseaSeverity",
            "2",
            "painSeverity",
            "2",
            "painFrequency",
            "3",
            "painInterference",
            "4");
    cia.addProCtcaeSymptom(0, "2,3,4");
    Set<CheckInResult> rules = cia.getHtStatus().getRuleResults();

    Assert.assertEquals("5 rules are created", 5, rules.size());
  }

  @Test
  public void countAnswer() {}

  @Test
  public void givenMissedCheckins_shouldReturnConsecutiveCheckinsMissed() {
    List<CheckIn> checkIns = new ArrayList<>();
    checkIns.add(
        DefaultDroolsServiceTest.createCheckInsFromSurveyPayload(
            CheckInType.ORAL, LocalDate.now().minusDays(1L), null));
    checkIns.add(
        DefaultDroolsServiceTest.createCheckInsFromSurveyPayload(
            CheckInType.SYMPTOM, LocalDate.now().minusDays(1L), null));
    checkIns.add(
        DefaultDroolsServiceTest.createCheckInsFromSurveyPayload(
            CheckInType.ORAL, LocalDate.now().minusDays(3L), null));
    checkIns.add(
        DefaultDroolsServiceTest.createCheckInsFromSurveyPayload(
            CheckInType.SYMPTOM, LocalDate.now().minusDays(3L), null));
    checkIns.add(
        DefaultDroolsServiceTest.createCheckInsFromSurveyPayload(
            CheckInType.ORAL,
            LocalDate.now().minusDays(5L),
            DefaultDroolsServiceTest.createSurvey("medicationTaken", "true")));
    checkIns.add(
        DefaultDroolsServiceTest.createCheckInsFromSurveyPayload(
            CheckInType.ORAL, LocalDate.now().minusDays(8L), null));

    CheckInAggregator agg = createCheckInAggregator(checkIns);

    Assert.assertEquals(2, agg.countMissed());
  }

  @Test
  public void countSideAffects() {}

  @Test
  public void countSideAffects1() {}

  @Test
  public void participationPercent() {}

  @Test
  public void participationPercent1() {}

  @Test
  public void adherencePercent() {}

  @Test
  public void addResult() {}

  @Test
  public void addResult1() {}

  @Test
  public void addSymptom() {}
}
