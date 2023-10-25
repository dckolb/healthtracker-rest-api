package com.navigatingcancer.healthtracker.api.data.service;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.verify;

import com.navigatingcancer.healthtracker.api.TestConfig;
import com.navigatingcancer.healthtracker.api.data.auth.Identity;
import com.navigatingcancer.healthtracker.api.data.client.PatientInfoServiceClient;
import com.navigatingcancer.healthtracker.api.data.model.*;
import com.navigatingcancer.healthtracker.api.data.model.patientInfo.PatientInfo;
import com.navigatingcancer.healthtracker.api.data.repo.EnrollmentRepository;
import com.navigatingcancer.healthtracker.api.data.repo.EnrollmentRepositoryTest;
import com.navigatingcancer.healthtracker.api.data.repo.HealthTrackerStatusRepository;
import com.navigatingcancer.healthtracker.api.data.service.impl.NotificationService;
import com.navigatingcancer.healthtracker.api.data.service.impl.PatientRecordService;
import com.navigatingcancer.healthtracker.api.data.service.impl.PxTokenService;
import com.navigatingcancer.healthtracker.api.data.service.impl.SchedulingServiceImpl;
import com.navigatingcancer.healthtracker.api.processor.HealthTrackerStatusService;
import com.navigatingcancer.healthtracker.api.rest.QueryParameters.EnrollmentQuery;
import com.navigatingcancer.healthtracker.api.rest.exception.DuplicateEnrollmentException;
import java.security.InvalidParameterException;
import java.time.LocalDate;
import java.util.*;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
@Import(TestConfig.class)
public class EnrollmentServiceTest {

  @Autowired private EnrollmentRepository enrollmentRepository;

  @Autowired private EnrollmentService enrollmentService;

  @Autowired private HealthTrackerStatusRepository statusRepository;

  @Autowired private HealthTrackerStatusService statusService;

  @MockBean private SchedulingServiceImpl schedulingServiceImpl;

  @MockBean private PxTokenService pxTokenService;

  @MockBean private PatientInfoServiceClient patientInfoClient;

  @MockBean private NotificationService notificationService;

  @MockBean private PatientRecordService patientRecordService;

  @MockBean private Identity identity;

  @Before
  public void setup() {
    Mockito.doReturn("url").when(pxTokenService).getUrl(any());
    Mockito.doNothing().when(schedulingServiceImpl).schedule(any(), anyBoolean());
    Mockito.doNothing().when(notificationService).sendNotification(any(), any(), any(), any());
    Mockito.doNothing().when(patientRecordService).publishEnrollmentCreated(any(), any());
    Mockito.doNothing().when(patientRecordService).publishProData(any(), any(), any());

    PatientInfoServiceClient.FeignClient client =
        Mockito.mock(PatientInfoServiceClient.FeignClient.class);
    Mockito.when(patientInfoClient.getApi()).thenReturn(client);
    Mockito.when(client.getPatients(Mockito.any(), Mockito.any()))
        .thenReturn(Arrays.asList(new PatientInfo()));
  }

  @Test
  public void givenNewEnrollment_patientRecordServiceShouldHaveId() {
    Enrollment e = EnrollmentRepositoryTest.createEnrollment(90, 90, 90);

    ArgumentCaptor<Enrollment> argumentCaptorEnrollment = ArgumentCaptor.forClass(Enrollment.class);
    ArgumentCaptor<Identity> argumentCaptorIdentity = ArgumentCaptor.forClass(Identity.class);

    enrollmentService.createEnrollment(e);
    verify(patientRecordService)
        .publishEnrollmentCreated(
            argumentCaptorEnrollment.capture(), argumentCaptorIdentity.capture());

    Enrollment arg = argumentCaptorEnrollment.getValue();

    Assert.assertTrue(arg.getId() != null);
  }

  @Test(expected = DuplicateEnrollmentException.class)
  public void givenEnrollment_shouldThrowOnDuplicate() {
    Enrollment e1 = EnrollmentRepositoryTest.createEnrollment(4, 4, 4);
    Enrollment e2 = EnrollmentRepositoryTest.createEnrollment(4, 4, 4);

    enrollmentService.createEnrollment(e1);
    enrollmentService.createEnrollment(e2);
  }

  @Test
  public void givenId_shouldReturnEnrollment() {
    String id = UUID.randomUUID().toString();

    Enrollment e = EnrollmentRepositoryTest.createEnrollment(1, 5, 5);
    e.setId(id);
    Set<TherapyType> types = e.getTherapyTypes();
    if (types == null) {
      types = new HashSet<TherapyType>();
    }
    types.add(TherapyType.IV);
    e.setTherapyTypes(types);

    enrollmentRepository.save(e);

    Enrollment enrollmentMono = enrollmentService.getEnrollment(id);

    Assert.assertNotNull("should get result of repository findById method", enrollmentMono);
    Assert.assertNotNull("should have programid set", enrollmentMono.getProgramId());
  }

