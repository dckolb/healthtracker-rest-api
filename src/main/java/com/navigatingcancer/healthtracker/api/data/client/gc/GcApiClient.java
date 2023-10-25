package com.navigatingcancer.healthtracker.api.data.client.gc;

import com.navigatingcancer.feign.utils.FeignUtils;
import com.navigatingcancer.healthtracker.api.data.model.ProReviewActivity;
import com.navigatingcancer.healthtracker.api.data.model.survey.SurveySubmissionRequestForGC;
import feign.*;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class GcApiClient {
  @Headers({"Accept: application/json"})
  public interface GcApi {
    @RequestLine("POST /patient_mgmt/api/health_tracker/patient_activities")
    @Headers({
      "Content-Type: application/json",
      "NC-CLINIC-ID: {clinicId}",
      "NC-CLINICIAN-ID: {enteredById}"
    })
    PatientActivityResponse savePatientActivity(
        SavePatientActivityRequest request,
        @Param("clinicId") Long clinicId,
        @Param("enteredById") Long enteredById);

    @RequestLine("POST /patient_mgmt/api/health_tracker/survey_submissions")
    @Headers({
      "Content-Type: application/json",
    })
    void submitSurvey(SurveySubmissionRequestForGC request);
  }

  private final String baseUrl;
  private final String accessKey;
  private final String accessSecret;

  @Autowired
  public GcApiClient(
      @Value("${feign.gcApi.url}") String baseUrl,
      @Value("${feign.gcApi.accessKey}") String accessKey,
      @Value("${feign.gcApi.accessSecret}") String accessSecret) {
    this.baseUrl = baseUrl;
    this.accessKey = accessKey;
    this.accessSecret = accessSecret;
  }

  public PatientActivityResponse savePatientActivity(ProReviewActivity activity) {
    var req = new SavePatientActivityRequest();
    req.setPatientId(activity.getPatientId());
    req.setClinicId(activity.getClinicId());
    req.setProReviewId(req.getProReviewId());
    req.setNotes(activity.getNotes());
    req.setSelectedActions(activity.getSelectedActions());
    req.setMinutes(activity.getMinutes());
    req.setInPerson(activity.isInPerson());
    req.setEnteredById(activity.getEnteredById());
    return getApi(baseUrl).savePatientActivity(req, req.getClinicId(), req.getEnteredById());
  }

  public void submitSurvey(Map<String, Object> surveyJsPayload, String surveyId, Long patientId) {
    var request = new SurveySubmissionRequestForGC(surveyJsPayload, surveyId, patientId);
    getApi(baseUrl).submitSurvey(request);
  }

  private GcApi getApi(String url) {
    return FeignUtils.feign()
        .requestInterceptor(
            req -> req.header("x-access-key", accessKey).header("x-access-secret", accessSecret))
        .logger(
            new Logger() {
              @Override
              protected void log(String configKey, String format, Object... args) {
                log.info(String.format(methodTag(configKey) + format, args));
              }
            })
        .logLevel(log.isDebugEnabled() ? Logger.Level.FULL : Logger.Level.BASIC)
        .target(GcApi.class, url);
  }
}
