package com.navigatingcancer.healthtracker.api.data.repo;

import com.navigatingcancer.healthtracker.api.TestConfig;
import com.navigatingcancer.healthtracker.api.data.model.HealthTrackerStatus;
import com.navigatingcancer.healthtracker.api.data.model.patientInfo.PatientInfo;
import com.navigatingcancer.healthtracker.api.data.model.survey.SurveyItemPayload;
import com.navigatingcancer.healthtracker.api.data.model.survey.SurveyPayload;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
@Import(TestConfig.class)
public class HealthTrackerStatusRepositoryTest {

  @Autowired private HealthTrackerStatusRepository repository;
  private String testId = "111111111111111111111111";

  @Before
  public void setup() {
    repository.deleteAll();

    HealthTrackerStatus testStatus = new HealthTrackerStatus();
    SurveyItemPayload item = new SurveyItemPayload();
    item.setPayload(Map.of("foo", "bar"));
    SurveyPayload payload = new SurveyPayload();
    payload.setSymptoms(List.of(item));
    testStatus.setSurveyPayload(payload);
    testStatus.setId(testId);

    repository.save(testStatus);
  }

  @Test
  public void findStatuses_withFilters() {
    Long clinicId = 12345L;
    Long patientId1 = 1L;
    Long patientId2 = 2L;

    List<Long> clinicIds = new ArrayList<>(Arrays.asList(clinicId));
    HealthTrackerStatus htStatus = new HealthTrackerStatus();
    htStatus.setClinicId(clinicId);
    PatientInfo pi = new PatientInfo();
    pi.setId(patientId1);
    htStatus.setPatientInfo(pi);

    HealthTrackerStatus htStatus2 = new HealthTrackerStatus();
    htStatus2.setClinicId(clinicId);
    PatientInfo pi2 = new PatientInfo();
    pi2.setId(patientId2);
    htStatus2.setPatientInfo(pi2);

    repository.save(htStatus);
    repository.save(htStatus2);

    List<Long> patientIdsWith1 = new ArrayList<>(Arrays.asList(patientId1));
    List<HealthTrackerStatus> filteredResults =
        repository.findStatuses(clinicIds, null, patientIdsWith1);

    // filter for PatientId 1
    Assert.assertEquals("Finds status for patientId 1", 1, filteredResults.size());
    PatientInfo resultPInfo = filteredResults.get(0).getPatientInfo();
    Assert.assertEquals("Filters for patientId 1 correctly", patientId1, resultPInfo.getId());

    // filter for PatientIds 1 and 2
    List<Long> allPatients = new ArrayList<>(Arrays.asList(patientId1, patientId2));
    List<HealthTrackerStatus> allResults = repository.findStatuses(clinicIds, null, allPatients);
    Assert.assertEquals("Finds status for patientId 1 and 2", 2, allPatients.size());

    // pass null for PatientIds
    List<HealthTrackerStatus> noPatientIdFilterResults =
        repository.findStatuses(clinicIds, null, null);
    Assert.assertEquals("Finds status for patientId 1 and 2", 2, noPatientIdFilterResults.size());
  }

