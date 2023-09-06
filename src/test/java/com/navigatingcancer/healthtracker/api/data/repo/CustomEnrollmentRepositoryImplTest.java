package com.navigatingcancer.healthtracker.api.data.repo;

import com.navigatingcancer.healthtracker.api.TestConfig;
import com.navigatingcancer.healthtracker.api.data.model.Enrollment;
import com.navigatingcancer.healthtracker.api.data.model.EnrollmentStatus;
import com.navigatingcancer.healthtracker.api.processor.HealthTrackerStatusService;
import com.navigatingcancer.patientinfo.PatientInfoClient;
import com.navigatingcancer.scheduler.client.service.SchedulerServiceClient;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.After;
import org.junit.Assert;
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
public class CustomEnrollmentRepositoryImplTest {
  @Autowired private EnrollmentRepository enrollmentRepository;

  @MockBean private SchedulerServiceClient schedulerServiceClient;

  @MockBean private HealthTrackerStatusService healthTrackerStatusService;

  @MockBean private PatientInfoClient patientInfoClient;

  // without Manual collect
  private Enrollment e1WithoutManualCollect, e2WithoutManualCollect;

  // WITH MANUAL COLLECT
  // these should be included in the result for Manual collect
  // txStartDate is set and is in past for different clinicIds
  private Enrollment e1WithManualCollectTxDateInPast, e2WithManualCollectTxDateInPast;
  // reminderStartDate is set and is in past
  private Enrollment e1WithManualCollectReminderDateInPast;
  // both dates in past
  private Enrollment e1WithManualCollectBothDatesInPast;

  // these should NOT be included in the result for Manual collect
  // txStartDate is set and is in future
  private Enrollment e1WithManualCollectTxStartDateInFuture;
  // reminderStartDate is set and is in future
  private Enrollment e1WithManualCollectReminderDateInFuture;
  // both are set and in future
  private Enrollment e1WithManualCollectBothDatesInFuture;

  // txStartDate is in past and reminderStart is in future so should NOT be included
  private Enrollment e1WithManualCollectTxDateInPastReminderInFuture;
  // txStartDate is in future and reminderStart is in past so should BE included
  private Enrollment e1WithManualCollectTxDateInFutureReminderDateInPast;

  private LocalDate futureDate, pastDate, today;
  private long clinicId1, locationId1, providerId1;
  private long patientId1;
  private long clinicId2, locationId2, providerId2;
  private long patientId2;

  private List<Long> allClinicIds;

  @Before
  public void setUp() throws Exception {
    today = LocalDate.now();
    futureDate = today.plusDays(1);
    pastDate = today.minusDays(1);

    clinicId1 = 1;
    locationId1 = 10;
    providerId1 = 100;
    patientId1 = 1000;

    e1WithoutManualCollect =
        createEnrollment(patientId1, clinicId1, locationId1, providerId1, null, null, false);

    // Keep 1 with locationId set for testing locationId filters
    e1WithManualCollectTxDateInPast =
        createEnrollment(patientId1, clinicId1, locationId1, null, pastDate, null, true);
    e1WithManualCollectReminderDateInPast =
        createEnrollment(patientId1, clinicId1, null, null, null, pastDate, true);
    e1WithManualCollectBothDatesInPast =
        createEnrollment(patientId1, clinicId1, null, null, pastDate, pastDate, true);
    e1WithManualCollectTxStartDateInFuture =
        createEnrollment(patientId1, clinicId1, null, null, futureDate, null, true);
    e1WithManualCollectReminderDateInFuture =
        createEnrollment(patientId1, clinicId1, null, null, null, futureDate, true);
    e1WithManualCollectBothDatesInFuture =
        createEnrollment(patientId1, clinicId1, null, null, futureDate, futureDate, true);
    e1WithManualCollectTxDateInPastReminderInFuture =
        createEnrollment(patientId1, clinicId1, null, null, pastDate, futureDate, true);
    e1WithManualCollectTxDateInFutureReminderDateInPast =
        createEnrollment(patientId1, clinicId1, null, null, futureDate, pastDate, true);

    clinicId2 = 2;
    locationId2 = 20;
    providerId2 = 200;
    patientId2 = 2000;

    e2WithManualCollectTxDateInPast =
        createEnrollment(patientId2, clinicId2, locationId2, providerId2, pastDate, null, true);

    e2WithoutManualCollect =
        createEnrollment(patientId2, clinicId2, locationId2, providerId2, null, null, false);

    allClinicIds = new ArrayList<>(Arrays.asList(clinicId1, clinicId2));
  }

