package com.navigatingcancer.healthtracker.api.data.repo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.navigatingcancer.healthtracker.api.TestConfig;
import com.navigatingcancer.healthtracker.api.data.model.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import org.junit.After;
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
public class CustomCheckInRepositoryTest {

  @Autowired private CheckInRepository checkInRepository;

  @Autowired private EnrollmentRepository enrollmentRepository;

  @After
  public void cleanup() {
    checkInRepository.deleteAll();
  }

  @Test
  public void givenOralAndSymptomCheckIns_findByType_shouldFilter() {
    String enrollmentId = UUID.randomUUID().toString();
    CheckIn ci = new CheckIn();
    ci.setStatus(CheckInStatus.COMPLETED);
    ci.setCheckInType(CheckInType.SYMPTOM);
    ci.setEnrollmentId(enrollmentId);
    ci.setScheduleDate(LocalDate.now().minusDays(1));
    this.checkInRepository.insert(ci);

    CheckIn ci2 = new CheckIn();
    ci2.setStatus(CheckInStatus.COMPLETED);
    ci2.setCheckInType(CheckInType.ORAL);
    ci2.setEnrollmentId(enrollmentId);
    ci2.setScheduleDate(LocalDate.now().minusDays(1));
    this.checkInRepository.insert(ci2);

    CheckIn ci3 = new CheckIn();
    ci3.setStatus(CheckInStatus.PENDING);
    ci3.setCheckInType(CheckInType.SYMPTOM);
    ci3.setEnrollmentId(enrollmentId);
    ci3.setScheduleDate(LocalDate.now());
    this.checkInRepository.insert(ci3);

    long count =
        this.checkInRepository
            .findByEnrollmentIdAndCheckInTypeAndStatusOrderByScheduleDateDesc(
                enrollmentId, CheckInType.ORAL, CheckInStatus.COMPLETED)
            .count();

    assertEquals("should only get one", 1, count);
    Optional<CheckIn> checkInOptional =
        this.checkInRepository
            .findByEnrollmentIdAndCheckInTypeAndStatusOrderByScheduleDateDesc(
                enrollmentId, CheckInType.ORAL, CheckInStatus.COMPLETED)
            .findFirst();
    assertTrue(checkInOptional.isPresent());
    CheckIn checkin = checkInOptional.get();
    assertTrue(checkin.equals(ci2));
  }

  @Test
  public void givenPendingCheckin_isPending_shouldReturnTrue() {
    String enrollmentId = UUID.randomUUID().toString();
    CheckIn ci = new CheckIn();
    ci.setStatus(CheckInStatus.PENDING);
    ci.setEnrollmentId(enrollmentId);
    ci.setScheduleDate(LocalDate.now());
    this.checkInRepository.insert(ci);

    boolean result = this.checkInRepository.isPending(enrollmentId);
    assertTrue("should be pending", result);
  }

  @Test
  public void givenMissedCheckin_isPending_shouldReturnFalse() {
    String enrollmentId = UUID.randomUUID().toString();
    CheckIn ci = new CheckIn();
    ci.setStatus(CheckInStatus.MISSED);
    ci.setEnrollmentId(enrollmentId);
    ci.setScheduleDate(LocalDate.now());
    this.checkInRepository.insert(ci);

    boolean result = this.checkInRepository.isPending(enrollmentId);
    Assert.assertFalse("should be pending", result);
  }

  @Test
  public void givenMissedCheckin_getMissing_shouldReturnOne() {
    String enrollmentId = UUID.randomUUID().toString();
    CheckIn ci = new CheckIn();
    ci.setStatus(CheckInStatus.MISSED);
    ci.setEnrollmentId(enrollmentId);
    ci.setScheduleDate(LocalDate.now().minusDays(1));
    this.checkInRepository.insert(ci);

    CheckIn ci2 = new CheckIn();
    ci2.setStatus(CheckInStatus.PENDING);
    ci2.setEnrollmentId(enrollmentId);
    ci2.setScheduleDate(LocalDate.now());
    this.checkInRepository.insert(ci2);

    Integer result = this.checkInRepository.getMissedCheckins(enrollmentId);
    assertTrue("should be one", result == 1);
  }

