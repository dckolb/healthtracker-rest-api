package com.navigatingcancer.healthtracker.api.data.client;

import com.navigatingcancer.feign.utils.FeignUtils;
import com.navigatingcancer.healthtracker.api.data.model.surveyConfig.ClinicConfig;
import com.navigatingcancer.healthtracker.api.data.model.surveyConfig.ProgramConfig;

import com.navigatingcancer.security.domain.Auth0Config;
import com.navigatingcancer.security.utils.SecurityUtils;
import feign.Param;
import feign.RequestLine;
import feign.RequestTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


@Component
@Slf4j
public class ConfigServiceClient {

    private final String BASE_URL;

    private final Auth0Config auth0Config;

    @Autowired
    public ConfigServiceClient(Auth0Config auth0Config, @Value("${feign.consent.url}") String baseUrl){
        this.BASE_URL = baseUrl;
        this.auth0Config = auth0Config;
    }

    public ClinicConfig getClinicConfig(Long clinicId){
        return this.getApi(BASE_URL).getClinicConfig(clinicId);
    }

    public ProgramConfig getProgramConfig(String programId){
        return this.getApi(BASE_URL).getProgramConfig(programId);
    }

    private static interface FeignClient {

        @RequestLine("GET /clinics/{clinicId}")
        ClinicConfig getClinicConfig(@Param("clinicId") Long clinicId);

        @RequestLine("GET /programs/{programId}")
        ProgramConfig getProgramConfig(@Param("programId") String programId);

    }

    private ConfigServiceClient.FeignClient getApi(String url) {
        return FeignUtils.feign()
                .requestInterceptor(this::setToken)
                .target(ConfigServiceClient.FeignClient.class, url);
    }

    private void setToken(RequestTemplate i) {
        try {
            i.header("Authorization",
                    "Bearer " + SecurityUtils.getServiceToServiceAuth(auth0Config.getDomain(),
                            auth0Config.getClientId(), auth0Config.getClientSecret(), auth0Config.getAudience()));
        }catch (Exception e) {
            log.error("unable to set access token for config service call", e);
        }
    }
}
