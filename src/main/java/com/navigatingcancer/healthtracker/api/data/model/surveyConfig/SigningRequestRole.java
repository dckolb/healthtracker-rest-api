package com.navigatingcancer.healthtracker.api.data.model.surveyConfig;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SigningRequestRole {
    private String name;
    private String email;
    private String role;
}
