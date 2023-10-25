package com.navigatingcancer.healthtracker.api.data.service;

import com.navigatingcancer.healthtracker.api.data.model.CheckInType;
import com.navigatingcancer.healthtracker.api.data.model.Enrollment;
import com.navigatingcancer.healthtracker.api.data.model.surveyConfig.ClinicConfig;
import com.navigatingcancer.healthtracker.api.data.model.surveyConfig.ProgramConfig;
import java.util.List;
import java.util.Optional;

public interface SurveyConfigService {
  ProgramConfig getProgramConfig(String id);

  List<ProgramConfig> getProgramConfigs(ClinicConfig clinicConfig);

  ClinicConfig getClinicConfig(Long id);

  String getSurveyIdForProgram(
      CheckInType checkInType, Enrollment enrollment, boolean emptyPendingAndMissedOral);

  boolean isFeatureEnabled(Long clinicId, String feature);

  Optional<String> getSurveyIdForCheckInType(Enrollment enrollment, CheckInType checkInType);
}
