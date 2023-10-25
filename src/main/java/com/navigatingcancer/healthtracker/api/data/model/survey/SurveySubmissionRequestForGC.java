package com.navigatingcancer.healthtracker.api.data.model.survey;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.CaseFormat;
import com.navigatingcancer.healthtracker.api.rest.exception.BadDataException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class SurveySubmissionRequestForGC {
  @JsonProperty("survey_submission")
  Map<String, Object> submission;

  @JsonProperty("survey_version")
  String surveyVersion;

  @JsonProperty("survey_type")
  String surveyType;

  public void setSubmission() {}

  public SurveySubmissionRequestForGC(
      Map<String, Object> surveyJsPayload, String surveyId, Long patientId)
      throws BadDataException {
    switch (surveyId) {
      case SurveyId.NCCN_2022_DISTRESS:
        setSurveyType("distress");
        setSurveyVersion("2022");
        break;
      default:
        throw new BadDataException(String.format("unknown surveyId for gc: %s", surveyId));
    }

    this.submission = flattenJsPayload(surveyJsPayload);
    this.submission.put("patient_id", patientId);
  }

  /*
   Takes the json payload returned by surveyJS and flattens it into what GC expects.
   The example payload:
   {
      "overallDistress": "3",
      "emotionalConcerns": [
        "emotional_anxiety",
        "emotional_depression",
        "emotional_loss_of_enjoyment"
      ],
      "otherConcerns": "asdf"
   }

   would return:
   {
      "emotional_anxiety": true,
      "emotional_depression": true,
      "emotional_loss_of_enjoyment": true,
      "patient_id": 1,
      "overall_distress": 10,
      "other_concerns": "asdf",
      "survey_mode": "Employee"
   }
  */
  private Map<String, Object> flattenJsPayload(Map<String, Object> payload) {
    Map<String, Object> flatMap = new HashMap<String, Object>();
    payload
        .entrySet()
        .forEach(
            entry -> {
              if (entry.getValue() instanceof List) {
                List<?> selectedValues = (List<Object>) entry.getValue();
                selectedValues.forEach(
                    selectedValue -> {
                      if (selectedValue == null) return;
                      // Don't need to convert case here since they are already in snake case
                      flatMap.put(selectedValue.toString(), true);
                    });
              } else {
                flatMap.put(camelCaseToSnakeCase(entry.getKey()), entry.getValue());
              }
            });

    return flatMap;
  }

  private String camelCaseToSnakeCase(String str) {
    return CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, str);
  }
}