  @Test
  public void givenTwoMissedCheckin_getMissing_shouldReturnTwo() {
    String enrollmentId = UUID.randomUUID().toString();
    CheckIn ci = new CheckIn();
    ci.setStatus(CheckInStatus.MISSED);
    ci.setEnrollmentId(enrollmentId);
    ci.setScheduleDate(LocalDate.now().minusDays(1));
    this.checkInRepository.insert(ci);

    CheckIn ci2 = new CheckIn();
    ci2.setStatus(CheckInStatus.MISSED);
    ci2.setEnrollmentId(enrollmentId);
    ci2.setScheduleDate(LocalDate.now().minusDays(2));
    this.checkInRepository.insert(ci2);

    CheckIn ci3 = new CheckIn();
    ci3.setStatus(CheckInStatus.PENDING);
    ci3.setEnrollmentId(enrollmentId);
    ci3.setScheduleDate(LocalDate.now());
    this.checkInRepository.insert(ci3);

    Integer result = this.checkInRepository.getMissedCheckins(enrollmentId);
    assertTrue("should be two", result == 2);
  }

  @Test
  public void givenPending_findCheckin() {
    CheckInType ct = CheckInType.SYMPTOM;
    Enrollment e1 = new Enrollment();
    e1.setStatus(EnrollmentStatus.ACTIVE);
    enrollmentRepository.insert(e1);

    Enrollment e2 = new Enrollment();
    e2.setStatus(EnrollmentStatus.ACTIVE);
    enrollmentRepository.insert(e2);

    // 3 checkins, last one pending
    CheckIn ci = new CheckIn();
    ci.setStatus(CheckInStatus.COMPLETED);
    ci.setEnrollmentId(e1.getId());
    ci.setScheduleDate(LocalDate.now().minusDays(1));
    ci.setCheckInType(ct);
    this.checkInRepository.insert(ci);

    CheckIn ci2 = new CheckIn();
    ci2.setStatus(CheckInStatus.MISSED);
    ci2.setEnrollmentId(e1.getId());
    ci2.setScheduleDate(LocalDate.now().minusDays(2));
    ci2.setCheckInType(ct);
    this.checkInRepository.insert(ci2);

    CheckIn ci3 = new CheckIn();
    ci3.setStatus(CheckInStatus.PENDING);
    ci3.setEnrollmentId(e1.getId());
    ci3.setScheduleDate(LocalDate.now());
    ci3.setCheckInType(ct);
    this.checkInRepository.insert(ci3);

    // 2 checkins, last one completed
    CheckIn ci4 = new CheckIn();
    ci4.setStatus(CheckInStatus.COMPLETED);
    ci4.setEnrollmentId(e2.getId());
    ci4.setScheduleDate(LocalDate.now().minusDays(1));
    ci4.setCheckInType(ct);
    this.checkInRepository.insert(ci4);

    CheckIn ci5 = new CheckIn();
    ci5.setStatus(CheckInStatus.MISSED);
    ci5.setEnrollmentId(e2.getId());
    ci5.setScheduleDate(LocalDate.now().minusDays(2));
    ci5.setCheckInType(ct);
    this.checkInRepository.insert(ci5);

    // Out of 2 enrollments we should get only one with pendng checkin
    List<String> statuses = Arrays.asList("PENDING");
    List<String> results =
        this.checkInRepository.findCheckIns(Arrays.asList(e1.getId(), e2.getId()), statuses, ct);
    assertTrue(results.size() == 1);
    assertTrue("should be pending checkin", results.contains(ci3.getEnrollmentId()));
  }

  private CheckIn createCheckIn(
      String enrollmentId,
      CheckInType type,
      LocalDate scheduleDate,
      LocalTime scheduleTime,
      CheckInStatus status) {
    CheckIn ci = new CheckIn();
    ci.setEnrollmentId(enrollmentId);
    ci.setCheckInType(type);
    ci.setScheduleDate(scheduleDate);
    ci.setScheduleTime(scheduleTime);
    ci.setStatus(status);
    return ci;
  }

