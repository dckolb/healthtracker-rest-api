package com.navigatingcancer.healthtracker.api.data.repo;

import static org.junit.Assert.assertEquals;

import com.navigatingcancer.healthtracker.api.TestConfig;
import com.navigatingcancer.healthtracker.api.data.model.CheckInFrequency;
import com.navigatingcancer.healthtracker.api.data.model.CheckInSchedule;
import com.navigatingcancer.healthtracker.api.data.model.CheckInType;
import com.navigatingcancer.healthtracker.api.data.model.Enrollment;
import com.navigatingcancer.healthtracker.api.data.model.EnrollmentStatus;
import com.navigatingcancer.healthtracker.api.rest.QueryParameters.EnrollmentQuery;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
@Import(TestConfig.class)
public class EnrollmentRepositoryTest {

  @Autowired private EnrollmentRepository repo;

  public static Enrollment createEnrollment(long locationId, long clinicId, long patientId) {
    CheckInSchedule schedule = new CheckInSchedule();
    schedule.setCheckInFrequency(CheckInFrequency.DAILY);
    schedule.setCheckInType(CheckInType.ORAL);
    schedule.setStartDate(LocalDate.now().minusDays(1l));
    schedule.setMedication(UUID.randomUUID().toString());

    return createEnrollmentWithSchedules(locationId, clinicId, patientId, schedule);
  }

  public static Enrollment createEnrollmentWithSchedules(
      long locationId,
      long clinicId,
      long patientId,
      EnrollmentStatus status,
      CheckInSchedule... schedules) {
    Enrollment en = new Enrollment();
    en.setLocationId(locationId);
    en.setClinicId(clinicId);
    en.setPatientId(patientId);
    en.setTxStartDate(LocalDate.now().minusDays(1));
    en.setStatus(status);
    en.setCycleNumber(1);
    en.setCycles(1);
    en.setDaysInCycle(21);
    en.setEmailAddress(UUID.randomUUID().toString());
    en.setPhoneNumber("425-555-1212");
    en.setRepeat(0);
    en.setMedication(UUID.randomUUID().toString());
    en.setReminderTime("09:00");
    en.setReminderTimeZone(TimeZone.getDefault().getID());
    en.setSchedules(Arrays.asList(schedules));
    return en;
  }

  public static Enrollment createEnrollmentWithSchedules(
      long locationId, long clinicId, long patientId, CheckInSchedule... schedules) {

    return createEnrollmentWithSchedules(
        locationId, clinicId, patientId, EnrollmentStatus.ACTIVE, schedules);
  }

  public static void assertEnrollmentEqual(Enrollment e1, Enrollment e2) {
    assertEquals(e1.getLocationId(), e2.getLocationId());
    assertEquals(e1.getClinicId(), e2.getClinicId());
    assertEquals(e1.getTxStartDate(), e2.getTxStartDate());
    assertEquals(e1.getStatus(), e2.getStatus());
    assertEquals(e1.getCycleNumber(), e2.getCycleNumber());
    assertEquals(e1.getCycles(), e2.getCycles());
    assertEquals(e1.getDaysInCycle(), e2.getDaysInCycle());
    assertEquals(e1.getEmailAddress(), e2.getEmailAddress());
    assertEquals(e1.getPhoneNumber(), e2.getPhoneNumber());
    assertEquals(e1.getReminderTime(), e2.getReminderTime());
    assertEquals(e1.getReminderTimeZone(), e2.getReminderTimeZone());
    assertEquals(e1.getRepeat(), e2.getRepeat());
  }

  @Test
  public void givenEnrollment_whenEnrollmentExists_returnsFalse() {
    Enrollment e = createEnrollment(22, 22, 22);
    repo.save(e); // block here to finish persisting

    Boolean result = repo.activeEnrollmentExists(22, 22);

    assertEquals(true, result);

    repo.delete(e);
  }

  @Test
  public void givenLocationId_whenFindByClinicId_thenFindEnrollment() {
    Enrollment e = createEnrollment(28, 28, 28);
    repo.save(e); // block here to finish persisting

    EnrollmentQuery query = new EnrollmentQuery();
    query.setClinicId(new ArrayList<>());
    query.getClinicId().add(28l);

    List<Enrollment> enrollments = repo.findEnrollments(query);

    for (Enrollment enrollment : enrollments) {
      assertEnrollmentEqual(e, enrollment);
    }

    repo.delete(e);
  }

  @Test
  public void givenLocationId_whenFindByPatientId_thenFindEnrollment() {
    Enrollment e = createEnrollment(2, 2, 2);
    repo.save(e); // block here to finish persisting

    EnrollmentQuery query = new EnrollmentQuery();
    query.setPatientId(new ArrayList<>());
    query.getPatientId().add(2l);

    List<Enrollment> enrollments = repo.findEnrollments(query);

    for (Enrollment enrollment : enrollments) {
      assertEnrollmentEqual(e, enrollment);
    }

    repo.delete(e);
  }

  @Test
  public void givenLocationId_whenFindByLocationId_thenFindEnrollment() {
    Enrollment e = createEnrollment(21, 21, 21);
    repo.save(e); // block here to finish persisting

    EnrollmentQuery query = new EnrollmentQuery();
    query.setLocationId(new ArrayList<>());
    query.getLocationId().add(21l);

    List<Enrollment> enrollments = repo.findEnrollments(query);

    for (Enrollment enrollment : enrollments) {
      assertEnrollmentEqual(e, enrollment);
    }

    repo.delete(e);
  }

  @Test
  public void givenLocationId_whenFindByStatus_thenFindEnrollment() {
    Enrollment e = createEnrollment(23, 23, 23);
    e.setStatus(EnrollmentStatus.PAUSED);
    repo.save(e); // block here to finish persisting

    EnrollmentQuery query = new EnrollmentQuery();
    query.setStatus(new ArrayList<>());
    query.getStatus().add(EnrollmentStatus.PAUSED);

    List<Enrollment> enrollments = repo.findEnrollments(query);

    for (Enrollment enrollment : enrollments) {
      assertEnrollmentEqual(e, enrollment);
    }

    repo.delete(e);
  }

  @Test
  public void givenLocationId_whenFindByDate_thenFindEnrollment() {
    Enrollment e = createEnrollment(25, 25, 25);
    e.setTxStartDate(LocalDate.now().minusDays(12));
    repo.save(e); // block here to finish persisting

    EnrollmentQuery query = new EnrollmentQuery();
    query.setStartDate(LocalDate.now().minusDays(13));
    query.setEndDate(LocalDate.now().minusDays(11));

    List<Enrollment> enrollments = repo.findEnrollments(query);

    for (Enrollment enrollment : enrollments) {
      assertEnrollmentEqual(e, enrollment);
    }

    repo.delete(e);
  }

  @Test
  public void givenMultipleEnrollments_returnLatest() throws Exception {
    Enrollment e1 = createEnrollment(88, 88, 88);
    e1 = repo.save(e1);
    e1.setStatus(EnrollmentStatus.STOPPED);
    repo.save(e1);
    Enrollment e2 = createEnrollment(88, 88, 88);
    repo.save(e2);
    EnrollmentQuery query = new EnrollmentQuery();
    query.setClinicId(Arrays.asList(88l));
    query.setPatientId(Arrays.asList(88l));

    List<Enrollment> results = repo.findEnrollments(query);

    Assert.assertTrue(results.size() == 1);

    query.setAll(true);
    results = repo.findEnrollments(query);

    Assert.assertTrue(results.size() == 2);
  }
}
