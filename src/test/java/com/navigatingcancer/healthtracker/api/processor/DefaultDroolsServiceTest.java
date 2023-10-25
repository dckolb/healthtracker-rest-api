package com.navigatingcancer.healthtracker.api.processor;

import com.navigatingcancer.healthtracker.api.TestConfig;
import com.navigatingcancer.healthtracker.api.data.model.*;
import com.navigatingcancer.healthtracker.api.data.model.patientInfo.PatientInfo;
import com.navigatingcancer.healthtracker.api.data.model.survey.SurveyItemPayload;
import com.navigatingcancer.healthtracker.api.data.model.survey.SurveyPayload;
import com.navigatingcancer.healthtracker.api.processor.model.DroolsManager;
import com.navigatingcancer.healthtracker.api.processor.model.HealthTracker;
import com.navigatingcancer.healthtracker.api.processor.model.ProFormatManager;
import java.time.LocalDate;
import java.util.*;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(
    classes = {
      DroolsConfig.class,
      DefaultDroolsService.class,
      DroolsManager.class,
      ProFormatManager.class
    })
@Import(TestConfig.class)
public class DefaultDroolsServiceTest {

  @Autowired private DefaultDroolsService service;

  @Autowired private ProFormatManager proFormatManager;

  @Value(value = "${MN.clinicIds:291}")
  private Long[] MN_CLINIC_IDs;

  private Long mnClinicId;
  private SurveyPayload surveyPayload;

  @Before
  public void setup() {
    mnClinicId = MN_CLINIC_IDs[0];
  }

  public static SurveyItemPayload createSurvey(String... items) {
    if (items != null && (items.length % 2) != 0)
      throw new IllegalArgumentException("createSurvey needs even number of items.");

    SurveyItemPayload item = new SurveyItemPayload();
    item.setPayload(new HashMap<>());

    if (items == null) return item;

    for (int i = 0; i < items.length; i = i + 2) {
      item.getPayload().put(items[i], items[i + 1]);
    }
    return item;
  }

  private boolean htStatusContainsResultWithTargetCategoryFromRule(
      HealthTrackerStatus htStatus, HealthTrackerStatusCategory category, String ruleName) {
    return htStatus.getRuleNames().contains(ruleName) && htStatus.getCategory().equals(category);
  }

  private boolean htStatusContainsCheckInResult(
      HealthTrackerStatus htStatus, CheckInResult.ResultType type, String title) {
    return htStatusContainsCheckInResult(htStatus, type, title, null);
  }

  private boolean htStatusContainsCheckInResult(
      HealthTrackerStatus htStatus, CheckInResult.ResultType type, String title, String note) {
    for (CheckInResult checkInResult : htStatus.getRuleResults()) {
      if (type == checkInResult.getType()
          && title.equals(checkInResult.getTitle())
          && (note == null || checkInResult.getNote().contains(note))) {
        return true;
      }
    }

    return false;
  }

  public static CheckIn createCheckInsFromSurveyPayload(
      CheckInType type, LocalDate scheduledDate, SurveyItemPayload item) {
    CheckIn checkIn = new CheckIn();
    checkIn.setCheckInType(type);
    checkIn.setScheduleDate(scheduledDate);
    checkIn.setSurveyPayload(item);
    checkIn.setStatus(
        item != null && !item.getPayload().isEmpty()
            ? CheckInStatus.COMPLETED
            : CheckInStatus.MISSED);
    return checkIn;
  }

  public static List<CheckIn> createCheckInsFromSurveyPayload(SurveyItemPayload... items) {
    List<CheckIn> checkIns = new ArrayList<>();
    LocalDate scheduledDate = LocalDate.now();

    for (SurveyItemPayload item : items) {
      CheckIn checkIn = new CheckIn();
      checkIn.setCheckInType(CheckInType.ORAL);
      checkIn.setScheduleDate(scheduledDate);
      checkIn.setSurveyPayload(item);
      checkIn.setStatus(
          item != null && !item.getPayload().isEmpty()
              ? CheckInStatus.COMPLETED
              : CheckInStatus.MISSED);
      checkIns.add(checkIn);

      scheduledDate = scheduledDate.minusDays(1);
    }

    return checkIns;
  }

