package com.navigatingcancer.healthtracker.api.data.model.surveyConfig;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SurveyConsent {

    private ConsentType type;
    private Map<String,String> templateIds = new HashMap<>();
    private List<String> roles;
}
