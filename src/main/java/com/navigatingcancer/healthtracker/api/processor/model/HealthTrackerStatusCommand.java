package com.navigatingcancer.healthtracker.api.processor.model;

import com.navigatingcancer.healthtracker.api.data.model.survey.SurveyPayload;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class HealthTrackerStatusCommand {
  private String enrollmentId;
  private SurveyPayload surveyPayload;
  // Missed or completed CheckIns that triggered this status event
  private List<String> checkInIds;
}
