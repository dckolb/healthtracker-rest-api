package com.navigatingcancer.healthtracker.api.rest.representation;

import com.navigatingcancer.healthtracker.api.data.model.ContactPreferences;
import com.navigatingcancer.healthtracker.api.data.model.ReasonForCheckInCreation;
import com.navigatingcancer.healthtracker.api.data.validator.ValidSurveyId;
import java.util.Map;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OnDemandCheckInRequest {
  @NotBlank private String enrollmentId;
  @NotNull private ReasonForCheckInCreation reason;
  @ValidSurveyId private String surveyId;
  @NotNull private Map<String, Object> surveyParameters;
  boolean sendNotification = false;
  @Valid ContactPreferences oneTimeContact;
}
