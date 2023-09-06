package com.navigatingcancer.healthtracker.api.data.model.surveyConfig;

import lombok.Data;

import java.util.Map;

@Data
public class ClinicConfig {
    private String id;
    private String type;
    private Long clinicId;
    private Map<String,String> programs;
    private Map<String,Boolean> features;

    public Boolean isFeatureEnabled(String key) {
        if (this.getFeatures() == null) return false;
        return this.getFeatures().getOrDefault(key, false);
    }
}
