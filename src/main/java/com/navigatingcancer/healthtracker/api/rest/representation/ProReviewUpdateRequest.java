package com.navigatingcancer.healthtracker.api.rest.representation;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.navigatingcancer.healthtracker.api.data.model.HealthTrackerStatusCategory;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProReviewUpdateRequest {
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

  @JsonProperty("patientActivityId")
  private Integer patientActivityId;
}
