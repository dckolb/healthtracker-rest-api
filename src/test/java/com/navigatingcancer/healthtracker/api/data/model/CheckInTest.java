package com.navigatingcancer.healthtracker.api.data.model;

import static org.junit.Assert.*;

import com.navigatingcancer.healthtracker.api.data.model.survey.SurveyId;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.junit.Test;

public class CheckInTest {

  @Test
  public void compareTo_sameSurveyPrecedence() {
    CheckIn c1 = new CheckIn();
    c1.setSurveyId(SurveyId.ORAL_ADHERENCE_PX);

    CheckIn c2 = new CheckIn();
    c2.setSurveyId(SurveyId.ORAL_ADHERENCE_PX);

    assertEquals(0, c1.compareTo(c2));
    assertEquals(0, c2.compareTo(c1));
  }

  @Test
  public void compareTo_surveyPrecedence() {
    CheckIn c1 = new CheckIn();
    c1.setSurveyId(SurveyId.ORAL_ADHERENCE_PX);

    CheckIn c2 = new CheckIn();
    c2.setSurveyId(SurveyId.HEALTH_TRACKER_PX);

    assertTrue(c1.compareTo(c2) < 0);
    assertTrue(c2.compareTo(c1) > 0);
  }

  @Test
  public void compareTo_unknownSurveyPrecedence() {
    CheckIn c1 = new CheckIn();
    c1.setSurveyId(SurveyId.ORAL_ADHERENCE_PX);

    CheckIn c2 = new CheckIn();
    c2.setSurveyId("some_unknown_survey");

    assertTrue(c1.compareTo(c2) < 0);
    assertTrue(c2.compareTo(c1) > 0);
  }

  @Test
  public void getScheduleDateTime_nullConstituents() {
    CheckIn c1 = new CheckIn();
    assertNull(c1.getScheduleDateTime());
  }

  @Test
  public void getScheduleDateTime_nullScheduleDate() {
    CheckIn c1 = new CheckIn();
    c1.setScheduleTime(LocalTime.now());
    assertNull(c1.getScheduleDateTime());
  }

  @Test
  public void getScheduleDateTime_nullScheduleTime() {
    CheckIn c1 = new CheckIn();
    c1.setScheduleDate(LocalDate.now());
    assertNull(c1.getScheduleDateTime());
  }

  @Test
  public void getScheduleDateTime_nonNullConstituents() {
    LocalDate nowDate = LocalDate.now();
    LocalTime nowTime = LocalTime.now();
    CheckIn c1 = new CheckIn();
    c1.setScheduleDate(nowDate);
    c1.setScheduleTime(nowTime);

    assertEquals(LocalDateTime.of(nowDate, nowTime), c1.getScheduleDateTime());
  }
}