  @Test
  public void getCompletedCount() {
    LocalDate scheduleDate1 = LocalDate.of(1999, 12, 31);
    LocalDate scheduleDate2 = LocalDate.of(2000, 1, 2);
    LocalDate scheduleDate3 = LocalDate.of(2000, 1, 1);
    String enrollmentId1 = UUID.randomUUID().toString();
    String enrollmentId2 = UUID.randomUUID().toString();

    //                  for 1st enrollment

    // create 2 completed checkIns in 2 scheduled dates
    CheckIn checkIn1ForEid1 =
        createCheckIn(
            enrollmentId1, CheckInType.ORAL, scheduleDate1, null, CheckInStatus.COMPLETED);
    this.checkInRepository.insert(checkIn1ForEid1);
    CheckIn checkIn2ForEid1 =
        createCheckIn(
            enrollmentId1, CheckInType.ORAL, scheduleDate2, null, CheckInStatus.COMPLETED);
    this.checkInRepository.insert(checkIn2ForEid1);

    // create 1 schedule which is pending
    CheckIn pendingCheckIn1ForEid1 =
        createCheckIn(enrollmentId1, CheckInType.ORAL, scheduleDate3, null, CheckInStatus.PENDING);
    this.checkInRepository.insert(pendingCheckIn1ForEid1);

    // create 1 schedule which is missed
    CheckIn notCompletedCheckIn1ForEid1 =
        createCheckIn(enrollmentId1, CheckInType.ORAL, scheduleDate3, null, CheckInStatus.MISSED);
    this.checkInRepository.insert(notCompletedCheckIn1ForEid1);

    //                  for 2nd enrollment

    CheckIn checkIn1ForEid2 =
        createCheckIn(
            enrollmentId2, CheckInType.ORAL, scheduleDate1, null, CheckInStatus.COMPLETED);
    this.checkInRepository.insert(checkIn1ForEid2);

    //                  Expectations
    Integer expected = 2;
    // Should find 2 completed checkIns for 1st enrollment
    assertEquals("should match", expected, checkInRepository.getCompletedCount(enrollmentId1));

    // Should find 1 completed checkIns for 2nd enrollment
    expected = 1;
    assertEquals("should match", expected, checkInRepository.getCompletedCount(enrollmentId2));
  }

  @Test
  public void getTotalCount() {
    LocalDate scheduleDate1 = LocalDate.of(1999, 12, 31);
    LocalDate scheduleDate2 = LocalDate.of(2000, 1, 2);
    LocalDate scheduleDate3 = LocalDate.of(2000, 1, 1);
    String enrollmentId1 = UUID.randomUUID().toString();
    String enrollmentId2 = UUID.randomUUID().toString();

    //                  for 1st enrollment

    // create 2 completed checkIns in 2 scheduled dates
    CheckIn checkIn1ForEid1 =
        createCheckIn(
            enrollmentId1, CheckInType.ORAL, scheduleDate1, null, CheckInStatus.COMPLETED);
    this.checkInRepository.insert(checkIn1ForEid1);
    CheckIn checkIn2ForEid1 =
        createCheckIn(
            enrollmentId1, CheckInType.ORAL, scheduleDate2, null, CheckInStatus.COMPLETED);
    this.checkInRepository.insert(checkIn2ForEid1);

    // create 1 schedule which is pending
    CheckIn pendingCheckIn1ForEid1 =
        createCheckIn(enrollmentId1, CheckInType.ORAL, scheduleDate3, null, CheckInStatus.PENDING);
    this.checkInRepository.insert(pendingCheckIn1ForEid1);

    // create 1 schedule which is missed
    CheckIn notCompletedCheckIn1ForEid1 =
        createCheckIn(enrollmentId1, CheckInType.ORAL, scheduleDate3, null, CheckInStatus.MISSED);
    this.checkInRepository.insert(notCompletedCheckIn1ForEid1);

    //                  for 2nd enrollment

    CheckIn checkIn1ForEid2 =
        createCheckIn(
            enrollmentId2, CheckInType.ORAL, scheduleDate1, null, CheckInStatus.COMPLETED);
    this.checkInRepository.insert(checkIn1ForEid2);

    //                  Expectations
    Integer expected = 3;
    // Should find 3 completed checkIns for 1st enrollment.
    assertEquals("should match", expected, checkInRepository.getTotalCount(enrollmentId1));

    // Should find 1 completed checkIns for 2nd enrollment
    expected = 1;
    assertEquals("should match", expected, checkInRepository.getTotalCount(enrollmentId2));
  }

