package com.navigatingcancer.healthtracker.api.data.client;

import com.navigatingcancer.feign.utils.FeignUtils;
import com.navigatingcancer.healthtracker.api.data.model.patientInfo.ClinicInfo;
import com.navigatingcancer.healthtracker.api.data.model.patientInfo.PatientInfo;
import com.navigatingcancer.security.domain.Auth0Config;
import com.navigatingcancer.security.utils.SecurityUtils;
import feign.Param;
import feign.RequestLine;
import feign.RequestTemplate;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;

@Component
@Import(Auth0Config.class)
public class PatientInfoServiceClient {

  // Default is here to support tests until this is refactored to match other service clients
  @Value("${feign.pt_info_svc.url:http://pt-info-svc/patientinfo}")
  private String apiUrl;

  @Autowired private Auth0Config auth0Config;

  public static interface FeignClient {

    @RequestLine("GET /patients?clinicId={clinicId}&patientId={patienId}")
    List<PatientInfo> getPatients(
        @Param("clinicId") Long clinicId, @Param("patienId") Long patienId);

    @RequestLine("GET /clinics?clinicId={clinicId}&locationId={locationId}")
    List<ClinicInfo> getClinics(
        @Param("clinicId") Long clinicId, @Param("locationId") Long locationId);
  }

  public FeignClient getApi() {
    return getApi(apiUrl);
  }

  public FeignClient getApi(String url) {
    if (url == null || url.isEmpty()) {
      throw new IllegalArgumentException("url must not be null or empty");
    }

    return FeignUtils.feign().requestInterceptor(i -> setToken(i)).target(FeignClient.class, url);
  }

  private void setToken(RequestTemplate i) {
    try {
      i.header(
          "Authorization",
          "Bearer "
              + SecurityUtils.getServiceToServiceAuth(
                  auth0Config.getDomain(),
                  auth0Config.getClientId(),
                  auth0Config.getClientSecret(),
                  auth0Config.getAudience()));
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
