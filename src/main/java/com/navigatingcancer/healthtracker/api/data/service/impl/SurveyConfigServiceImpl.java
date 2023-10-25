package com.navigatingcancer.healthtracker.api.data.service.impl;

import com.google.common.base.Preconditions;
import com.navigatingcancer.healthtracker.api.data.client.ConfigServiceClient;
import com.navigatingcancer.healthtracker.api.data.model.CheckInType;
import com.navigatingcancer.healthtracker.api.data.model.Enrollment;
import com.navigatingcancer.healthtracker.api.data.model.ProgramType;
import com.navigatingcancer.healthtracker.api.data.model.TherapyType;
import com.navigatingcancer.healthtracker.api.data.model.survey.SurveyId;
import com.navigatingcancer.healthtracker.api.data.model.surveyConfig.ClinicConfig;
import com.navigatingcancer.healthtracker.api.data.model.surveyConfig.ProgramConfig;
import com.navigatingcancer.healthtracker.api.data.service.SurveyConfigService;
import java.util.*;
import org.codehaus.plexus.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SurveyConfigServiceImpl implements SurveyConfigService {

  private final ConfigServiceClient configServiceClient;

  @Autowired
  public SurveyConfigServiceImpl(ConfigServiceClient configServiceClient) {
    this.configServiceClient = configServiceClient;
  }

  @Override
  public ProgramConfig getProgramConfig(String id) {
    if (StringUtils.isBlank(id)) return null;
    return this.configServiceClient.getProgramConfig(id);
  }

  @Override
  public ClinicConfig getClinicConfig(Long id) {
    return this.configServiceClient.getClinicConfig(id);
  }

  @Override
  public List<ProgramConfig> getProgramConfigs(ClinicConfig clinicConfig) {
    return clinicConfig.getPrograms().values().stream().map(this::getProgramConfig).toList();
  }

  private List<ProgramConfig> getProgramConfigs(Long clinicId) {
    return getProgramConfigs(getClinicConfig(clinicId));
  }

  @Override
  public String getSurveyIdForProgram(
      CheckInType checkInType, Enrollment enrollment, boolean emptyPendingAndMissedOral) {
    if (checkInType != null && StringUtils.isNotBlank(enrollment.getProgramId())) {
      ProgramConfig programConfig = getProgramConfig(enrollment.getProgramId());
      List<ProgramConfig.SurveyDef> surveyDefs = programConfig.getSurveys();
      for (ProgramConfig.SurveyDef surveyDef : surveyDefs) {
        if (checkInType.name().equalsIgnoreCase(surveyDef.getType())) {
          String key =
              enrollment.isManualCollect()
                  ? ProgramConfig.CLINIC_COLLECT
                  : ProgramConfig.PATIENT_COLLECT;

          String surveyId;
          if (checkInType.equals(CheckInType.COMBO) && emptyPendingAndMissedOral) {
            surveyId =
                enrollment.isManualCollect()
                    ? SurveyId.ORAL_ADHERENCE_CX
                    : SurveyId.ORAL_ADHERENCE_PX;
          } else {
            surveyId = surveyDef.getSurveyIds().getOrDefault(key, null);
          }

          return surveyId;
        }
      }
    }
    return null;
  }

  @Override
  public boolean isFeatureEnabled(Long clinicId, String feature) {
    var clinicConfig = getClinicConfig(clinicId);
    return clinicConfig != null && clinicConfig.isFeatureEnabled(feature);
  }

  @Override
  public Optional<String> getSurveyIdForCheckInType(
      Enrollment enrollment, CheckInType checkInType) {
    Preconditions.checkNotNull(checkInType, "checkInType must not be null");

    var programConfigs = getProgramConfigs(getClinicConfig(enrollment.getClinicId()));

    var proCtcaeProgram =
        programConfigs.stream()
            .filter(p -> ProgramType.PRO_CTCAE.getProgramName().equalsIgnoreCase(p.getType()))
            .findFirst();

    final String programId;
    // if found, use PRO-CTCAE program *only* for symptom check-ins and only if IV therapy type
    // selected
    // TODO: make this therapy type restriction a core part of the program config?
    if (proCtcaeProgram.isPresent()
        && checkInType == CheckInType.SYMPTOM
        && (enrollment.getTherapyTypes() != null
            && enrollment.getTherapyTypes().contains(TherapyType.IV))) {
      programId = proCtcaeProgram.get().getId();
    } else {
      programId = ProgramConfig.getProgramId(programConfigs, ProgramType.HEALTH_TRACKER);
    }

    var programConfig = getProgramConfig(programId);
    return programConfig.getSurveyId(checkInType, enrollment.isManualCollect());
  }
}
