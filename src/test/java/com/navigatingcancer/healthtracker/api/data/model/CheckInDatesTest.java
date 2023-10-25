package com.navigatingcancer.healthtracker.api.data.model;

import static org.junit.Assert.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.junit.Before;
import org.junit.Test;

public class CheckInDatesTest {
  private Enrollment enrollment;
  private CheckInSchedule oralSchedule;
  private CheckInSchedule symptomSchedule;
  private List<CheckIn> checkIns;
  private LocalDateTime expectedNextEnrollmentCheckInDate;
  private LocalDateTime expectedLastEnrollmentCheckInDate;
  private LocalDateTime expectedNextOralCheckInDate;
  private LocalDateTime expectedLastOralCheckInDate;
  private LocalDateTime expectedNextSymptomCheckInDate;
  private LocalDateTime expectedLastSymptomCheckInDate;

  @Before
  public void setUp() {
    enrollment = new Enrollment();
    enrollment.setId("enrollment_id");
    enrollment.setReminderTime("12:00");
    enrollment.setReminderTimeZone("America/Los_Angeles");
    enrollment.setDaysInCycle(3);

    oralSchedule = new CheckInSchedule();
    oralSchedule.setId("oral_schedule_id");
    oralSchedule.setCheckInType(CheckInType.ORAL);
    oralSchedule.setCheckInFrequency(CheckInFrequency.DAILY);

    symptomSchedule = new CheckInSchedule();
    symptomSchedule.setId("symptom_schedule_id");
    symptomSchedule.setCheckInType(CheckInType.SYMPTOM);
    symptomSchedule.setCheckInFrequency(CheckInFrequency.DAILY);

    enrollment.setSchedules(new ArrayList<>(List.of(oralSchedule, symptomSchedule)));

    CheckIn nextSymptomCheckIn;
    CheckIn lastSymptomCheckIn;
    CheckIn lastOralCheckIn;
    checkIns = new ArrayList<>();
    checkIns.addAll(
        List.of(
            nextSymptomCheckIn =
                createCheckIn(
                    enrollment,
                    symptomSchedule,
                    CheckInStatus.PENDING,
                    0), // next check-in for enrollment
            lastSymptomCheckIn =
                createCheckIn(
                    enrollment,
                    symptomSchedule,
                    CheckInStatus.MISSED,
                    -2), // last check-in this schedule
            createCheckIn(enrollment, symptomSchedule, CheckInStatus.COMPLETED, -3),
            createCheckIn(enrollment, symptomSchedule, CheckInStatus.MISSED, -4),

            // no pending check-in for oral -- must be calculated according to schedule
            lastOralCheckIn =
                createCheckIn(
                    enrollment,
                    oralSchedule,
                    CheckInStatus.COMPLETED,
                    -1), // last check-in for enrollment and this schedule
            createCheckIn(enrollment, oralSchedule, CheckInStatus.MISSED, -3)));

    expectedLastSymptomCheckInDate = lastSymptomCheckIn.getScheduleDateTime();
    expectedNextSymptomCheckInDate = nextSymptomCheckIn.getScheduleDateTime();

    expectedLastOralCheckInDate = lastOralCheckIn.getScheduleDateTime();
    expectedNextOralCheckInDate =
        LocalDateTime.of(
            LocalDate.now(ZoneId.of(enrollment.getReminderTimeZone())).plusDays(0),
            LocalTime.of(12, 0));

    expectedNextEnrollmentCheckInDate = expectedNextSymptomCheckInDate;
    expectedLastEnrollmentCheckInDate = expectedLastOralCheckInDate;
  }

  @Test
  public void forEnrollment() {
    var dates = CheckInDates.forEnrollment(enrollment, checkIns);
    assertEquals(expectedNextEnrollmentCheckInDate, dates.getNextCheckInDate());
    assertEquals(expectedLastEnrollmentCheckInDate, dates.getLastCheckInDate());
  }

  @Test
  public void forCheckInType() {
    var dates = CheckInDates.forCheckInType(enrollment, CheckInType.ORAL, checkIns);
    assertEquals(expectedNextOralCheckInDate, dates.getNextCheckInDate());
    assertEquals(expectedLastOralCheckInDate, dates.getLastCheckInDate());
  }

  @Test
  public void forSchedule() {
    var filteredCheckIns =
        checkIns.stream()
            .filter(c -> Objects.equals(c.getCheckInScheduleId(), symptomSchedule.getId()))
            .toList();
    var dates = CheckInDates.forSchedule(enrollment, symptomSchedule, filteredCheckIns);
    assertEquals(expectedNextSymptomCheckInDate, dates.getNextCheckInDate());
    assertEquals(expectedLastSymptomCheckInDate, dates.getLastCheckInDate());
  }

  @Test
  public void forSchedule_endedWithNoCheckInType() {
    var schedule = new CheckInSchedule();
    schedule.setId("ended_schedule_without_check_in_type");
    schedule.setCheckInFrequency(CheckInFrequency.DAILY);
    schedule.setEndDate(LocalDate.now(ZoneId.of("America/Los_Angeles")).minusDays(1));

    enrollment.getSchedules().add(schedule);

    CheckIn lastCheckIn;
    checkIns.addAll(
        List.of(
            lastCheckIn = createCheckIn(enrollment, schedule, CheckInStatus.COMPLETED, -2),
            createCheckIn(enrollment, schedule, CheckInStatus.COMPLETED, -3),
            createCheckIn(enrollment, schedule, CheckInStatus.COMPLETED, -4),
            createCheckIn(enrollment, schedule, CheckInStatus.COMPLETED, -5),
            createCheckIn(enrollment, schedule, CheckInStatus.COMPLETED, -6),
            createCheckIn(enrollment, schedule, CheckInStatus.COMPLETED, -7)));

    var dates = CheckInDates.forSchedule(enrollment, schedule, checkIns);
    assertNull(dates.getNextCheckInDate());
    assertEquals(lastCheckIn.getScheduleDateTime(), dates.getLastCheckInDate());
  }

  @Test
  public void forEnrollment_discardsCheckInsWithNoScheduleDate() {
    CheckIn badCompletedCheckIn =
        createCheckIn(enrollment, oralSchedule, CheckInStatus.COMPLETED, 0);
    CheckIn badPendingCheckIn = createCheckIn(enrollment, oralSchedule, CheckInStatus.PENDING, 0);
    badCompletedCheckIn.setScheduleDate(null);
    badPendingCheckIn.setScheduleDate(null);

    checkIns.add(badCompletedCheckIn);
    checkIns.add(badPendingCheckIn);

    var dates = CheckInDates.forEnrollment(enrollment, checkIns);
    assertEquals(expectedNextEnrollmentCheckInDate, dates.getNextCheckInDate());
    assertEquals(expectedLastEnrollmentCheckInDate, dates.getLastCheckInDate());
  }

  private static CheckIn createCheckIn(
      Enrollment enrollment, CheckInSchedule schedule, CheckInStatus status, int offset) {
    var checkIn = new CheckIn();
    checkIn.setEnrollmentId(enrollment.getId());
    checkIn.setStatus(status);
    checkIn.setCheckInScheduleId(schedule.getId());
    checkIn.setCheckInType(schedule.getCheckInType());

    checkIn.setScheduleDate(LocalDate.now(ZoneId.of("America/Los_Angeles")).plusDays(offset));
    checkIn.setScheduleTime(LocalTime.of(12, 0));
    return checkIn;
  }
}
