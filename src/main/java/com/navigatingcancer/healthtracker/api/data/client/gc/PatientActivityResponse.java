package com.navigatingcancer.healthtracker.api.data.client.gc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import lombok.Data;

@Data
@JsonTypeName(value = "patient_activity")
@JsonTypeInfo(include = JsonTypeInfo.As.WRAPPER_OBJECT, use = JsonTypeInfo.Id.NAME)
@JsonIgnoreProperties(ignoreUnknown = true)
public class PatientActivityResponse {
  @JsonProperty("id")
  private Long id;

  @JsonProperty("created_at")
  private String createdAt;

  @JsonProperty("created_by")
  private String createdBy;

  @JsonProperty("selected_actions")
  private List<String> actions;

  @JsonProperty("notes")
  private String notes;

  @JsonProperty("in_person")
  private boolean inPerson;

  @JsonProperty("action_minutes")
  private int minutes;

  @JsonProperty("patient_id")
  private Long patientId;
}