  @After
  public void tearDown() throws Exception {
    enrollmentRepository.deleteAll();
  }

  @Test
  public void getCurrentEnrollments_clinicIdBasedFiltering() {
    List<Enrollment> enrollments =
        enrollmentRepository.getCurrentEnrollments(allClinicIds, null, null, null);
    Assert.assertEquals(
        "should find all enrollments if no filters selected", 11, enrollments.size());

    // test with subset of clinicIds
    List<Long> clinicIds = new ArrayList<>(Arrays.asList(e2WithoutManualCollect.getClinicId()));
    enrollments = enrollmentRepository.getCurrentEnrollments(clinicIds, null, null, null);
    Assert.assertEquals("should find enrollments matching clinicIds", 2, enrollments.size());

    List<String> enrollmentIds = convertToEnrollmentIds(enrollments);
    Assert.assertTrue(enrollmentIds.contains(e2WithoutManualCollect.getId()));
    Assert.assertTrue(enrollmentIds.contains(e2WithManualCollectTxDateInPast.getId()));
  }

  @Test
  public void getCurrentEnrollments_allFiltersSelected() {
    List<Long> locationIds = new ArrayList<>(Arrays.asList(locationId1, locationId2, null));
    List<Long> providerIds = new ArrayList<>(Arrays.asList(providerId1, providerId2, null));

    List<Enrollment> enrollments =
        enrollmentRepository.getCurrentEnrollments(allClinicIds, locationIds, providerIds, null);
    Assert.assertEquals(
        "should find all enrollments if all filters are selected", 11, enrollments.size());
  }

  @Test
  public void getCurrentEnrollments_manualCollectFiltering() {
    List<Enrollment> enrollments;

    // test with manual collect = true
    enrollments = enrollmentRepository.getCurrentEnrollments(allClinicIds, null, null, true);

    List<String> enrollmentIds = convertToEnrollmentIds(enrollments);
    Assert.assertTrue(enrollmentIds.contains(e1WithManualCollectTxDateInPast.getId()));
    Assert.assertTrue(enrollmentIds.contains(e2WithManualCollectTxDateInPast.getId()));
    Assert.assertTrue(enrollmentIds.contains(e1WithManualCollectReminderDateInPast.getId()));
    Assert.assertTrue(enrollmentIds.contains(e1WithManualCollectBothDatesInPast.getId()));
    Assert.assertTrue(
        enrollmentIds.contains(e1WithManualCollectTxDateInFutureReminderDateInPast.getId()));
    Assert.assertEquals(
        "should find only manual collect == true enrollments", 5, enrollments.size());

    Assert.assertFalse(enrollmentIds.contains(e1WithManualCollectTxStartDateInFuture.getId()));
    Assert.assertFalse(enrollmentIds.contains(e1WithManualCollectReminderDateInFuture.getId()));
    Assert.assertFalse(enrollmentIds.contains(e1WithManualCollectBothDatesInFuture.getId()));
    Assert.assertFalse(
        enrollmentIds.contains(e1WithManualCollectTxDateInPastReminderInFuture.getId()));

    // test with manual collect = false
    enrollments = enrollmentRepository.getCurrentEnrollments(allClinicIds, null, null, false);
    Assert.assertEquals(
        "should find only manual collect == false enrollments", 2, enrollments.size());

    enrollmentIds = convertToEnrollmentIds(enrollments);
    Assert.assertTrue(enrollmentIds.contains(e1WithoutManualCollect.getId()));
    Assert.assertTrue(enrollmentIds.contains(e2WithoutManualCollect.getId()));
  }

  @Test
  public void getCurrentEnrollments_locationFiltering() {
    List<Enrollment> enrollments;

    // test locationId filtering
    long locationId = e1WithoutManualCollect.getLocationId();
    List<Long> locationIds = new ArrayList<>(Arrays.asList(locationId));
    enrollments = enrollmentRepository.getCurrentEnrollments(allClinicIds, locationIds, null, null);
    Assert.assertEquals("should filter on locationId", 2, enrollments.size());

    List<String> enrollmentIds = convertToEnrollmentIds(enrollments);
    Assert.assertTrue(enrollmentIds.contains(e1WithoutManualCollect.getId()));
    Assert.assertTrue(enrollmentIds.contains(e1WithManualCollectTxDateInPast.getId()));
  }