  private HealthTracker createHealthTrackerForMN(boolean isHighRisk, SurveyItemPayload... items) {
    Enrollment enrollment = new Enrollment();
    enrollment.setClinicId(mnClinicId);
    Set<TherapyType> types = new HashSet<>();
    types.add(TherapyType.IV);
    enrollment.setTherapyTypes(types);

    HealthTracker ht = createHealthTracker(enrollment, isHighRisk, items);

    Assert.assertTrue(proFormatManager.followsCtcaeStandard(ht.getEnrollment()));

    return ht;
  }

  private HealthTracker createHealthTracker(
      Enrollment enrollment, boolean isHighRisk, SurveyItemPayload... items) {
    if (enrollment == null) {
      enrollment = new Enrollment();
      enrollment.setMedication("Xtandi");
      enrollment.setClinicId(111L);

      Set<TherapyType> therapyTypes = new HashSet<>();
      therapyTypes.add(TherapyType.ORAL);
      enrollment.setTherapyTypes(therapyTypes);
      enrollment.setTxStartDate(LocalDate.now());
    }

    List<CheckIn> checkIns = createCheckInsFromSurveyPayload(items);

    PatientInfo pi = new PatientInfo();
    pi.setHighRisk(isHighRisk);

    HealthTrackerStatus healthTrackerStatus = new HealthTrackerStatus();
    healthTrackerStatus.setPatientInfo(pi);

    return new HealthTracker(
        enrollment, checkIns, healthTrackerStatus, proFormatManager, surveyPayload);
  }

  private Enrollment createOralEnrollment() {
    Enrollment enrollment = new Enrollment();
    enrollment.setMedication("Xtandi");
    enrollment.setClinicId(111L);

    Set<TherapyType> therapyTypes = new HashSet<>();
    therapyTypes.add(TherapyType.ORAL);
    enrollment.setTherapyTypes(therapyTypes);

    return enrollment;
  }

  private HealthTracker createHealthTracker(
      Enrollment enrollment, boolean isHighRisk, List<CheckIn> checkIns) {
    if (enrollment == null) {
      enrollment = new Enrollment();
      enrollment.setMedication("Xtandi");
      enrollment.setClinicId(111L);

      Set<TherapyType> therapyTypes = new HashSet<>();
      therapyTypes.add(TherapyType.ORAL);
      enrollment.setTherapyTypes(therapyTypes);
    }

    PatientInfo pi = new PatientInfo();
    pi.setHighRisk(isHighRisk);

    HealthTrackerStatus healthTrackerStatus = new HealthTrackerStatus();
    healthTrackerStatus.setPatientInfo(pi);

    return new HealthTracker(
        enrollment, checkIns, healthTrackerStatus, proFormatManager, surveyPayload);
  }

  @Test
  public void givenNoSymptoms_shouldReturnNoActionNeeded() {

    HealthTracker ht1 = createHealthTracker(null, false, createSurvey(null));

    HealthTrackerStatus status = service.process(ht1);

    // Default set of rules does not have a match for no data
    Assert.assertTrue("No rules should fire by default", status.getRuleNames().isEmpty());
    Assert.assertEquals(null, status.getCategory());
    Assert.assertTrue("No results produced by default", status.getRuleResults().isEmpty());
  }

  // MN related rules
  @Test
  public void givenNoSymptoms_shouldAutoCloseAndWatchMN() {

    // Only one MISSED checkin is created
    HealthTracker ht1 = createHealthTrackerForMN(false, createSurvey(null));

    HealthTrackerStatus status = service.process(ht1);

    Assert.assertTrue(
        "Should get default no action card",
        htStatusContainsResultWithTargetCategoryFromRule(
            status, HealthTrackerStatusCategory.NO_ACTION_NEEDED, "No Action Needed"));

    Assert.assertTrue(
        htStatusContainsCheckInResult(
            status, CheckInResult.ResultType.noAction, "No Action Needed."));
    Assert.assertFalse(status.getEndCurrentCycle()); // Do not auto-close if only missed checkins
  }

