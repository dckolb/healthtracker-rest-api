package com.navigatingcancer.healthtracker.api.data.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.navigatingcancer.healthtracker.api.TestConfig;
import com.navigatingcancer.healthtracker.api.data.model.CheckInData;
import com.navigatingcancer.healthtracker.api.data.model.CheckInType;
import com.navigatingcancer.healthtracker.api.data.model.Enrollment;
import com.navigatingcancer.healthtracker.api.data.model.HealthTrackerStatus;
import com.navigatingcancer.healthtracker.api.data.model.patientInfo.PatientInfo;
import com.navigatingcancer.healthtracker.api.data.repo.CheckInRepository;
import com.navigatingcancer.healthtracker.api.data.repo.EnrollmentRepository;
import com.navigatingcancer.healthtracker.api.data.repo.HealthTrackerStatusRepository;
import com.navigatingcancer.healthtracker.api.rest.representation.HealthTrackerStatusResponse;
import java.util.*;
import org.junit.Assert;
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
public class StatusServiceTest {

  @MockBean private HealthTrackerStatusRepository statusRepository;

  @MockBean private CheckInRepository checkInRepository;

  @MockBean private CheckInService checkInService;

  @MockBean private EnrollmentRepository enrollmentRepository;

  @Autowired private StatusService statusService;

  @Test
  public void givenIds_shouldReturnRepositoryResults() {
    HealthTrackerStatus htStatus = new HealthTrackerStatus();
    htStatus.setClinicId(1234l);
    PatientInfo pi = new PatientInfo();
    pi.setId(1234l);
    htStatus.setPatientInfo(pi);
    List<HealthTrackerStatus> repoResults = new ArrayList<>(Arrays.asList(htStatus));
    when(statusRepository.findByIds(any(Long.class), any(List.class))).thenReturn(repoResults);

    List<String> ids = new ArrayList<>(Arrays.asList("55555"));
    List<HealthTrackerStatus> results = statusService.getByIds(2345l, ids);

    Assert.assertEquals("should get array from repository call", repoResults, results);
  }

  @Test
  public void givenPendingAndMissedCheckin_shouldReturnManualCollect() {
    // need to return checkin, enrollment and status
    String eid1 = UUID.randomUUID().toString();
    String eid2 = UUID.randomUUID().toString();
    String eid3 = UUID.randomUUID().toString();

    Enrollment e1 = new Enrollment();
    e1.setId(eid1);

    Enrollment e2 = new Enrollment();
    e2.setId(eid2);

    CheckInData checkIn1 = new CheckInData();
    checkIn1.setEnrollment(e1);

    CheckInData checkIn2 = new CheckInData();
    checkIn2.setEnrollment(e2);

    HealthTrackerStatus status = new HealthTrackerStatus();
    status.setId(eid1);

    HealthTrackerStatus status2 = new HealthTrackerStatus();
    status2.setId(eid2);

    when(checkInRepository.findCheckIns(any(List.class), any(), eq(CheckInType.SYMPTOM)))
        .thenReturn(Arrays.asList(eid1));
    when(checkInRepository.findCheckIns(any(List.class), any(), eq(CheckInType.ORAL)))
        .thenReturn(Arrays.asList(eid2));
    when(checkInService.getCheckInDataByEnrollmentIDs(any(List.class), anyBoolean()))
        .thenReturn(Arrays.asList(checkIn1, checkIn2));

    when(enrollmentRepository.findEnrollmentsByIds(any(Long.class), any(List.class)))
        .thenReturn(Arrays.asList(e1, e2));

    when(statusRepository.findByIds(any(Long.class), any(List.class)))
        .thenReturn(Arrays.asList(status, status2));

    List<String> ids = Arrays.asList(eid1, eid2, eid3);
    Long clinicId = 2l;
    List<HealthTrackerStatusResponse> results =
        statusService.getManualCollectDueByIds(clinicId, ids);

    Assert.assertNotNull(results);
    Assert.assertEquals(2, results.size());
    Assert.assertEquals(eid1, results.get(0).getEnrollment().getId());
  }
}
