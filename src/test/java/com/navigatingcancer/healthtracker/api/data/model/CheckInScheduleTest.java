package com.navigatingcancer.healthtracker.api.data.model;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.LocalDate;
import java.util.Calendar;
import java.util.List;
import org.junit.Test;

public class CheckInScheduleTest {
  @Test
  public void givenMatchingCheckInTypesAndNullMedications_shouldMatch() {
    CheckInSchedule schedule = new CheckInSchedule();
    schedule.setCheckInType(CheckInType.ORAL);
    schedule.setMedication(null);
    schedule.setCheckInFrequency(CheckInFrequency.DAILY);

    CheckInSchedule otherSchedule = new CheckInSchedule();
    otherSchedule.setCheckInType(CheckInType.ORAL);
    otherSchedule.setMedication(null);
    schedule.setCheckInFrequency(CheckInFrequency.CUSTOM); // checking only meds and type

    assertTrue(schedule.matchesTypeAndMedication(otherSchedule));
  }

  @Test
  public void givenMatchingCheckInTypesAndMatchingMedications_shouldMatch() {
    CheckInSchedule schedule = new CheckInSchedule();
    schedule.setCheckInType(CheckInType.ORAL);
    schedule.setMedication("MED");
    schedule.setCheckInFrequency(CheckInFrequency.DAILY);

    CheckInSchedule otherSchedule = new CheckInSchedule();
    otherSchedule.setCheckInType(CheckInType.ORAL);
    otherSchedule.setMedication("Med");
    schedule.setCheckInFrequency(CheckInFrequency.CUSTOM); // checking only meds and type

    assertTrue(schedule.matchesTypeAndMedication(otherSchedule));
  }

  @Test
  public void givenDifferingCheckInTypesAndNullMedications_shouldNotMatch() {
    CheckInSchedule schedule = new CheckInSchedule();
    schedule.setCheckInType(CheckInType.ORAL);
    schedule.setMedication(null);
    schedule.setCheckInFrequency(CheckInFrequency.DAILY);

    CheckInSchedule otherSchedule = new CheckInSchedule();
    otherSchedule.setCheckInType(CheckInType.SYMPTOM);
    otherSchedule.setMedication(null);
    schedule.setCheckInFrequency(CheckInFrequency.DAILY);

    assertFalse(schedule.matchesTypeAndMedication(otherSchedule));
  }

  @Test
  public void givenDifferingCheckInTypesAndDifferingMedications_NotMatch() {
    CheckInSchedule schedule = new CheckInSchedule();
    schedule.setCheckInType(CheckInType.ORAL);
    schedule.setMedication("FOO");

    CheckInSchedule otherSchedule = new CheckInSchedule();
    otherSchedule.setCheckInType(CheckInType.SYMPTOM);
    otherSchedule.setMedication("BAR");

    assertFalse(schedule.matchesTypeAndMedication(otherSchedule));
  }

  @Test
  public void givenDifferingSchedules_NotMatch() {
    CheckInSchedule schedule = new CheckInSchedule();
    schedule.setCheckInType(CheckInType.ORAL);
    schedule.setMedication("FOO");

    CheckInSchedule otherSchedule = new CheckInSchedule();
    otherSchedule.setCheckInType(CheckInType.ORAL);
    otherSchedule.setMedication("FOO");

    // Just the schedule type
    schedule.setCheckInFrequency(CheckInFrequency.DAILY);
    otherSchedule.setCheckInFrequency(CheckInFrequency.CUSTOM);
    assertFalse(schedule.matches(otherSchedule));

    // Same type, same days
    schedule.setCheckInFrequency(CheckInFrequency.DAILY);
    otherSchedule.setCheckInFrequency(CheckInFrequency.DAILY);
    LocalDate testDate = LocalDate.of(2020, 01, 10);
    schedule.setCheckInDays(List.of(testDate.plusDays(1), testDate.plusDays(2)));
    otherSchedule.setCheckInDays(List.of(testDate.plusDays(1), testDate.plusDays(2)));
    schedule.setCycleDays(List.of(1, 2, 3, 4));
    otherSchedule.setCycleDays(List.of(1, 2, 3, 4));
    schedule.setWeeklyDays(List.of(1, 2, 3, 4));
    otherSchedule.setWeeklyDays(List.of(1, 2, 3, 4));
    assertTrue(schedule.matches(otherSchedule));

    // Different days
    schedule.setCheckInDays(
        List.of(testDate.plusDays(1), testDate.plusDays(2), testDate.plusDays(3)));
    assertFalse(schedule.matches(otherSchedule));

    // Different days
    schedule.setCheckInDays(List.of(testDate.plusDays(1), testDate.plusDays(2)));
    schedule.setCycleDays(List.of(1, 2, 3, 4, 5));
    assertFalse(schedule.matches(otherSchedule));

    // Different days
    schedule.setCycleDays(List.of(1, 2, 3, 4));
    schedule.setWeeklyDays(List.of(1));
    assertFalse(schedule.matches(otherSchedule));
  }