  @Test
  public void givenMildSymptoms_shouldReturnWatchCarefully() {
    HealthTracker ht1 = createHealthTrackerForMN(false, createSurvey("nauseaSeverity", "1"));

    HealthTrackerStatus status = service.process(ht1);

    Assert.assertTrue(
        "Should get watch carefully card since response is < 1",
        htStatusContainsResultWithTargetCategoryFromRule(
            status, HealthTrackerStatusCategory.WATCH_CAREFULLY, "Mild Side Effect"));

    Assert.assertTrue(
        htStatusContainsCheckInResult(status, CheckInResult.ResultType.symptom, "Mild nausea"));
    Assert.assertTrue(status.getEndCurrentCycle());
  }

  @Test
  public void givenModerateSymptoms_shouldReturnTriage() {
    HealthTracker ht1 = createHealthTrackerForMN(false, createSurvey("nauseaSeverity", "2"));

    HealthTrackerStatus status = service.process(ht1);

    Assert.assertTrue(
        "Should get triage card since response is equal to 2",
        htStatusContainsResultWithTargetCategoryFromRule(
            status,
            HealthTrackerStatusCategory.TRIAGE,
            "Moderate, severe or very severe response for pain, nausea and constipation"));

    Assert.assertTrue(
        htStatusContainsCheckInResult(status, CheckInResult.ResultType.symptom, "Moderate nausea"));
    Assert.assertFalse(status.getEndCurrentCycle());
  }

  @Test
  public void givenSevereSymptoms_shouldReturnTriage() {
    HealthTracker ht1 =
        createHealthTrackerForMN(
            false,
            createSurvey("nauseaSeverity", "2", "painSeverity", "4", "constipationSeverity", "1"));

    HealthTrackerStatus status = service.process(ht1);

    Assert.assertTrue(
        "Should get triage card since 1 of the responses is > 2",
        htStatusContainsResultWithTargetCategoryFromRule(
            status,
            HealthTrackerStatusCategory.TRIAGE,
            "Moderate, severe or very severe response for pain, nausea and constipation"));

    Assert.assertTrue(
        htStatusContainsCheckInResult(
            status, CheckInResult.ResultType.symptom, "Very severe pain"));
    Assert.assertFalse(status.getEndCurrentCycle());
  }

  @Test
  public void givenOneSevereSymptomForFrequency_shouldReturnTriage() {
    HealthTracker ht1 = createHealthTrackerForMN(false, createSurvey("painFrequency", "2"));

    HealthTrackerStatus status = service.process(ht1);

    Assert.assertTrue(
        "Should get triage card since 1 of the responses is >= 2",
        htStatusContainsResultWithTargetCategoryFromRule(
            status,
            HealthTrackerStatusCategory.TRIAGE,
            "Moderate, severe or very severe response for pain, nausea and constipation"));

    Assert.assertTrue(
        htStatusContainsCheckInResult(
            status, CheckInResult.ResultType.symptom, "Pain occurs occasionally"));
    Assert.assertFalse(status.getEndCurrentCycle());
  }

  // Non Mandatory Pro CTCAE question related tests
  @Test
  public void givenOneSevereSymptomForNonMandatoryQuestion_shouldReturnTriage() {
    HealthTracker ht1 = createHealthTrackerForMN(false, createSurvey("otherSeverity", "4"));

    HealthTrackerStatus status = service.process(ht1);

    Assert.assertTrue(
        "Should get triage card since non mandatory response is >= 4",
        htStatusContainsResultWithTargetCategoryFromRule(
            status,
            HealthTrackerStatusCategory.TRIAGE,
            "Severe or very severe response for non mandatory Pro Ctcae questions"));

    Assert.assertTrue(
        htStatusContainsCheckInResult(
            status, CheckInResult.ResultType.symptom, "Very severe other symptoms"));
    Assert.assertFalse(status.getEndCurrentCycle());
  }

