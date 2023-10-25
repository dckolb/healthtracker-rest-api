package com.navigatingcancer.healthtracker.api.data.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import com.navigatingcancer.healthtracker.api.TestConfig;
import com.navigatingcancer.healthtracker.api.data.client.ConfigServiceClient;
import com.navigatingcancer.healthtracker.api.data.model.CheckInType;
import com.navigatingcancer.healthtracker.api.data.model.Enrollment;
import com.navigatingcancer.healthtracker.api.data.model.ProgramType;
import com.navigatingcancer.healthtracker.api.data.model.TherapyType;
import com.navigatingcancer.healthtracker.api.data.model.surveyConfig.ClinicConfig;
import com.navigatingcancer.healthtracker.api.data.model.surveyConfig.ProgramConfig;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
@Import(TestConfig.class)
public class SurveyConfigServiceImplTest {
  private final Long HT_CLINIC_ID = 4L;
  private final Long PRO_CTCAE_CLINIC_ID = 12L;

  @MockBean ConfigServiceClient configServiceClient;

  @Autowired SurveyConfigServiceImpl surveyConfigService;

  private ProgramConfig proCtcaeProgram;
  private ProgramConfig htProgram;
  private ClinicConfig htClinicConfig;
  private ClinicConfig proCtcaeClinicConfig;

  @Before
  public void setup() {

    var htOralDef = new ProgramConfig.SurveyDef();
    htOralDef.setType(CheckInType.ORAL.name());
    htOralDef.setSurveyIds(
        Map.of(
            ProgramConfig.PATIENT_COLLECT, "ht-oral-px",
            ProgramConfig.CLINIC_COLLECT, "ht-oral-cx"));

    var htSymptomDef = new ProgramConfig.SurveyDef();
    htSymptomDef.setType(CheckInType.SYMPTOM.name());
    htSymptomDef.setSurveyIds(
        Map.of(
            ProgramConfig.PATIENT_COLLECT, "ht-symptom-px",
            ProgramConfig.CLINIC_COLLECT, "ht-symptom-cx"));

    var proCtcaeSymptomDef = new ProgramConfig.SurveyDef();
    proCtcaeSymptomDef.setType(CheckInType.SYMPTOM.name());
    proCtcaeSymptomDef.setSurveyIds(
        Map.of(
            ProgramConfig.PATIENT_COLLECT, "proctcae-px",
            ProgramConfig.CLINIC_COLLECT, "proctcae-cx"));

    proCtcaeProgram = new ProgramConfig();
    proCtcaeProgram.setId("proctcae-id");
    proCtcaeProgram.setType(ProgramType.PRO_CTCAE.getProgramName());
    proCtcaeProgram.setSurveys(List.of(proCtcaeSymptomDef));

    htProgram = new ProgramConfig();
    htProgram.setId(ProgramConfig.HEALTH_TRACKER_PROGRAM_ID);
    htProgram.setType(ProgramType.HEALTH_TRACKER.getProgramName());
    htProgram.setSurveys(List.of(htSymptomDef, htOralDef));

    htClinicConfig = new ClinicConfig();
    htClinicConfig.setClinicId(HT_CLINIC_ID);
    htClinicConfig.setId("1");
    htClinicConfig.setPrograms(
        Map.of(ProgramType.HEALTH_TRACKER.getProgramName(), proCtcaeProgram.getId()));

    proCtcaeClinicConfig = new ClinicConfig();
    proCtcaeClinicConfig.setClinicId(PRO_CTCAE_CLINIC_ID);
    proCtcaeClinicConfig.setId("2");
    proCtcaeClinicConfig.setPrograms(
        Map.of(ProgramType.PRO_CTCAE.getProgramName(), proCtcaeProgram.getId()));

    when(configServiceClient.getClinicConfig(PRO_CTCAE_CLINIC_ID)).thenReturn(proCtcaeClinicConfig);
    when(configServiceClient.getClinicConfig(HT_CLINIC_ID)).thenReturn(htClinicConfig);
    when(configServiceClient.getProgramConfig(proCtcaeProgram.getId())).thenReturn(proCtcaeProgram);
    when(configServiceClient.getProgramConfig(htProgram.getId())).thenReturn(htProgram);
  }

