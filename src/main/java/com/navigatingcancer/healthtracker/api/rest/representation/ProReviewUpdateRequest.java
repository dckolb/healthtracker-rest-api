package com.navigatingcancer.healthtracker.api.rest.representation;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.navigatingcancer.healthtracker.api.data.model.HealthTrackerStatusCategory;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProReviewUpdateRequest {
  @Data
  public static class Activity {
    @JsonProperty("selectedActions")
    private List<String> selectedActions;

    @JsonProperty("notes")
    private String notes;

    @JsonProperty("inPerson")
    private boolean inPerson;

    @JsonProperty("minutes")
    private int minutes;
  }

  @JsonProperty("enrollmentId")
  private String enrollmentId;

  @JsonProperty("checkInIds")
  private List<String> checkInIds;

  @JsonProperty("noteContent")
  private String noteContent;

  @JsonProperty("category")
  private HealthTrackerStatusCategory category;

  @JsonProperty("sendToEhr")
  private boolean sendToEhr;

  @JsonProperty("activity")
  private Activity activity;

  /* To be removed in next major version */
  @Deprecated(forRemoval = true)
  @JsonSetter("patientActivityId")
  void setPatientActivityId(Long patientActivityId) {
    // noop
  }
}