  @Test
  public void givenModerateSymptomsForNonMandatory_shouldCreateWatchCarefully_insteadOfTriage() {

    HealthTracker ht1 = createHealthTrackerForMN(false, createSurvey("coughSeverity", "2"));

    HealthTrackerStatus status = service.process(ht1);

    Assert.assertTrue(
        "Should create WATCH CAREFULLY since severity == 2 and its a non mandatory question ",
        htStatusContainsResultWithTargetCategoryFromRule(
            status, HealthTrackerStatusCategory.TRIAGE, "Mild Side Effect"));
    Assert.assertTrue(
        "Should add Watch Carefully ruleName", status.getRuleNames().contains("Mild Side Effect"));
    Assert.assertTrue(
        "Should add Action Needed ruleName",
        status.getRuleNames().contains("Moderate, severe or very severe response for any symptom"));
    Assert.assertFalse(status.getEndCurrentCycle());
  }

  @Test
  public void givenMildSymptoms_shouldCreateWatchCarefully() {

    HealthTracker ht1 = createHealthTrackerForMN(false, createSurvey("coughSeverity", "1"));

    HealthTrackerStatus status = service.process(ht1);

    Assert.assertTrue(
        "Should create WATCH CAREFULLY since severity == 1 in proctcae",
        htStatusContainsResultWithTargetCategoryFromRule(
            status, HealthTrackerStatusCategory.WATCH_CAREFULLY, "Mild Side Effect"));
    Assert.assertTrue(
        "Should add Watch Carefully ruleName", status.getRuleNames().contains("Mild Side Effect"));
    Assert.assertTrue(
        "Should add No Action ruleName", status.getRuleNames().contains("No Action Needed"));
    Assert.assertTrue(status.getEndCurrentCycle());
  }

  // No action rules test
  @Test
  public void givenNoSymptoms_shouldCreateNoTicket() {

    HealthTracker ht1 = createHealthTrackerForMN(false, createSurvey("coughSeverity", "0"));

    HealthTrackerStatus status = service.process(ht1);

    Assert.assertTrue(
        "Should create No Action Needed",
        htStatusContainsResultWithTargetCategoryFromRule(
            status, HealthTrackerStatusCategory.WATCH_CAREFULLY, "No Action Needed"));
    Assert.assertTrue(
        "Should add No Action ruleName", status.getRuleNames().contains("No Action Needed"));
    Assert.assertTrue(status.getEndCurrentCycle());
  }

  @Test
  public void givenHighFrequencyNonMandatorySymptom_shouldNotEscalateToTriage() {

    // mild severity but severe frequency
    HealthTracker ht1 =
        createHealthTrackerForMN(false, createSurvey("coughSeverity", "1", "coughFrequency", "4"));

    HealthTrackerStatus status = service.process(ht1);

    Assert.assertTrue(
        "Should create Watch Carefully",
        htStatusContainsResultWithTargetCategoryFromRule(
            status, HealthTrackerStatusCategory.TRIAGE, "Mild Side Effect"));
    Assert.assertTrue(
        "Should add Action Needed ruleName",
        status.getRuleNames().contains("Moderate, severe or very severe response for any symptom"));
    Assert.assertFalse(status.getEndCurrentCycle());
  }

  @Test
  public void givenHighInterferenceNonMandatorySymptom_shouldNotEscalateToTriage() {

    // mild severity but severe frequency
    HealthTracker ht1 =
        createHealthTrackerForMN(
            false, createSurvey("coughSeverity", "1", "coughInterference", "4"));

    HealthTrackerStatus status = service.process(ht1);

    Assert.assertTrue(
        "Should create Watch Carefully",
        htStatusContainsResultWithTargetCategoryFromRule(
            status, HealthTrackerStatusCategory.TRIAGE, "Mild Side Effect"));
    Assert.assertTrue(
        "Should add Action Needed ruleName",
        status.getRuleNames().contains("Moderate, severe or very severe response for any symptom"));
    Assert.assertFalse(status.getEndCurrentCycle());
  }