  private Enrollment testEnrollment(Long clinicId, boolean manualCollect) {
    return testEnrollment(clinicId, manualCollect, Set.of());
  }

  private Enrollment testEnrollment(
      Long clinicId, boolean manualCollect, Set<TherapyType> therapyTypes) {
    var enr = new Enrollment();
    enr.setClinicId(clinicId);
    enr.setManualCollect(manualCollect);
    enr.setTherapyTypes(therapyTypes);
    return enr;
  }

  @Test
  public void getSurveyIdForCheckInType_ht_oral_patientCollect() {
    var enr = testEnrollment(HT_CLINIC_ID, false);
    var surveyId = surveyConfigService.getSurveyIdForCheckInType(enr, CheckInType.ORAL);

    assertTrue(surveyId.isPresent());
    assertEquals("ht-oral-px", surveyId.get());
  }

  @Test
  public void getSurveyIdForCheckInType_ht_symptom_patientCollect() {
    var enr = testEnrollment(HT_CLINIC_ID, false);

    var surveyId = surveyConfigService.getSurveyIdForCheckInType(enr, CheckInType.SYMPTOM);

    assertTrue(surveyId.isPresent());
    assertEquals("ht-symptom-px", surveyId.get());
  }

  @Test
  public void getSurveyIdForCheckInType_ht_oral_clinicCollect() {
    var enr = testEnrollment(HT_CLINIC_ID, true);
    var surveyId = surveyConfigService.getSurveyIdForCheckInType(enr, CheckInType.ORAL);

    assertTrue(surveyId.isPresent());
    assertEquals("ht-oral-cx", surveyId.get());
  }

  @Test
  public void getSurveyIdForCheckInType_ht_symptom_clinicCollect() {
    var enr = testEnrollment(HT_CLINIC_ID, true);

    var surveyId = surveyConfigService.getSurveyIdForCheckInType(enr, CheckInType.SYMPTOM);

    assertTrue(surveyId.isPresent());
    assertEquals("ht-symptom-cx", surveyId.get());
  }

  @Test
  public void getSurveyIdForCheckInType_proctcae_oral_patientCollect() {
    var enr = testEnrollment(PRO_CTCAE_CLINIC_ID, false);
    var surveyId = surveyConfigService.getSurveyIdForCheckInType(enr, CheckInType.ORAL);

    assertTrue(surveyId.isPresent());
    assertEquals("oral survey should resolve to HT default", "ht-oral-px", surveyId.get());
  }

  @Test
  public void getSurveyIdForCheckInType_proctcae_oral_clinicCollect() {
    var enr = testEnrollment(PRO_CTCAE_CLINIC_ID, true);
    var surveyId = surveyConfigService.getSurveyIdForCheckInType(enr, CheckInType.ORAL);

    assertTrue(surveyId.isPresent());
    assertEquals("oral survey should resolve to HT default", "ht-oral-cx", surveyId.get());
  }

  @Test
  public void getSurveyIdForCheckInType_proctcae_symptom_patientCollect() {
    var enr = testEnrollment(PRO_CTCAE_CLINIC_ID, false, Set.of(TherapyType.IV));

    var surveyId = surveyConfigService.getSurveyIdForCheckInType(enr, CheckInType.SYMPTOM);

    assertTrue(surveyId.isPresent());
    assertEquals("proctcae-px", surveyId.get());
  }

  @Test
  public void getSurveyIdForCheckInType_proctcae_symptom_clinicCollect() {
    var enr = testEnrollment(PRO_CTCAE_CLINIC_ID, true, Set.of(TherapyType.IV));

    var surveyId = surveyConfigService.getSurveyIdForCheckInType(enr, CheckInType.SYMPTOM);

    assertTrue(surveyId.isPresent());
    assertEquals("proctcae-cx", surveyId.get());
  }
}