  @Test
  public void getTotalOralCount() {
    LocalDate scheduleDate1 = LocalDate.of(1999, 12, 31);
    LocalDate scheduleDate2 = LocalDate.of(2000, 1, 2);
    LocalDate scheduleDate3 = LocalDate.of(2000, 1, 1);
    String enrollmentId1 = UUID.randomUUID().toString();

    // create 2 checkIns for ORAL
    CheckIn oralCheckIn1 =
        createCheckIn(
            enrollmentId1, CheckInType.ORAL, scheduleDate1, null, CheckInStatus.COMPLETED);
    this.checkInRepository.insert(oralCheckIn1);
    CheckIn oralCheckIn2 =
        createCheckIn(
            enrollmentId1, CheckInType.ORAL, scheduleDate2, null, CheckInStatus.COMPLETED);
    this.checkInRepository.insert(oralCheckIn2);

    // create 1 checkIn for Symptom
    CheckIn symptomCheckIn =
        createCheckIn(
            enrollmentId1, CheckInType.SYMPTOM, scheduleDate3, null, CheckInStatus.COMPLETED);
    this.checkInRepository.insert(symptomCheckIn);

    //                  Expectations
    Integer expected = 2;
    // Should find 2 completed checkIns for ORAL
    assertEquals(expected, checkInRepository.getTotalOralCount(enrollmentId1));

    expected = 3;
    // Should find 3 completed checkIns in total
    assertEquals(expected, checkInRepository.getTotalCount(enrollmentId1));
  }

  @Test
  public void getAdherencePercent() {
    LocalDate scheduleDate1 = LocalDate.of(1999, 12, 31);
    LocalDate scheduleDate2 = LocalDate.of(2000, 1, 5);
    LocalDate scheduleDate3 = LocalDate.of(2000, 1, 2);
    LocalDate scheduleDate4 = LocalDate.of(2000, 1, 1);
    String enrollmentId1 = UUID.randomUUID().toString();

    // create 2 checkIns for ORAL with medicationTaken = true
    CheckIn c1 =
        createCheckIn(
            enrollmentId1, CheckInType.ORAL, scheduleDate1, null, CheckInStatus.COMPLETED);
    c1.setMedicationTaken(true);
    this.checkInRepository.insert(c1);
    CheckIn c2 =
        createCheckIn(
            enrollmentId1, CheckInType.ORAL, scheduleDate2, null, CheckInStatus.COMPLETED);
    c2.setMedicationTaken(true);
    this.checkInRepository.insert(c2);

    // create 1 checkIns for ORAL with medicationTaken = false
    CheckIn c3 =
        createCheckIn(
            enrollmentId1, CheckInType.ORAL, scheduleDate3, null, CheckInStatus.COMPLETED);
    c1.setMedicationTaken(false);
    this.checkInRepository.insert(c3);

    // create 1 checkIn for Symptom
    CheckIn notTakenCheckIn =
        createCheckIn(
            enrollmentId1, CheckInType.SYMPTOM, scheduleDate4, null, CheckInStatus.COMPLETED);
    notTakenCheckIn.setMedicationTaken(false);
    this.checkInRepository.insert(notTakenCheckIn);

    //                  Expectations
    Integer expected = 2;
    // Should find 2 completed checkIns for ORAL
    assertEquals(expected, checkInRepository.getTotalOralMedsTakenCount(enrollmentId1));

    // Total ORAL count should be 3
    expected = 3;
    assertEquals(expected, checkInRepository.getTotalOralCount(enrollmentId1));

    // Total count should be 4
    expected = 4;
    assertEquals(expected, checkInRepository.getTotalCount(enrollmentId1));

    // Adherence should be 2 out of 3
    float percent = 66.66f;
    assertEquals(percent, checkInRepository.getAdherencePercent(enrollmentId1), 0.01);

    // invalid enrollment id
    percent = 0f;
    expected = 0;
    assertEquals(expected, checkInRepository.getTotalOralMedsTakenCount("foo-bar"));
    assertEquals(expected, checkInRepository.getTotalOralCount("foo-bar"));
    assertEquals(expected, checkInRepository.getTotalCount("foo-bar"));
    assertEquals(percent, checkInRepository.getAdherencePercent("foo-bar"), 0);
  }