  // 											NON PRO CTCAE tests
  // 											NON PRO CTCAE tests
  // 											NON PRO CTCAE tests
  // Triage rules test

  @Test
  public void givenMixedMildAndSevereSymptoms_shouldReturnTriage() {
    HealthTracker ht1 =
        createHealthTracker(null, false, createSurvey("coughSeverity", "3", "nauseaSeverity", "1"));
    HealthTrackerStatus status = service.process(ht1);

    Assert.assertEquals(HealthTrackerStatusCategory.TRIAGE, status.getCategory());
    Assert.assertTrue(
        "should get triage response",
        htStatusContainsResultWithTargetCategoryFromRule(
            status,
            HealthTrackerStatusCategory.TRIAGE,
            "Very Severe, Severe, or Moderate Side Effect"));

    Assert.assertTrue(
        htStatusContainsCheckInResult(status, CheckInResult.ResultType.symptom, "Severe cough"));
  }

  @Test
  public void givenMixedMildAndModerateSymptoms_shouldReturnTriage() {
    HealthTracker ht1 =
        createHealthTracker(
            null,
            false,
            createSurvey(
                "coughSeverity",
                "2",
                "coughFrequency",
                "2",
                "nauseaSeverity",
                "1",
                "nauseaFrequency",
                "1"));

    HealthTrackerStatus status = service.process(ht1);

    Assert.assertEquals(HealthTrackerStatusCategory.TRIAGE, status.getCategory());
    Assert.assertTrue(
        "should get triage response",
        htStatusContainsResultWithTargetCategoryFromRule(
            status,
            HealthTrackerStatusCategory.TRIAGE,
            "Very Severe, Severe, or Moderate Side Effect"));

    Assert.assertTrue(
        htStatusContainsCheckInResult(status, CheckInResult.ResultType.symptom, "Moderate cough"));
  }

  @Test
  public void givenModerateCough_shouldReturnTriage() {

    HealthTracker ht1 = createHealthTracker(null, false, createSurvey("coughSeverity", "2"));

    HealthTrackerStatus status = service.process(ht1);

    Assert.assertTrue(
        "should get triage response",
        htStatusContainsResultWithTargetCategoryFromRule(
            status,
            HealthTrackerStatusCategory.TRIAGE,
            "Very Severe, Severe, or Moderate Side Effect"));

    Assert.assertTrue(
        htStatusContainsCheckInResult(status, CheckInResult.ResultType.symptom, "Moderate cough"));
  }

  @Test
  public void givenModerateOther_shouldReturnTriage() {

    HealthTracker ht1 = createHealthTracker(null, false, createSurvey("otherSeverity", "2"));

    HealthTrackerStatus status = service.process(ht1);

    Assert.assertTrue(
        "should get triage response",
        htStatusContainsResultWithTargetCategoryFromRule(
            status,
            HealthTrackerStatusCategory.TRIAGE,
            "Very Severe, Severe, or Moderate Side Effect"));

    Assert.assertTrue(
        htStatusContainsCheckInResult(
            status, CheckInResult.ResultType.symptom, "Moderate other symptoms"));
  }

  @Test
  public void givenSeverePain_shouldReturnTriage() {

    HealthTracker ht1 = createHealthTracker(null, false, createSurvey("painSeverity", "5"));

    HealthTrackerStatus status = service.process(ht1);

    Assert.assertTrue(
        "should get triage response",
        htStatusContainsResultWithTargetCategoryFromRule(
            status, HealthTrackerStatusCategory.TRIAGE, "Pain Severity of 4, 5, 6, 7, 8, 9, 10"));

    Assert.assertTrue(
        htStatusContainsCheckInResult(status, CheckInResult.ResultType.symptom, "Moderate pain"));
  }

