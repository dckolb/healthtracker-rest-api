package com.navigatingcancer.healthtracker.api.data.model.survey;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SurveyPayloadContent {

  @JsonProperty("enrollmentId")
  String enrollmentId;

  @JsonProperty("medication")
  List<SurveyItemPayload> oral;

  @JsonProperty("symptoms")
  List<SurveyItemPayload> symptoms;
}