  @Test
  public void givenQuery_shouldReturnFluxList() {
    Enrollment e1 = EnrollmentRepositoryTest.createEnrollment(4, 4, 4);
    Enrollment e2 = EnrollmentRepositoryTest.createEnrollment(5, 5, 5);
    Enrollment e3 = EnrollmentRepositoryTest.createEnrollment(1, 1, 1);

    enrollmentRepository.save(e1);
    enrollmentRepository.save(e2);
    enrollmentRepository.save(e3);

    EnrollmentQuery query = new EnrollmentQuery();
    query.setStatus(new ArrayList<>());
    query.getStatus().add(EnrollmentStatus.ACTIVE);
    query.setLocationId(Arrays.asList(4L));
    query.setClinicId(Arrays.asList(4L));

    List<Enrollment> enrollmentFlux = enrollmentService.getEnrollments(query);

    for (Enrollment result : enrollmentFlux) {
      assertEquals("should not contain third enrollment", result.getClinicId().intValue(), 4);
      assertEquals("should not contain third enrollment", result.getLocationId().intValue(), 4);
    }
  }

  @Test
  public void testEnrollmentStopped() throws Exception {
    Enrollment e = EnrollmentRepositoryTest.createEnrollment(7, 7, 7);
    enrollmentRepository.save(e);

    // create status
    HealthTrackerStatus htStatus = statusService.getOrCreateNewStatus(e);
    Assert.assertNotNull(htStatus);
    Assert.assertEquals(
        "status should be pending", HealthTrackerStatusCategory.PENDING, htStatus.getCategory());

    enrollmentService.changeStatus(e.getId(), EnrollmentStatus.STOPPED, "reason", "note");

    ArgumentCaptor<NotificationService.Event> argument =
        ArgumentCaptor.forClass(NotificationService.Event.class);
    verify(notificationService).sendNotification(any(), any(), argument.capture(), any());
    Assert.assertEquals(argument.getValue(), NotificationService.Event.STOPPED);

    e = enrollmentRepository.findById(e.getId()).get();

    Assert.assertEquals(e.getStatusLogs().size(), 1);
    Assert.assertEquals(e.getStatusLogs().get(0).getStatus(), EnrollmentStatus.STOPPED);
    Assert.assertEquals(e.getStatusLogs().get(0).getReason(), "reason");
    Assert.assertEquals(e.getStatusLogs().get(0).getNote(), "note");

    // ensure ht status updated
    htStatus = statusRepository.getById(e.getId());
    Assert.assertNotNull(htStatus);
    Assert.assertEquals(
        "status should be completed",
        HealthTrackerStatusCategory.COMPLETED,
        htStatus.getCategory());
  }

  @Test
  public void updateEnrollment_success() {
    // Arrange
    String id = UUID.randomUUID().toString();
    Enrollment e = EnrollmentRepositoryTest.createEnrollment(5, 5, 5);
    e.setId(id);
    enrollmentRepository.save(e);

    e.setEmailAddress("daffy@internet.com");

    // Act
    Enrollment result = enrollmentService.updateEnrollment(e);

    // Assert
    // ... no errors should be thrown
  }

  @Test
  public void updateEnrollment_success_sameCreatedDate() {
    // Arrange
    String id = UUID.randomUUID().toString();
    Enrollment e = EnrollmentRepositoryTest.createEnrollment(5, 5, 5);
    e.setId(id);
    enrollmentRepository.save(e);

    Date actualdate = e.getCreatedDate();
    String actualby = e.getCreatedBy();

    int dayInMilliseconds = 24 * 60 * 60 * 1000;
    Date createdDate = new Date(System.currentTimeMillis() - dayInMilliseconds);
    String createdBy = "DROCKEM";

    e.setEmailAddress("daffy@internet.com");
    e.setCreatedDate(createdDate);
    e.setCreatedBy(createdBy);

    // Act
    Enrollment result = enrollmentService.updateEnrollment(e);

    // Assert
    Assert.assertEquals(actualdate, result.getCreatedDate());
    Assert.assertEquals(actualby, result.getCreatedBy());
  }

  @Test(expected = InvalidParameterException.class)
  public void updateEnrollment_failure_scheduleCountMismatch() {
    // Arrange
    String id = UUID.randomUUID().toString();
    Enrollment e = EnrollmentRepositoryTest.createEnrollment(5, 5, 5);
    e.setId(id);
    enrollmentRepository.save(e);

    e.setEmailAddress("daffy@internet.com");
    List<CheckInSchedule> schedules = new ArrayList<CheckInSchedule>();
    e.setSchedules(schedules);

    // Act
    Enrollment result = enrollmentService.updateEnrollment(e);

    // Assert
    // ... should throw invalidparamexception
  }

  @Test(expected = InvalidParameterException.class)
  public void updateEnrollment_failure_sameDateUpdate() {
    // Arrange
    String id = UUID.randomUUID().toString();
    Enrollment e = EnrollmentRepositoryTest.createEnrollment(5, 5, 5);
    e.setId(id);
    e.setTxStartDate(LocalDate.now());
    enrollmentRepository.save(e);

    e.setEmailAddress("cindybear@internet.com");
    List<CheckInSchedule> schedules = new ArrayList<CheckInSchedule>();
    e.setSchedules(schedules);

    // Act
    Enrollment result = enrollmentService.updateEnrollment(e);

    // Assert
    // ... should throw invalidparamexception
  }

