package com.navigatingcancer.healthtracker.api.data.model.survey;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SurveyPayload {

  public SurveyPayload() {
    this.content = new SurveyPayloadContent();
  }

  @JsonProperty("surveyPayload")
  public SurveyPayloadContent content;

  public static final String SURVEY_PAYLOAD_SEVERITY_SUBTYPE = "severity";
  public static final String SURVEY_PAYLOAD_INTERFERENCE_SUBTYPE = "interference";
  public static final String SURVEY_PAYLOAD_COMMENT_SUBTYPE = "comment";
  public static final String SURVEY_PAYLOAD_FREQUENCY_SUBTYPE = "frequency";

  @JsonIgnore
  public List<SurveyItemPayload> getOral() {
    if (this.content != null) return this.content.oral;
    else return null;
  }

  @JsonIgnore
  public List<SurveyItemPayload> getSymptoms() {
    if (this.content != null) return this.content.symptoms;
    else return null;
  }

  @JsonIgnore
  public String getEnrollmentId() {
    if (this.content != null) return this.content.enrollmentId;
    else return null;
  }

  @JsonIgnore
  public void setOral(List<SurveyItemPayload> oral) {
    if (this.content == null) {
      this.content = new SurveyPayloadContent();
    }
    this.content.setOral(oral);
  }

  @JsonIgnore
  public void setSymptoms(List<SurveyItemPayload> symptoms) {
    if (this.content == null) {
      this.content = new SurveyPayloadContent();
    }
    this.content.setSymptoms(symptoms);
  }

  @JsonIgnore
  public void setEnrollmentId(String enrollmentId) {
    if (this.content == null) {
      this.content = new SurveyPayloadContent();
    }
    this.content.setEnrollmentId(enrollmentId);
  }
}