  @Test
  public void testSetMissed() throws InterruptedException {
    LocalDate scheduleDate1 = LocalDate.of(2019, 6, 15);
    String enrollmentId1 = UUID.randomUUID().toString();

    CheckIn c1 =
        createCheckIn(enrollmentId1, CheckInType.ORAL, scheduleDate1, null, CheckInStatus.PENDING);
    c1 = this.checkInRepository.insert(c1);
    CheckIn c2 =
        createCheckIn(
            enrollmentId1,
            CheckInType.ORAL,
            scheduleDate1.minusDays(1l),
            null,
            CheckInStatus.PENDING);
    c2 = this.checkInRepository.insert(c2);
    CheckIn c3 =
        createCheckIn(
            enrollmentId1,
            CheckInType.ORAL,
            scheduleDate1.plusDays(1l),
            null,
            CheckInStatus.PENDING);
    c3 = this.checkInRepository.insert(c3);

    // Sleep for a second to make sure we can compare dates
    Thread.sleep(1000l);

    // There are 2 missed checkins
    Long updated = checkInRepository.setMissedCheckins(enrollmentId1, scheduleDate1);
    assertEquals(Long.valueOf(2), updated);

    // All missed checkins should have been set to MISSED
    updated = checkInRepository.setMissedCheckins(enrollmentId1, scheduleDate1);
    assertEquals(Long.valueOf(0), updated);

    // Make sure updated date changes
    Optional<CheckIn> c1_2 = this.checkInRepository.findById(c1.getId());
    assertTrue(c1_2.isPresent());
    assertEquals(c1.getCreatedDate(), c1_2.get().getCreatedDate());
    Assert.assertNotEquals(c1.getUpdatedDate(), c1_2.get().getUpdatedDate());
  }

  @Test
  public void testSimpleLastMissedCheckinCount() {
    LocalDate scheduleDate1 = LocalDate.of(2019, 6, 15);
    String enrollmentId1 = UUID.randomUUID().toString();

    int daysOffest = 0;

    // 3 missed
    CheckIn c =
        createCheckIn(
            enrollmentId1,
            CheckInType.ORAL,
            scheduleDate1.minusDays(daysOffest++),
            null,
            CheckInStatus.MISSED);
    this.checkInRepository.insert(c);
    c =
        createCheckIn(
            enrollmentId1,
            CheckInType.ORAL,
            scheduleDate1.minusDays(daysOffest++),
            null,
            CheckInStatus.MISSED);
    this.checkInRepository.insert(c);
    c =
        createCheckIn(
            enrollmentId1,
            CheckInType.ORAL,
            scheduleDate1.minusDays(daysOffest++),
            null,
            CheckInStatus.MISSED);
    this.checkInRepository.insert(c);

    // There are 3 missed checkins since the last completed
    Long res = checkInRepository.getLastMissedCheckinsCount(enrollmentId1);
    assertEquals(Long.valueOf(3), res);
  }

