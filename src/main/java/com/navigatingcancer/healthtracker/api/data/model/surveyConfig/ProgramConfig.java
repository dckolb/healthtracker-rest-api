package com.navigatingcancer.healthtracker.api.data.model.surveyConfig;

import com.navigatingcancer.healthtracker.api.data.model.CheckInType;
import com.navigatingcancer.healthtracker.api.data.model.ProgramType;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.Data;

@Data
public class ProgramConfig {
  public static final String HEALTH_TRACKER_PROGRAM_ID = "5f065dfc7be5761f058b6cc7";
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
    private Map<String, ConsentContent> consentContent;
  }

  @Data
  public static class SurveyDef {
    @Deprecated private String type;
    private Map<String, String> surveyIds;
  }

  @Data
  public static class ConsentContent {
    private String templateId;
    private String emailSubject;
    private String emailBlurb;
  }

  public static String getProgramId(List<ProgramConfig> configs, ProgramType type) {
    for (ProgramConfig p : configs) {
      if (type.getProgramName().equalsIgnoreCase(p.getType())) {
        return p.getId();
      }
    }
    return HEALTH_TRACKER_PROGRAM_ID; // default to healthtracker so a survey gets displayed
  }

  /**
   * Return the appropriate survey id for this program or an empty optional.
   *
   * @param checkInType
   * @param manualCollect
   * @return
   */
  public Optional<String> getSurveyId(CheckInType checkInType, boolean manualCollect) {
    String key = manualCollect ? ProgramConfig.CLINIC_COLLECT : ProgramConfig.PATIENT_COLLECT;

    for (var surveyDef : getSurveys()) {
      if (checkInType.name().equalsIgnoreCase(surveyDef.getType())) {
        var surveyId = surveyDef.getSurveyIds().get(key);

        if (surveyId != null) {
          return Optional.of(surveyId);
        }
      }
    }

    return Optional.empty();
  }
}
