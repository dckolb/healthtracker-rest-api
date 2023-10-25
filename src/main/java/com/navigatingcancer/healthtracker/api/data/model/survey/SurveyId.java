package com.navigatingcancer.healthtracker.api.data.model.survey;

import java.util.List;

public class SurveyId {

  public static final String HEALTH_TRACKER_CX = "5eb1b09b24992c0fe1779085";
  public static final String HEALTH_TRACKER_PX = "5ef3cc9c296b54c5bc8af1d2";
  public static final String ORAL_ADHERENCE_CX = "5eb2461624992c0fe1779088";
  public static final String ORAL_ADHERENCE_PX = "5ef3ccd0296b54c5bc8af1d3";
  public static final String ORAL_ADHERENCE_HT_CX = "5eb23a0924992c0fe1779087";
  public static final String ORAL_ADHERENCE_HT_PX = "5ef503f66e236d2a8797613d";
  public static final String PROCTCAE_CX = "5eb1fc9824992c0fe1779086";
  public static final String PROCTCAE_PX = "5ef28c0b5931f51c58ed6ff7";
  public static final String NCCN_2022_DISTRESS = "64a3512b9b0da6f25df8fa00";

  public static final List<String> PREFERRED_SURVEY_ORDER =
      List.of(
          NCCN_2022_DISTRESS,
          ORAL_ADHERENCE_HT_PX,
          ORAL_ADHERENCE_HT_CX,
          ORAL_ADHERENCE_PX,
          ORAL_ADHERENCE_CX,
          HEALTH_TRACKER_PX,
          HEALTH_TRACKER_CX,
          PROCTCAE_PX,
          PROCTCAE_CX);

  public static boolean isOralSurvey(String surveyId) {
    return ORAL_ADHERENCE_CX.equals(surveyId) || ORAL_ADHERENCE_PX.equals(surveyId);
  }

  public static boolean isSymptomSurvey(String surveyId) {
    return HEALTH_TRACKER_CX.equals(surveyId)
        || HEALTH_TRACKER_PX.equals(surveyId)
        || PROCTCAE_CX.equals(surveyId)
        || PROCTCAE_PX.equals(surveyId);
  }

  public static boolean isClinicCollect(String surveyId) {
    return HEALTH_TRACKER_CX.equals(surveyId)
        || PROCTCAE_CX.equals(surveyId)
        || ORAL_ADHERENCE_HT_CX.equals(surveyId)
        || ORAL_ADHERENCE_CX.equals(surveyId);
  }

  public static boolean isValid(String surveyId) {
    return PREFERRED_SURVEY_ORDER.contains(surveyId);
  }
}