  @Test
  public void givenDidNotTakeMedication_outOfMedication_shouldReturnTriage() {
    HealthTracker ht1 =
        createHealthTracker(
            null,
            false,
            createSurvey("medicationTaken", "no", "medicationSkipReason", "outOfMedication"));

    HealthTrackerStatus status = service.process(ht1);

    Assert.assertTrue(
        "should get action needed response",
        htStatusContainsResultWithTargetCategoryFromRule(
            status, HealthTrackerStatusCategory.TRIAGE, "Out of Medication"));

    Assert.assertTrue(
        htStatusContainsCheckInResult(
            status, CheckInResult.ResultType.adherence, "Out of Medication"));
  }

  @Test
  public void givenDidNotStartMedication_outOfMedication_shouldReturnTriage() {
    HealthTracker ht1 =
        createHealthTracker(
            null,
            false,
            createSurvey("medicationStarted", "no", "medicationSkipReason", "outOfMedication"));

    HealthTrackerStatus status = service.process(ht1);

    Assert.assertTrue(
        "should get action needed response",
        htStatusContainsResultWithTargetCategoryFromRule(
            status, HealthTrackerStatusCategory.TRIAGE, "Out of Medication"));

    Assert.assertTrue(
        htStatusContainsCheckInResult(
            status, CheckInResult.ResultType.adherence, "Out of Medication"));
  }

  // Watch carefully test

  @Test
  public void givenMildCough_shouldReturnWatchCarefully() {

    HealthTracker ht1 = createHealthTracker(null, false, createSurvey("coughSeverity", "1"));

    HealthTrackerStatus status = service.process(ht1);

    Assert.assertTrue(
        "should get watch carefully response",
        htStatusContainsResultWithTargetCategoryFromRule(
            status, HealthTrackerStatusCategory.WATCH_CAREFULLY, "Mild Side Effect"));

    Assert.assertTrue(
        htStatusContainsCheckInResult(status, CheckInResult.ResultType.symptom, "Mild cough"));
  }

  @Test
  public void givenHignRiskAndMissed_shouldReturnWatchCarefully() {

    HealthTracker ht1 = createHealthTracker(null, true, createSurvey());

    HealthTrackerStatus status = service.process(ht1);

    Assert.assertTrue(
        "should get watch carefully response",
        htStatusContainsResultWithTargetCategoryFromRule(
            status,
            HealthTrackerStatusCategory.WATCH_CAREFULLY,
            "High Risk and one missed check-in"));

    Assert.assertTrue(
        htStatusContainsCheckInResult(
            status, CheckInResult.ResultType.participation, "1 missed check-in"));
  }

  @Test
  public void givenHignRiskAndLowParticipation_shouldReturnActionNeeded() {

    List<SurveyItemPayload> payloads = new ArrayList<>();
    for (int i = 1; i <= 10; i++) {
      SurveyItemPayload payload = i < 8 ? createSurvey("medicationTaken", "no") : null;
      payloads.add(payload);
    }

    HealthTracker ht1 = createHealthTracker(null, true, payloads.toArray(new SurveyItemPayload[0]));

    HealthTrackerStatus status = service.process(ht1);

    Assert.assertTrue(
        "should get action needed response",
        htStatusContainsResultWithTargetCategoryFromRule(
            status,
            HealthTrackerStatusCategory.ACTION_NEEDED,
            "High Risk and low participation (75% or less over  14 last days)"));
    Assert.assertTrue(
        htStatusContainsCheckInResult(
            status, CheckInResult.ResultType.participationRate, "Low participation"));
  }

  @Test
  public void givenNonHignRiskAndAdherenceNo_shouldReturnWatchCarefully() {

    HealthTracker ht1 =
        createHealthTracker(
            null,
            false,
            createSurvey("medicationTaken", "no"),
            createSurvey("medicationTaken", "no"),
            createSurvey("medicationTaken", "yes"),
            createSurvey("medicationTaken", "yes"),
            createSurvey("medicationTaken", "yes"));

    HealthTrackerStatus status = service.process(ht1);

    Assert.assertTrue(
        "should get watch carefully response",
        htStatusContainsResultWithTargetCategoryFromRule(
            status,
            HealthTrackerStatusCategory.WATCH_CAREFULLY,
            "Non-High Risk patients that have an Oral Adherence of NO 2 of the last 5 days"));

    Assert.assertTrue(
        htStatusContainsCheckInResult(
            status, CheckInResult.ResultType.adherence, "Missed 2 out of 5 recent doses"));
  }