  @Test
  public void testLastMissedCheckinCountAndCheckinSearch() {
    LocalDate scheduleDate1 = LocalDate.of(2019, 6, 15);
    String enrollmentId1 = UUID.randomUUID().toString();

    // Slect set of checkins we test the serach against
    List<CheckIn> searchSet = new LinkedList<>();
    CheckIn lastOral;
    CheckIn lastSymptom;

    int daysOffest = 0;
    // 2 pending on the same last day
    CheckIn c1 =
        createCheckIn(enrollmentId1, CheckInType.ORAL, scheduleDate1, null, CheckInStatus.PENDING);
    c1 = this.checkInRepository.insert(c1);
    lastOral = c1;
    searchSet.add(c1);
    c1 =
        createCheckIn(
            enrollmentId1, CheckInType.SYMPTOM, scheduleDate1, null, CheckInStatus.PENDING);
    c1 = this.checkInRepository.insert(c1);
    lastSymptom = c1;
    searchSet.add(c1);

    // 3 missed
    CheckIn c2 =
        createCheckIn(
            enrollmentId1,
            CheckInType.ORAL,
            scheduleDate1.minusDays(++daysOffest),
            null,
            CheckInStatus.MISSED);
    c2 = this.checkInRepository.insert(c2);
    searchSet.add(c2);
    c2 =
        createCheckIn(
            enrollmentId1,
            CheckInType.ORAL,
            scheduleDate1.minusDays(++daysOffest),
            null,
            CheckInStatus.MISSED);
    c2 = this.checkInRepository.insert(c2);
    searchSet.add(c2);
    // 2 checkins missed on the same date
    c2 =
        createCheckIn(
            enrollmentId1,
            CheckInType.ORAL,
            scheduleDate1.minusDays(++daysOffest),
            null,
            CheckInStatus.MISSED);
    this.checkInRepository.insert(c2);
    c2 =
        createCheckIn(
            enrollmentId1,
            CheckInType.SYMPTOM,
            scheduleDate1.minusDays(daysOffest),
            null,
            CheckInStatus.MISSED);
    c2 = this.checkInRepository.insert(c2);
    searchSet.add(c2);

    // 1 completed
    CheckIn c4 =
        createCheckIn(
            enrollmentId1,
            CheckInType.ORAL,
            scheduleDate1.minusDays(++daysOffest),
            null,
            CheckInStatus.COMPLETED);
    c4 = this.checkInRepository.insert(c4);
    searchSet.add(c4);
    // 2 more staill missed
    CheckIn c5 =
        createCheckIn(
            enrollmentId1,
            CheckInType.ORAL,
            scheduleDate1.minusDays(++daysOffest),
            null,
            CheckInStatus.MISSED);
    this.checkInRepository.insert(c5);
    CheckIn c6 =
        createCheckIn(
            enrollmentId1,
            CheckInType.ORAL,
            scheduleDate1.minusDays(++daysOffest),
            null,
            CheckInStatus.MISSED);
    this.checkInRepository.insert(c6);

    // There are 3 missed checkins since the last completed
    Long res = checkInRepository.getLastMissedCheckinsCount(enrollmentId1);
    assertEquals(Long.valueOf(3), res);

    // Ids list
    List<String> searchIds = searchSet.stream().map(ci -> ci.getId()).collect(Collectors.toList());
    // Find last ORAL from the set
    CheckIn foundOral = checkInRepository.getLastCheckinByType(searchIds, CheckInType.ORAL);
    Assert.assertNotNull(foundOral);
    assertEquals(lastOral.getId(), foundOral.getId());

    // Find last SYMPTOM from the set
    CheckIn foundSymptom = checkInRepository.getLastCheckinByType(searchIds, CheckInType.SYMPTOM);
    Assert.assertNotNull(foundSymptom);
    assertEquals(lastSymptom.getId(), foundSymptom.getId());

    // There is no COMBO in the list
    CheckIn foundCombo = checkInRepository.getLastCheckinByType(searchIds, CheckInType.COMBO);
    Assert.assertNull(foundCombo);
  }

  @Test
  public void updateFieldsById_throwsWithNullArgs() {
    assertThrows(
        NullPointerException.class,
        () -> checkInRepository.updateFieldsById(null, new HashMap<String, Object>()));

    assertThrows(
        NullPointerException.class, () -> checkInRepository.updateFieldsById("testId", null));
  }

  @Test
  public void updateFieldsById_updatesFields() {
    var c1 =
        createCheckIn(
            "testEnrollmentId",
            CheckInType.ORAL,
            LocalDate.now(),
            LocalTime.now(),
            CheckInStatus.PENDING);
    checkInRepository.insert(c1);

    var updates = new HashMap<String, Object>();
    updates.put("enrollmentId", "newId");
    updates.put("status", CheckInStatus.COMPLETED);
    checkInRepository.updateFieldsById(c1.getId(), updates);
    var updatedC1 = checkInRepository.findById(c1.getId()).get();

    assertEquals("newId", updatedC1.getEnrollmentId());
    assertEquals(CheckInStatus.COMPLETED, updatedC1.getStatus());
  }

