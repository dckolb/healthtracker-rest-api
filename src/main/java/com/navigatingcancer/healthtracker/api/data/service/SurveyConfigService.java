package com.navigatingcancer.healthtracker.api.data.service;

import java.util.Collection;
import java.util.List;

import com.navigatingcancer.healthtracker.api.data.model.surveyConfig.ClinicConfig;
import com.navigatingcancer.healthtracker.api.data.model.surveyConfig.ProgramConfig;
import com.navigatingcancer.healthtracker.api.data.model.surveyConfig.SurveyConfig;

public interface SurveyConfigService {
    @Deprecated
    SurveyConfig getSurveyConfig(String id);
    ProgramConfig getProgramConfig(String id);
    ClinicConfig getClinicConfig(Long id);
    List<ProgramConfig> getProgramConfigs(Collection<String> Ids);
    String getProgramId(List<ProgramConfig> configs, String type);
}