  // Action needed is false if scheduleStartDateChanged is false
  @Test
  public void givenPatientReportedDifferentTxStartDate_txStartDate_shouldGetActionNeeded()
      throws Exception {
    Enrollment enrollment = createOralEnrollment();
    enrollment.setTxStartDate(LocalDate.now());

    List<CheckIn> checkIns = new ArrayList<>();
    CheckIn oralCheckIn =
        DefaultDroolsServiceTest.createCheckInsFromSurveyPayload(
            CheckInType.ORAL, LocalDate.now(), null);
    oralCheckIn.setTxStartDate(enrollment.getTxStartDate());

    oralCheckIn.setPatientReportedTxStartDate(LocalDate.now().minusDays(-1));
    checkIns.add(oralCheckIn);

    HealthTracker ht1 = createHealthTracker(enrollment, false, checkIns);
    HealthTrackerStatus status = service.process(ht1);

    Assert.assertTrue(
        "should get action needed response",
        htStatusContainsResultWithTargetCategoryFromRule(
            status,
            HealthTrackerStatusCategory.ACTION_NEEDED,
            "Patient reports a different Start Date"));
  }

  // Action needed is false if scheduleStartDateChanged is false
  @Test
  public void givenPatientReportedDifferentTxStartDate_reminderStartDate_shouldGetActionNeeded()
      throws Exception {
    Enrollment enrollment = createOralEnrollment();
    enrollment.setReminderStartDate(LocalDate.now());
    enrollment.setTxStartDate(null);

    List<CheckIn> checkIns = new ArrayList<>();
    CheckIn oralCheckIn =
        DefaultDroolsServiceTest.createCheckInsFromSurveyPayload(
            CheckInType.ORAL, LocalDate.now(), null);

    oralCheckIn.setPatientReportedTxStartDate(LocalDate.now().minusDays(-1));
    oralCheckIn.setEnrollmentReminderStartDate(enrollment.getReminderStartDate());
    checkIns.add(oralCheckIn);

    HealthTracker ht1 = createHealthTracker(enrollment, false, checkIns);
    HealthTrackerStatus status = service.process(ht1);

    Assert.assertTrue(
        "should get action needed response",
        htStatusContainsResultWithTargetCategoryFromRule(
            status,
            HealthTrackerStatusCategory.ACTION_NEEDED,
            "Patient reports a different Start Date"));
  }

  @Test
  public void givenHighRiskAndNoCheckinsIn2Days_shouldReturnActionNeeded() {
    List<CheckIn> checkIns = new ArrayList<>();
    checkIns.add(
        DefaultDroolsServiceTest.createCheckInsFromSurveyPayload(
            CheckInType.SYMPTOM, LocalDate.now().minusDays(1L), null));
    checkIns.add(
        DefaultDroolsServiceTest.createCheckInsFromSurveyPayload(
            CheckInType.SYMPTOM, LocalDate.now().minusDays(5L), null));
    HealthTracker ht1 = createHealthTracker(null, true, checkIns);

    HealthTrackerStatus status = service.process(ht1);

    Assert.assertTrue(
        "should get action needed response",
        htStatusContainsResultWithTargetCategoryFromRule(
            status,
            HealthTrackerStatusCategory.ACTION_NEEDED,
            "High risk and no check ins for 2 consecutive checkins"));
    Assert.assertTrue(
        htStatusContainsCheckInResult(
            status, CheckInResult.ResultType.participationRate, "Low participation"));
  }