  @Test
  public void givenOneClinicId_shouldOnlyReturnStatusForThatClinic() {
    Long clinicId = 1345L;
    Long patientId1 = 1L;
    Long clinicId2 = 4444l;
    Long patientId2 = 2L;

    List<Long> clinicIds = new ArrayList<>(Arrays.asList(clinicId));
    HealthTrackerStatus htStatus = new HealthTrackerStatus();
    htStatus.setClinicId(clinicId);
    PatientInfo pi = new PatientInfo();
    pi.setId(patientId1);
    htStatus.setPatientInfo(pi);

    HealthTrackerStatus htStatus2 = new HealthTrackerStatus();
    htStatus2.setClinicId(clinicId2);
    PatientInfo pi2 = new PatientInfo();
    pi2.setId(patientId2);
    htStatus2.setPatientInfo(pi2);

    repository.save(htStatus);
    repository.save(htStatus2);

    List<Long> patientIdsWith1 = new ArrayList<>(Arrays.asList(patientId1));
    List<HealthTrackerStatus> filteredResults =
        repository.findStatuses(clinicIds, null, patientIdsWith1);

    // filter for PatientId 1
    Assert.assertEquals("Finds status for patientId 1", 1, filteredResults.size());
    PatientInfo resultPInfo = filteredResults.get(0).getPatientInfo();
    Assert.assertEquals("Filters for patientId 1 correctly", patientId1, resultPInfo.getId());

    // filter for PatientIds 1 and 2
    List<Long> allPatients = new ArrayList<>(Arrays.asList(patientId1, patientId2));
    List<HealthTrackerStatus> allResults = repository.findStatuses(clinicIds, null, allPatients);
    Assert.assertEquals("Finds status for patientId 1 but not 2", 1, allResults.size());
    Assert.assertEquals(
        "result should be patient 1", patientId1, allResults.get(0).getPatientInfo().getId());

    // pass null for PatientIds

    List<HealthTrackerStatus> noPatientIdFilterResults =
        repository.findStatuses(Arrays.asList(clinicId2), null, null);
    Assert.assertEquals(
        "Finds status for patientId 2 but not 1", 1, noPatientIdFilterResults.size());
    Assert.assertEquals(
        "result should be patient 2",
        patientId2,
        noPatientIdFilterResults.get(0).getPatientInfo().getId());
  }

  @Test
  public void givenStatus_shouldFindByIds() {
    Long clinicId = 1345L;
    Long patientId1 = 16L;
    Long patientId2 = 26L;
    Long patientId3 = 36L;

    HealthTrackerStatus htStatus = new HealthTrackerStatus();
    htStatus.setClinicId(clinicId);
    PatientInfo pi = new PatientInfo();
    pi.setId(patientId1);
    htStatus.setPatientInfo(pi);

    HealthTrackerStatus htStatus2 = new HealthTrackerStatus();
    htStatus2.setClinicId(clinicId);
    PatientInfo pi2 = new PatientInfo();
    pi2.setId(patientId2);
    htStatus2.setPatientInfo(pi2);

    HealthTrackerStatus htStatus3 = new HealthTrackerStatus();
    htStatus3.setClinicId(clinicId);
    PatientInfo pi3 = new PatientInfo();
    pi3.setId(patientId3);
    htStatus3.setPatientInfo(pi3);

    HealthTrackerStatus htStatusPersisted = repository.save(htStatus);
    HealthTrackerStatus htStatusPersisted2 = repository.save(htStatus2);
    HealthTrackerStatus htStatusPersisted3 = repository.save(htStatus3);

    List<String> ids =
        new ArrayList<>(Arrays.asList(htStatusPersisted.getId(), htStatusPersisted3.getId()));
    List<HealthTrackerStatus> filteredResults = repository.findByIds(clinicId, ids);

    Assert.assertEquals("Finds status for clinic 1", 2, filteredResults.size());
    for (HealthTrackerStatus status : filteredResults) {
      Assert.assertEquals(clinicId, status.getClinicId());
      Assert.assertTrue(ids.contains(status.getId()));
    }
    Assert.assertFalse(
        "should not contain status with other id", ids.contains(htStatusPersisted2.getId()));
  }

  @Test
  public void findAndReplaceStatus_throwsOnInvalidArg() {
    Assert.assertThrows(
        IllegalArgumentException.class, () -> repository.findAndReplaceStatus(null));
  }

  @Test
  public void findAndReplaceStatus_updates() {
    HealthTrackerStatus status = new HealthTrackerStatus();
    SurveyItemPayload item = new SurveyItemPayload();
    Map<String, Object> payloadMap = Map.of("biz", "baz");
    item.setPayload(payloadMap);
    SurveyPayload payload = new SurveyPayload();
    payload.setOral(List.of(item));
    status.setSurveyPayload(payload);
    status.setId(testId);

    var currentCount = repository.count();
    repository.findAndReplaceStatus(status);

    Assert.assertEquals(currentCount, repository.count());

    Optional<HealthTrackerStatus> statusResult = repository.findById(testId);

    Assert.assertTrue(statusResult.isPresent());
    Assert.assertEquals(
        payloadMap, statusResult.get().getSurveyPayload().getOral().get(0).getPayload());
    Assert.assertNull(statusResult.get().getSurveyPayload().getSymptoms());
  }
}
