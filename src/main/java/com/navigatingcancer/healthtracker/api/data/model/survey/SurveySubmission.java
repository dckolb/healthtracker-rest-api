package com.navigatingcancer.healthtracker.api.data.model.survey;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class SurveySubmission {
  public static final String PRACTICE_CHECKIN_ID = "PRACTICE";

  public static final String DECLINE_A_CALL_QUESTION_ID = "declineACall";
  public static final String DECLINE_A_CALL_COMMENT_QUESTION_ID = "declineACallComment";
  public static final String MISSED_CHECK_INS_QUESTION_ID = "missedCheckIns";

  private static final ObjectMapper objectMapper = new ObjectMapper();

  @JsonProperty("checkInId")
  private String checkInId;

  @JsonProperty(value = "surveyPayload", defaultValue = "{}")
  private Map<String, Object> surveyPayload = new HashMap<>();

  @SuppressWarnings("unchecked")
  public <T> T getSurveyResponse(String k, Class<T> cls) {
    var val = surveyPayload.get(k);
    if (cls.isInstance(val)) {
      return (T) val;
    }

    return null;
  }

  public List<SurveyItemPayload> getMissedCheckIns() {
    var missedCheckIns =
        getSurveyResponse(SurveySubmission.MISSED_CHECK_INS_QUESTION_ID, List.class);

    var list = new ArrayList<SurveyItemPayload>();
    if (missedCheckIns != null && !missedCheckIns.isEmpty()) {
      for (var missedCheckIn : missedCheckIns) {
        if (missedCheckIn instanceof Map missedCheckInMap && missedCheckInMap.containsKey("id") && missedCheckInMap.containsKey("payload")) {
          list.add(objectMapper.convertValue(missedCheckInMap, SurveyItemPayload.class));
        }
      }
    }
    return list;
  }

  public boolean isDeclineACall() {
    var asList = getSurveyResponse(DECLINE_A_CALL_QUESTION_ID, List.class);
    if (asList != null) {
      return asList.contains("true");
    }

    return getSurveyResponse(DECLINE_A_CALL_QUESTION_ID, Boolean.class) == Boolean.TRUE;
  }

  public String getDeclineACallComment() {
    return getSurveyResponse(DECLINE_A_CALL_COMMENT_QUESTION_ID, String.class);
  }

  public boolean isPracticeCheckIn() {
    return PRACTICE_CHECKIN_ID.equals(this.checkInId);
  }
}
