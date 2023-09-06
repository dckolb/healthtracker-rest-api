package com.navigatingcancer.healthtracker.api.data.model;

import java.time.LocalDate;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class CheckInScheduleTest {
  @Test
  public void givenMatchingCheckInTypesAndNullMedications_shouldMatch(){
    CheckInSchedule schedule = new CheckInSchedule();
    schedule.setCheckInType(CheckInType.ORAL);
    schedule.setMedication(null);
    schedule.setCheckInFrequency(CheckInFrequency.DAILY);

    CheckInSchedule otherSchedule = new CheckInSchedule();
    otherSchedule.setCheckInType(CheckInType.ORAL);
    otherSchedule.setMedication(null);
    schedule.setCheckInFrequency(CheckInFrequency.CUSTOM); // checking only meds and type

    Assert.assertTrue(schedule.matchesTypeAndMedication(otherSchedule));
  }

  @Test
  public void givenMatchingCheckInTypesAndMatchingMedications_shouldMatch(){
    CheckInSchedule schedule = new CheckInSchedule();
    schedule.setCheckInType(CheckInType.ORAL);
    schedule.setMedication("MED");
    schedule.setCheckInFrequency(CheckInFrequency.DAILY);

    CheckInSchedule otherSchedule = new CheckInSchedule();
    otherSchedule.setCheckInType(CheckInType.ORAL);
    otherSchedule.setMedication("Med");
    schedule.setCheckInFrequency(CheckInFrequency.CUSTOM); // checking only meds and type

    Assert.assertTrue(schedule.matchesTypeAndMedication(otherSchedule));
  }

  @Test
  public void givenDifferingCheckInTypesAndNullMedications_shouldNotMatch(){
    CheckInSchedule schedule = new CheckInSchedule();
    schedule.setCheckInType(CheckInType.ORAL);
    schedule.setMedication(null);
    schedule.setCheckInFrequency(CheckInFrequency.DAILY);

    CheckInSchedule otherSchedule = new CheckInSchedule();
    otherSchedule.setCheckInType(CheckInType.SYMPTOM);
    otherSchedule.setMedication(null);
    schedule.setCheckInFrequency(CheckInFrequency.DAILY);

    Assert.assertFalse(schedule.matchesTypeAndMedication(otherSchedule));
  }

  @Test
  public void givenDifferingCheckInTypesAndDifferingMedications_NotMatch(){
    CheckInSchedule schedule = new CheckInSchedule();
    schedule.setCheckInType(CheckInType.ORAL);
    schedule.setMedication("FOO");

    CheckInSchedule otherSchedule = new CheckInSchedule();
    otherSchedule.setCheckInType(CheckInType.SYMPTOM);
    otherSchedule.setMedication("BAR");

    Assert.assertFalse(schedule.matchesTypeAndMedication(otherSchedule));
  }

  @Test
  public void givenDifferingSchedules_NotMatch(){
    CheckInSchedule schedule = new CheckInSchedule();
    schedule.setCheckInType(CheckInType.ORAL);
    schedule.setMedication("FOO");

    CheckInSchedule otherSchedule = new CheckInSchedule();
    otherSchedule.setCheckInType(CheckInType.ORAL);
    otherSchedule.setMedication("FOO");

    // Just the schedule type
    schedule.setCheckInFrequency(CheckInFrequency.DAILY);
    otherSchedule.setCheckInFrequency(CheckInFrequency.CUSTOM);
    Assert.assertFalse(schedule.matches(otherSchedule));

    // Same type, same days
    schedule.setCheckInFrequency(CheckInFrequency.DAILY);
    otherSchedule.setCheckInFrequency(CheckInFrequency.DAILY);
    LocalDate testDate = LocalDate.of(2020, 01, 10);
    schedule.setCheckInDays(List.of(testDate.plusDays(1),testDate.plusDays(2)));
    otherSchedule.setCheckInDays(List.of(testDate.plusDays(1),testDate.plusDays(2)));
    schedule.setCycleDays(List.of(1,2,3,4));
    otherSchedule.setCycleDays(List.of(1,2,3,4));
    schedule.setWeeklyDays(List.of(1,2,3,4));
    otherSchedule.setWeeklyDays(List.of(1,2,3,4));
    Assert.assertTrue(schedule.matches(otherSchedule));

    // Different days
    schedule.setCheckInDays(List.of(testDate.plusDays(1),testDate.plusDays(2), testDate.plusDays(3)));
    Assert.assertFalse(schedule.matches(otherSchedule));

    // Different days
    schedule.setCheckInDays(List.of(testDate.plusDays(1),testDate.plusDays(2)));
    schedule.setCycleDays(List.of(1,2,3,4,5));
    Assert.assertFalse(schedule.matches(otherSchedule));

    // Different days
    schedule.setCycleDays(List.of(1,2,3,4));
    schedule.setWeeklyDays(List.of(1));
    Assert.assertFalse(schedule.matches(otherSchedule));
  }

}