  public void updateEnrollment_success_afterDateUpdate() {
    // Arrange
    String id = UUID.randomUUID().toString();
    Enrollment e = EnrollmentRepositoryTest.createEnrollment(5, 5, 5);
    e.setId(id);
    e.setTxStartDate(LocalDate.now().minusDays(10));
    enrollmentRepository.save(e);

    e.setEmailAddress("cindybear@internet.com");
    List<CheckInSchedule> schedules = new ArrayList<CheckInSchedule>();
    e.setSchedules(schedules);

    // Act
    Enrollment result = enrollmentService.updateEnrollment(e);

    // Assert
    // ... should throw invalidparamexception
  }

  public void updateEnrollment_success_beforeTxStartDate() {
    // Arrange
    String id = UUID.randomUUID().toString();
    Enrollment e = EnrollmentRepositoryTest.createEnrollment(5, 5, 5);
    e.setId(id);
    e.setTxStartDate(LocalDate.now().plusDays(1));
    enrollmentRepository.save(e);

    e.setEmailAddress("cindybear@internet.com");
    List<CheckInSchedule> schedules = new ArrayList<CheckInSchedule>();
    e.setSchedules(schedules);

    // Act
    Enrollment result = enrollmentService.updateEnrollment(e);

    // Assert
    // ... should not throw invalidparamexception
  }

  @Test
  public void updateEnrollment_success_scheduleMatching() {
    // Arrange
    String id = UUID.randomUUID().toString();
    Enrollment e = EnrollmentRepositoryTest.createEnrollment(5, 5, 5);
    e.setId(id);
    e.setTxStartDate(LocalDate.now().plusDays(1));

    var scheduleId = ObjectId.get().toHexString();
    e.getSchedules().get(0).setId(scheduleId);

    e = enrollmentRepository.save(e);

    e.setEmailAddress("cindybear@internet.com");

    // simulate id missing from incoming schedule
    e.getSchedules().get(0).setId(null);

    // Act
    Enrollment result = enrollmentService.updateEnrollment(e);

    assertEquals(
        "update should retain schedule id", scheduleId, result.getSchedules().get(0).getId());
  }

  @Test
  public void updateEnrollment_addsScheduleToMinimalEnrollment() {
    Enrollment enrollment = new Enrollment();
    enrollment.setPatientId(5l);
    enrollment.setClinicId(4l);
    enrollment.setStatus(EnrollmentStatus.ACTIVE);
    enrollment.setSchedules(new ArrayList<>());

    var savedEnrollment = enrollmentRepository.save(enrollment);

    CheckInSchedule schedule = new CheckInSchedule();
    schedule.setCheckInFrequency(CheckInFrequency.DAILY);
    schedule.setCheckInType(CheckInType.ORAL);
    schedule.setStartDate(LocalDate.now().minusDays(1l));
    schedule.setMedication(UUID.randomUUID().toString());
    var enrollmentInput =
        EnrollmentRepositoryTest.createEnrollmentWithSchedules(
            8l, 5l, 4l, EnrollmentStatus.ACTIVE, schedule);

    enrollmentInput.setId(savedEnrollment.getId());
    enrollmentInput.setVersion(savedEnrollment.getVersion());

    var result = enrollmentService.updateEnrollment(enrollmentInput);

    assertNotNull(result);
    assertEquals(1, result.getSchedules().size());

    var foundEnrollment = enrollmentRepository.findById(result.getId()).get();

    assertEquals(1, foundEnrollment.getSchedules().size());
  }

  @Test
  public void updateEnrollment_doesNotErrorWithNoSchedule() {
    Enrollment enrollment = new Enrollment();
    enrollment.setPatientId(5l);
    enrollment.setClinicId(4l);
    enrollment.setStatus(EnrollmentStatus.ACTIVE);
    enrollment.setSchedules(new ArrayList<>());

    var savedEnrollment = enrollmentRepository.save(enrollment);

    Long locationId = 8l;
    enrollment.setId(savedEnrollment.getId());
    enrollment.setLocationId(locationId);

    var result = enrollmentService.updateEnrollment(enrollment);

    assertNotNull(result);
    assertEquals(locationId, result.getLocationId());

    var foundEnrollment = enrollmentRepository.findById(result.getId()).get();

    assertEquals(locationId, foundEnrollment.getLocationId());
  }

  @Test
  public void createEnrollment_createsMinimalEnrollment() {
    Long patientId = 9l;
    Long clinicId = 4l;
    Enrollment enrollment = new Enrollment();
    enrollment.setPatientId(patientId);
    enrollment.setClinicId(clinicId);
    enrollment.setStatus(EnrollmentStatus.ACTIVE);
    enrollment.setSchedules(new ArrayList<>());
    Enrollment result = enrollmentService.createEnrollment(enrollment);

    assertNotNull(result.getId());
    assertEquals(EnrollmentStatus.ACTIVE, result.getStatus());
    assertEquals(patientId, result.getPatientId());
    assertEquals(clinicId, result.getClinicId());
  }
}