  @Test
  public void findCheckInsBySchedule() {
    var schedule = new CheckInSchedule();
    schedule.setEnrollmentId("findCheckInsByScheduleEnrollmentId");
    schedule.setCheckInType(CheckInType.ORAL);
    schedule.setId("findCheckInsByScheduleScheduleId");
    var c1 =
        createCheckIn(
            schedule.getEnrollmentId(),
            schedule.getCheckInType(),
            LocalDate.now(),
            LocalTime.now(),
            CheckInStatus.PENDING);
    c1.setCheckInScheduleId("findCheckInsByScheduleScheduleId");
    var persistedC1 = checkInRepository.insert(c1);

    // verify that this check-in can be found via check-in type alone
    var c2 =
        createCheckIn(
            schedule.getEnrollmentId(),
            schedule.getCheckInType(),
            LocalDate.now(),
            LocalTime.now(),
            CheckInStatus.PENDING);
    var persistedC2 = checkInRepository.insert(c2);

    var found = checkInRepository.findCheckInsBySchedule(schedule.getEnrollmentId(), schedule);

    assertEquals(2, found.size());
    assertTrue(found.stream().anyMatch(c -> Objects.equals(c.getId(), persistedC1.getId())));
    assertTrue(found.stream().anyMatch(c -> Objects.equals(c.getId(), persistedC2.getId())));
  }

  @Test(expected = IllegalArgumentException.class)
  public void findCheckInsBySchedule_failsForInvalidSchedule() {
    var schedule = new CheckInSchedule();
    schedule.setEnrollmentId("findCheckInsByScheduleEnrollmentId");
    checkInRepository.findCheckInsBySchedule(schedule.getEnrollmentId(), schedule);
  }

  @Test
  public void upsertByNaturalKey_checkInTypeOnly() {
    var c1 =
        createCheckIn(
            "upsertByNaturalKey_checkInTypeOnly",
            CheckInType.ORAL,
            LocalDate.now(),
            LocalTime.now(),
            CheckInStatus.PENDING);

    checkInRepository.upsertByNaturalKey(c1);
    checkInRepository.upsertByNaturalKey(c1);
    checkInRepository.upsertByNaturalKey(c1);
    checkInRepository.upsertByNaturalKey(c1);

    var checkIns = checkInRepository.findByEnrollmentId("upsertByNaturalKey_checkInTypeOnly");
    Assert.assertEquals(1, checkIns.size());
  }

  @Test
  public void upsertByNaturalKey_differentDays() {
    var c1 =
        createCheckIn(
            "upsertByNaturalKey_differentDays",
            CheckInType.ORAL,
            LocalDate.now(),
            LocalTime.now(),
            CheckInStatus.PENDING);

    var c2 =
        createCheckIn(
            "upsertByNaturalKey_differentDays",
            CheckInType.ORAL,
            LocalDate.now().plusDays(1),
            LocalTime.now(),
            CheckInStatus.PENDING);

    checkInRepository.upsertByNaturalKey(c1);
    checkInRepository.upsertByNaturalKey(c1);
    checkInRepository.upsertByNaturalKey(c1);
    checkInRepository.upsertByNaturalKey(c1);
    checkInRepository.upsertByNaturalKey(c2);
    checkInRepository.upsertByNaturalKey(c2);
    checkInRepository.upsertByNaturalKey(c2);
    checkInRepository.upsertByNaturalKey(c2);

    var checkIns = checkInRepository.findByEnrollmentId("upsertByNaturalKey_differentDays");
    Assert.assertEquals(2, checkIns.size());
  }

  @Test
  public void upsertByNaturalKey_survey_instance() {
    var c1 =
        createCheckIn(
            "upsertByNaturalKey_survey_instance",
            CheckInType.ORAL,
            LocalDate.now(),
            LocalTime.now(),
            CheckInStatus.PENDING);

    var c2 =
        createCheckIn(
            "upsertByNaturalKey_survey_instance",
            CheckInType.ORAL,
            LocalDate.now(),
            LocalTime.now(),
            CheckInStatus.PENDING);
    c2.setSurveyInstanceId("upsertByNaturalKey_survey_instance");

    var c3 =
        createCheckIn(
            "upsertByNaturalKey_survey_instance",
            null,
            LocalDate.now(),
            LocalTime.now(),
            CheckInStatus.PENDING);
    c3.setSurveyInstanceId("upsertByNaturalKey_survey_instance different survey instance");

    checkInRepository.upsertByNaturalKey(c1);
    checkInRepository.upsertByNaturalKey(c1);
    checkInRepository.upsertByNaturalKey(c2);
    checkInRepository.upsertByNaturalKey(c2);
    checkInRepository.upsertByNaturalKey(c3);
    checkInRepository.upsertByNaturalKey(c3);

    var checkIns = checkInRepository.findByEnrollmentId("upsertByNaturalKey_survey_instance");
    Assert.assertEquals(2, checkIns.size());

    assertTrue(
        checkIns.stream()
            .anyMatch(c -> Objects.equals(c.getSurveyInstanceId(), c1.getSurveyInstanceId())));
    assertTrue(
        checkIns.stream()
            .anyMatch(c -> Objects.equals(c.getSurveyInstanceId(), c3.getSurveyInstanceId())));
  }

