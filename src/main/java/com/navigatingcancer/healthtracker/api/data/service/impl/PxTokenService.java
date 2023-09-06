package com.navigatingcancer.healthtracker.api.data.service.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.navigatingcancer.feign.utils.FeignUtils;
import com.navigatingcancer.healthtracker.api.data.model.Enrollment;
import feign.Headers;
import feign.Param;
import feign.RequestLine;
import java.util.List;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PxTokenService {

  @Value("${patient.experience.domain}")
  String pxUrl;

  @Value("${patient.experience.key}")
  String pxAccessKey;

  @Value("${patient.experience.secret}")
  String pxAccessSecret;

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class ContentTicket {
    @JsonProperty("patient_id")
    Long patientId;

    @JsonProperty("clinic_id")
    Long clinicId;

    @JsonProperty("location_id")
    Long locationId;

    @JsonProperty("content_url")
    String contentUrl = "";

    @JsonProperty("survey_id")
    String surveyId = "";
  }

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class UrlToken {
    @JsonProperty("urlToken")
    String urlToken;
  }

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class TokenRequest {
    @JsonProperty("content_ticket")
    ContentTicket contentTicket;
  }

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class TokenResponse {
    @JsonProperty("urlTokens")
    List<UrlToken> urlTokens;
  }

  static interface FeignClient {

    @RequestLine("POST /api/health_trackers/create")
    @Headers({
      "Content-Type: application/json",
      "X-Access-Key: {accessKey}",
      "X-Access-Secret: {accessSecret}"
    })
    TokenResponse getToken(
        TokenRequest request,
        @Param("accessKey") String accessKey,
        @Param("accessSecret") String accessSecret);
  }

  public String getUrl(Enrollment enrollment) {
    FeignClient feign = FeignUtils.feign().target(FeignClient.class, pxUrl);
    ContentTicket contentTicket = new ContentTicket();
    contentTicket.setClinicId(enrollment.getClinicId());
    contentTicket.setPatientId(enrollment.getPatientId());
    contentTicket.setLocationId(enrollment.getLocationId());
    contentTicket.setSurveyId(enrollment.getSurveyId());
    TokenRequest tokenRequest = new TokenRequest();
    tokenRequest.setContentTicket(contentTicket);
    TokenResponse response = feign.getToken(tokenRequest, pxAccessKey, pxAccessSecret);
    return String.join("/", pxUrl, response.urlTokens.get(0).urlToken);
  }
}
