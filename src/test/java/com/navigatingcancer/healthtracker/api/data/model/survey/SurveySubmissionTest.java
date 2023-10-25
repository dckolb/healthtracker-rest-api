package com.navigatingcancer.healthtracker.api.data.model.survey;

import static org.junit.Assert.*;

import java.util.List;
import java.util.Map;
import org.junit.Test;

public class SurveySubmissionTest {

  @Test
  public void getSurveyResponse_returnsNullForMissingResponse() {
    var submission = new SurveySubmission();
    assertNull(submission.getSurveyResponse("not available", Object.class));
  }

  @Test
  public void getSurveyResponse_returnsValue() {
    var submission = new SurveySubmission();
    submission.setSurveyPayload(Map.of("foo", "bar"));
    assertEquals("bar", submission.getSurveyResponse("foo", Object.class));
    assertEquals("bar", submission.getSurveyResponse("foo", String.class));
  }

  @Test
  public void getSurveyResponse_returnsValueWorksForMaps() {
    var submission = new SurveySubmission();
    submission.setSurveyPayload(Map.of("foo", Map.of("baz", "quux")));
    assertEquals(Map.of("baz", "quux"), submission.getSurveyResponse("foo", Object.class));
    assertEquals(Map.of("baz", "quux"), submission.getSurveyResponse("foo", Map.class));
  }

  @Test
  public void getMissedCheckIns_emptyWhenNotSet() {
    var submission = new SurveySubmission();
    assertTrue(submission.getMissedCheckIns().isEmpty());
  }

  @Test
  public void getMissedCheckIns_notEmptyWhenSet() {
    var submission = new SurveySubmission();
    submission.setSurveyPayload(
        Map.of(
            SurveySubmission.MISSED_CHECK_INS_QUESTION_ID,
            List.of(Map.of("id", "checkInId", "payload", Map.of("medicationTaken", true)))));

    assertEquals(1, submission.getMissedCheckIns().size());

    var missedCheckIn = submission.getMissedCheckIns().get(0);
    assertEquals("checkInId", missedCheckIn.getId());
    assertEquals(Map.of("medicationTaken", true), missedCheckIn.getPayload());
  }

  @Test
  public void getMissedCheckIns_emptyWhenInvalid() {
    var submission = new SurveySubmission();
    submission.setSurveyPayload(
        Map.of(SurveySubmission.MISSED_CHECK_INS_QUESTION_ID, List.of(Map.of())));
    assertTrue(submission.getMissedCheckIns().isEmpty());
  }

  @Test
  public void isDeclineACall_falseWhenNotSet() {
    var submission = new SurveySubmission();
    assertFalse(submission.isDeclineACall());
  }

  @Test
  public void isDeclineACall_trueWhenBoolTrue() {
    var submission = new SurveySubmission();
    submission.setSurveyPayload(Map.of(SurveySubmission.DECLINE_A_CALL_QUESTION_ID, true));
    assertTrue(submission.isDeclineACall());
  }

  @Test
  public void isDeclineACall_falseWhenBoolFalse() {
    var submission = new SurveySubmission();
    submission.setSurveyPayload(Map.of(SurveySubmission.DECLINE_A_CALL_QUESTION_ID, false));
    assertFalse(submission.isDeclineACall());
  }

  @Test
  public void isDeclineACall_trueWhenStringListWithTrue() {
    var submission = new SurveySubmission();
    submission.setSurveyPayload(
        Map.of(SurveySubmission.DECLINE_A_CALL_QUESTION_ID, List.of("true")));
    assertTrue(submission.isDeclineACall());
  }

  @Test
  public void isDeclineACall_falseWhenStringListWithoutTrue() {
    var submission = new SurveySubmission();
    submission.setSurveyPayload(
        Map.of(SurveySubmission.DECLINE_A_CALL_QUESTION_ID, List.of("nope")));
    assertFalse(submission.isDeclineACall());
  }

  @Test
  public void getDeclineACallComment() {
    var submission = new SurveySubmission();
    submission.setSurveyPayload(
        Map.of(SurveySubmission.DECLINE_A_CALL_COMMENT_QUESTION_ID, "comment"));
    assertEquals("comment", submission.getDeclineACallComment());
  }

  @Test
  public void getDeclineACallComment_nullWhenNotSet() {
    var submission = new SurveySubmission();
    assertNull(submission.getDeclineACallComment());
  }

  @Test
  public void isPracticeCheckIn_false() {
    var submission = new SurveySubmission();
    submission.setCheckInId("any old check-in");
    assertFalse(submission.isPracticeCheckIn());
  }

  @Test
  public void isPracticeCheckIn_true() {
    var submission = new SurveySubmission();
    submission.setCheckInId(SurveySubmission.PRACTICE_CHECKIN_ID);
    assertTrue(submission.isPracticeCheckIn());
  }
}
