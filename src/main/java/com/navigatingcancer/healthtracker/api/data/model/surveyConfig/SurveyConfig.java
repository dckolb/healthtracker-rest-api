package com.navigatingcancer.healthtracker.api.data.model.surveyConfig;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SurveyConfig {

    private String id;
    private String type;
    private SurveyConsent consent;
}