  @Test
  public void upsertByNaturalKey_existingCheckInWithCheckInType() {
    var c1 =
        createCheckIn(
            "upsertByNaturalKey_existingCheckInWithCheckInType",
            CheckInType.ORAL,
            LocalDate.now(),
            LocalTime.now(),
            CheckInStatus.PENDING);
    c1.setSurveyInstanceId("upsertByNaturalKey_existingCheckInWithCheckInType");

    var c2 =
        createCheckIn(
            "upsertByNaturalKey_existingCheckInWithCheckInType",
            null,
            LocalDate.now(),
            LocalTime.now(),
            CheckInStatus.PENDING);
    c2.setSurveyInstanceId("upsertByNaturalKey_existingCheckInWithCheckInType");

    checkInRepository.upsertByNaturalKey(c1);
    checkInRepository.upsertByNaturalKey(c1);
    checkInRepository.upsertByNaturalKey(c2);
    checkInRepository.upsertByNaturalKey(c2);

    var checkIns =
        checkInRepository.findByEnrollmentId("upsertByNaturalKey_existingCheckInWithCheckInType");
    Assert.assertEquals(1, checkIns.size());
  }

  @Test
  public void upsertByNaturalKey_bySurveyInstanceOnlyNoPending() {
    // this one should be ignored
    var c1 =
        createCheckIn(
            "upsertByNaturalKey_noCheckInType",
            null,
            LocalDate.now(),
            LocalTime.now(),
            CheckInStatus.COMPLETED);
    c1.setSurveyInstanceId("upsertByNaturalKey_survey_instance-testid");

    // these should be de-duped
    var c2 =
        createCheckIn(
            "upsertByNaturalKey_noCheckInType",
            null,
            LocalDate.now(),
            LocalTime.now(),
            CheckInStatus.PENDING);
    c2.setSurveyInstanceId("upsertByNaturalKey_survey_instance-testid");

    var c3 =
        createCheckIn(
            "upsertByNaturalKey_noCheckInType",
            null,
            LocalDate.now(),
            LocalTime.now(),
            CheckInStatus.PENDING);
    c3.setSurveyInstanceId("upsertByNaturalKey_survey_instance-testid");

    checkInRepository.upsertByNaturalKey(c1);
    checkInRepository.upsertByNaturalKey(c2);
    checkInRepository.upsertByNaturalKey(c3);

    var checkIns = checkInRepository.findByEnrollmentId("upsertByNaturalKey_noCheckInType");
    Assert.assertEquals(2, checkIns.size());
  }

  @Test
  public void upsertByNaturalKey_correctlyInserts() {
    var c1 =
        createCheckIn(
            "upsertByNaturalKey_correctlyInserts",
            CheckInType.ORAL,
            LocalDate.now(),
            // note: this value is truncated to millis when written to the db
            LocalTime.now().truncatedTo(ChronoUnit.MILLIS),
            CheckInStatus.PENDING);
    c1.setSurveyInstanceId("upsertByNaturalKey_correctlyInserts");
    c1.setCreatedReason(ReasonForCheckInCreation.SCHEDULED);
    c1.setPatientId(15L);
    c1.setClinicId(3L);
    c1.setCheckInScheduleId("upsertByNaturalKey_correctlyInserts");

    var updated = checkInRepository.upsertByNaturalKey(c1);
    var readin = checkInRepository.findById(updated.getId()).orElseThrow();
    assertEquals(c1.getScheduleDate(), updated.getScheduleDate());
    assertEquals(c1.getScheduleDate(), readin.getScheduleDate());
    Assert.assertEquals(c1, updated);
  }
}
