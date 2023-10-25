package com.navigatingcancer.healthtracker.api.data.model.survey;

import com.navigatingcancer.healthtracker.api.rest.exception.BadDataException;
import java.util.HashMap;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

public class SurveySubmissionRequestForGCTest {

  @Test
  public void SurveySubmissionRequestForGCConstructor_flattensJS() {
    var surveyJsPayload = new HashMap<String, Object>();
    surveyJsPayload.put("overallDistress", "3");
    surveyJsPayload.put(
        "emotionalConcerns",
        List.of("emotional_anxiety", "emotional_depression", "emotional_loss_of_enjoyment"));

    surveyJsPayload.put("otherConcerns", "asdf");
    Long patientId = 4l;
    var flattenedPayload =
        new SurveySubmissionRequestForGC(surveyJsPayload, SurveyId.NCCN_2022_DISTRESS, patientId);

    Assert.assertEquals("3", flattenedPayload.getSubmission().get("overall_distress"));
    Assert.assertEquals(true, flattenedPayload.getSubmission().get("emotional_depression"));
    Assert.assertEquals(patientId, flattenedPayload.getSubmission().get("patient_id"));
    Assert.assertEquals("distress", flattenedPayload.getSurveyType());
  }

  @Test
  public void SurveySubmissionRequestForGCConstructor_throwsForUnsupportedSurveyId() {
    var surveyJsPayload = new HashMap<String, Object>();
    String surveyId = "testId";
    Long patientId = 4l;

    Assert.assertThrows(
        BadDataException.class,
        () -> new SurveySubmissionRequestForGC(surveyJsPayload, surveyId, patientId));
  }
}
