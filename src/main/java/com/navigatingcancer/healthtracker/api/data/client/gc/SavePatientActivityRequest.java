package com.navigatingcancer.healthtracker.api.data.client.gc;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import lombok.Data;

@Data
@JsonTypeName(value = "patient_activity")
@JsonTypeInfo(include = JsonTypeInfo.As.WRAPPER_OBJECT, use = JsonTypeInfo.Id.NAME)
public class SavePatientActivityRequest {

  @JsonProperty("selected_actions")
  private List<String> selectedActions;

  @JsonProperty("notes")
  private String notes;

  @JsonProperty("in_person")
  private boolean inPerson;

  @JsonProperty("action_minutes")
  private int minutes;

  @JsonProperty("patient_id")
  private Long patientId;

  @JsonProperty("clinic_id")
  private Long clinicId;

  @JsonProperty("entered_by_id")
  private Long enteredById;

  @JsonProperty("pro_review_id")
  private String proReviewId;
}