  @Test
  public void isScheduled_dailyFrequency() {
    var schedule = new CheckInSchedule();
    schedule.setStartDate(LocalDate.now());
    schedule.setEndDate(LocalDate.now().plusDays(2));
    schedule.setCheckInFrequency(CheckInFrequency.DAILY);

    assertFalse(schedule.isScheduled(LocalDate.now().minusDays(1), 1));
    assertTrue(schedule.isScheduled(LocalDate.now(), 1));
    assertTrue(schedule.isScheduled(LocalDate.now().plusDays(1), 1));
    assertTrue(schedule.isScheduled(LocalDate.now().plusDays(2), 1));
    assertFalse(schedule.isScheduled(LocalDate.now().plusDays(3), 1));
  }

  @Test
  public void isScheduled_weekdayFrequency() {
    var start = LocalDate.parse("2023-08-07"); // happy monday
    var end = start.plusDays(6);
    var schedule = new CheckInSchedule();
    schedule.setStartDate(start);
    schedule.setEndDate(end);
    schedule.setCheckInFrequency(CheckInFrequency.WEEKDAY);

    assertTrue(schedule.isScheduled(start)); // monday
    assertTrue(schedule.isScheduled(start.plusDays(1))); // tuesday
    assertTrue(schedule.isScheduled(start.plusDays(2))); // wednesday
    assertTrue(schedule.isScheduled(start.plusDays(3))); // thursday
    assertTrue(schedule.isScheduled(start.plusDays(4))); // friday
    assertFalse(schedule.isScheduled(start.plusDays(5))); // saturday
    assertFalse(schedule.isScheduled(start.plusDays(6))); // sunday
  }

  @Test
  public void isScheduled_weeklyFrequency() {
    var start = LocalDate.parse("2023-08-07"); // happy monday
    var end = start.plusDays(13);
    var schedule = new CheckInSchedule();
    schedule.setStartDate(start);
    schedule.setEndDate(end);
    schedule.setCheckInFrequency(CheckInFrequency.WEEKLY);
    schedule.setWeeklyDays(List.of(Calendar.MONDAY, Calendar.TUESDAY));

    assertTrue(schedule.isScheduled(start)); // monday
    assertTrue(schedule.isScheduled(start.plusDays(1))); // tuesday
    assertFalse(schedule.isScheduled(start.plusDays(2))); // wednesday
    assertFalse(schedule.isScheduled(start.plusDays(3))); // thursday
    assertFalse(schedule.isScheduled(start.plusDays(4))); // friday
    assertFalse(schedule.isScheduled(start.plusDays(5))); // saturday
    assertFalse(schedule.isScheduled(start.plusDays(6))); // sunday
    assertTrue(schedule.isScheduled(start.plusDays(7))); // monday
    assertTrue(schedule.isScheduled(start.plusDays(8))); // tuesday
  }

  @Test
  public void isScheduled_customFrequency() {
    var start = LocalDate.parse("2023-07-26");
    var end = start.plusDays(5);
    var schedule = new CheckInSchedule();
    schedule.setStartDate(start);
    schedule.setEndDate(end);
    schedule.setCheckInFrequency(CheckInFrequency.CUSTOM);
    schedule.setDaysInCycle(5);

    // note: the cycle days are 1-indexed
    schedule.setCycleDays(List.of(1, 2, 3, 4, 5));

    assertFalse(schedule.isScheduled(start.minusDays(1)));
    assertFalse(schedule.isScheduled(end.plusDays(1)));

    for (int i = 0; i < schedule.getDaysInCycle(); i++) {
      assertTrue(
          String.format("cycle day %d is scheduled", i + 1),
          schedule.isScheduled(start.plusDays(i)));
    }
  }
}