  @Test
  public void getCurrentEnrollments_providerFiltering() {
    List<Enrollment> enrollments;

    // test provider filtering
    long providerId = e2WithManualCollectTxDateInPast.getProviderId();
    List<Long> providerIds = new ArrayList<>(Arrays.asList(providerId));
    enrollments = enrollmentRepository.getCurrentEnrollments(allClinicIds, null, providerIds, null);
    Assert.assertEquals("should filter on providerIds", 2, enrollments.size());

    List<String> enrollmentIds = convertToEnrollmentIds(enrollments);
    Assert.assertTrue(enrollmentIds.contains(e2WithManualCollectTxDateInPast.getId()));
    Assert.assertTrue(enrollmentIds.contains(e2WithoutManualCollect.getId()));
  }

  @Test
  public void getCurrentEnrollments_MultipleFilters() {
    List<Enrollment> enrollments;

    List<Long> providerIds = new ArrayList<>(Arrays.asList(e2WithoutManualCollect.getProviderId()));
    List<Long> locationIds = new ArrayList<>(Arrays.asList(e2WithoutManualCollect.getLocationId()));

    enrollments =
        enrollmentRepository.getCurrentEnrollments(
            allClinicIds, locationIds, providerIds, e2WithoutManualCollect.isManualCollect());

    Assert.assertEquals("should find 1 enrollment matching all filters", 1, enrollments.size());
    Assert.assertEquals(
        "should find 1 enrollment matching",
        e2WithoutManualCollect.getId(),
        enrollments.get(0).getId());
  }

  @Test
  public void getCurrentEnrollments_LocationIdContains0() {
    createEnrollment(88L, 11L, 1L, null, null, null, false);
    createEnrollment(888L, 11L, 0L, null, null, null, false);
    createEnrollment(8888L, 11L, null, null, null, null, false);

    List<Enrollment> enrollments =
        enrollmentRepository.getCurrentEnrollments(
            Arrays.asList(11L), Arrays.asList(1L), null, false);
    Assert.assertTrue(enrollments.size() == 1);
    enrollments =
        enrollmentRepository.getCurrentEnrollments(
            Arrays.asList(11L), Arrays.asList(1L, 0L), null, false);
    Assert.assertTrue(enrollments.size() == 3);
    enrollments =
        enrollmentRepository.getCurrentEnrollments(
            Arrays.asList(11L), Arrays.asList(0L), null, false);
    Assert.assertTrue(enrollments.size() == 2);
  }

  @Test
  public void getCurrentEnrollments_ProviderIdContains0() {
    createEnrollment(88L, 11L, null, 1L, null, null, false);
    createEnrollment(888L, 11L, null, 0L, null, null, false);
    createEnrollment(8888L, 11L, null, null, null, null, false);

    List<Enrollment> enrollments =
        enrollmentRepository.getCurrentEnrollments(
            Arrays.asList(11L), null, Arrays.asList(1L), false);
    Assert.assertTrue(enrollments.size() == 1);
    enrollments =
        enrollmentRepository.getCurrentEnrollments(
            Arrays.asList(11L), null, Arrays.asList(1L, 0L), false);
    Assert.assertTrue(enrollments.size() == 3);
    enrollments =
        enrollmentRepository.getCurrentEnrollments(
            Arrays.asList(11L), null, Arrays.asList(0L), false);
    Assert.assertTrue(enrollments.size() == 2);
  }

  private List<String> convertToEnrollmentIds(List<Enrollment> enrollments) {
    return enrollments.stream().map(enrollment -> enrollment.getId()).collect(Collectors.toList());
  }

  private Enrollment createEnrollment(
      Long patientId,
      Long clinicId,
      Long locationId,
      Long providerId,
      LocalDate txStartDate,
      LocalDate reminderStartDate,
      Boolean manualCollect) {
    Enrollment enrollment = new Enrollment();
    enrollment.setId(UUID.randomUUID().toString());
    enrollment.setPatientId(patientId);
    enrollment.setClinicId(clinicId);
    enrollment.setLocationId(locationId);
    enrollment.setManualCollect(manualCollect);
    enrollment.setTxStartDate(txStartDate);
    enrollment.setReminderStartDate(reminderStartDate);
    if (manualCollect) {
      enrollment.setStatus(EnrollmentStatus.ACTIVE);
      // enrollment.setTxStartDate(LocalDate.now().minusDays(10));
      // enrollment.setReminderStartDate(LocalDate.now().minusDays(10));
    }
    enrollment.setProviderId(providerId);
    enrollmentRepository.save(enrollment);
    return enrollment;
  }
}
