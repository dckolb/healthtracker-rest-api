package com.navigatingcancer.healthtracker.api.data.model.surveyConfig;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ProgramConfig {
    public static final String CLINIC_COLLECT = "CX";
    public static final String PATIENT_COLLECT = "PX";

    private String id;
    private String type;
    private ProgramConsent consent;
    private List<SurveyDef> surveys;

    @Data
    public static class ProgramConsent {
        private String type;
        private List<String> roles;
        private Map<String,ConsentContent> consentContent;
    }

    @Data
    public static class SurveyDef {
        private String type;
        private Map<String,String> surveyIds;
    }

    @Data
    public static class ConsentContent {
        private String templateId;
        private String emailSubject;
        private String emailBlurb;
    }
}