  @Test
  public void givenHighRiskAndAdherenceNo_shouldReturnActionNeeded() {

    HealthTracker ht1 = createHealthTracker(null, true, createSurvey("medicationTaken", "no"));

    HealthTrackerStatus status = service.process(ht1);

    Assert.assertTrue(
        "should get action needed response",
        htStatusContainsResultWithTargetCategoryFromRule(
            status,
            HealthTrackerStatusCategory.ACTION_NEEDED,
            "High risk and Oral Adherence is NO 1 time"));

    Assert.assertTrue(
        htStatusContainsCheckInResult(
            status, CheckInResult.ResultType.adherence, "No, I didn't take my Xtandi"));
  }

  @Test
  public void givenNonHighRiskAndMissed3CheckinsInRow_shouldReturnActionNeeded() {
    // DRM 2019-10-7  HT-842
    // check is now date sensitive and createSurvey() creates them all with same date
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
            CheckInType.SYMPTOM, LocalDate.now().minusDays(5L), null));
    HealthTracker ht1 = createHealthTracker(null, false, checkIns);

    HealthTrackerStatus status = service.process(ht1);

    Assert.assertTrue(
        "should get Action Needed response",
        htStatusContainsResultWithTargetCategoryFromRule(
            status,
            HealthTrackerStatusCategory.ACTION_NEEDED,
            "Non-high risk and No check in for 3 consecutive checkins"));

    Assert.assertTrue(
        htStatusContainsCheckInResult(
            status, CheckInResult.ResultType.participationRate, "Low participation"));
  }

  @Test
  public void givenNonHighRiskAndAdherenceNo3of5_shouldReturnActionNeeded() {

    HealthTracker ht1 =
        createHealthTracker(
            null,
            false,
            createSurvey("medicationTaken", "no"),
            createSurvey("medicationTaken", "no"),
            createSurvey("medicationTaken", "yes"),
            createSurvey("medicationTaken", "yes"),
            createSurvey("medicationTaken", "no"));

    HealthTrackerStatus status = service.process(ht1);

    Assert.assertTrue(
        "should get triage response",
        htStatusContainsResultWithTargetCategoryFromRule(
            status,
            HealthTrackerStatusCategory.ACTION_NEEDED,
            "Non-high risk and Oral Adherence is NO 3 out of the last 5 days"));

    Assert.assertTrue(
        htStatusContainsCheckInResult(
            status, CheckInResult.ResultType.adherence, "Missed 3 out of 5 recent doses"));
  }

  @Test
  public void givenNonHighRiskAndLowParticipation_shouldReturnWatchCarefully() {

    List<SurveyItemPayload> payloads = new ArrayList<>();
    for (int i = 1; i <= 10; i++) {
      SurveyItemPayload payload = i < 6 ? createSurvey("medicationTaken", "yes") : null;
      payloads.add(payload);
    }

    HealthTracker ht1 =
        createHealthTracker(null, false, payloads.toArray(new SurveyItemPayload[0]));

    HealthTrackerStatus status = service.process(ht1);

    Assert.assertTrue(
        "should get watch carefully response",
        htStatusContainsResultWithTargetCategoryFromRule(
            status,
            HealthTrackerStatusCategory.WATCH_CAREFULLY,
            "Non-high risk and Low Participation (50% or less in the past 14 days)"));

    Assert.assertTrue(
        htStatusContainsCheckInResult(
            status, CheckInResult.ResultType.participationRate, "Low participation"));
  }

  @Test
  public void givenModeratePain_shouldNotReturnTriage() {

    HealthTracker ht1 = createHealthTracker(null, false, createSurvey("painSeverity", "2"));

    HealthTrackerStatus status = service.process(ht1);

    Assert.assertFalse(status.getCategory() == HealthTrackerStatusCategory.TRIAGE);
  }

  @Test
  public void testNote() {
    HealthTracker ht1 =
        createHealthTracker(
            null, false, createSurvey("coughSeverity", "3", "coughInterference", "3"));
    HealthTrackerStatus status = service.process(ht1);

    Assert.assertTrue(
        htStatusContainsCheckInResult(
            status,
            CheckInResult.ResultType.symptom,
            "Severe cough",
            "It interferes with my daily activities quite a bit."));
  }
}
