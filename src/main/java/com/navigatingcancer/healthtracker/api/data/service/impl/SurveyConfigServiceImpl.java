package com.navigatingcancer.healthtracker.api.data.service.impl;

import com.navigatingcancer.healthtracker.api.data.client.ConfigServiceClient;
import com.navigatingcancer.healthtracker.api.data.model.surveyConfig.ClinicConfig;
import com.navigatingcancer.healthtracker.api.data.model.surveyConfig.ProgramConfig;
import com.navigatingcancer.healthtracker.api.data.model.surveyConfig.SurveyConfig;
import com.navigatingcancer.healthtracker.api.data.service.SurveyConfigService;
import org.codehaus.plexus.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Service
public class SurveyConfigServiceImpl implements SurveyConfigService {
    Map<String,SurveyConfig> surveys;

    private final ConfigServiceClient configServiceClient;

    @Autowired
    public SurveyConfigServiceImpl(ConfigServiceClient configServiceClient) {
        this.configServiceClient = configServiceClient;
    }

    @Override
    public SurveyConfig getSurveyConfig(String id) {
        return surveys.getOrDefault(id, null);
    }

    @Override
    public ProgramConfig getProgramConfig(String id) {
        if (StringUtils.isBlank(id))
            return null;
        return this.configServiceClient.getProgramConfig(id);
    }

    @Override
    public ClinicConfig getClinicConfig(Long id) {
        return this.configServiceClient.getClinicConfig(id);
    }

    @Override
    public List<ProgramConfig> getProgramConfigs(Collection<String> Ids) {
        List<ProgramConfig> programs = new ArrayList<>();

        for(String id : Ids) {
            ProgramConfig p = this.getProgramConfig(id);
            programs.add(p);
        }

        return programs;
    }

    @Override
    public String getProgramId(List<ProgramConfig> configs, String type) {
        for (ProgramConfig p : configs) {
            if (p.getType().toLowerCase().equals(type.toLowerCase())) {
                return p.getId();
            }
        }

        return "5f065dfc7be5761f058b6cc7"; // default to healthtracker so a survey gets displayed
    }
}
