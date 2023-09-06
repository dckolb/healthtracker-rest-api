package com.navigatingcancer.healthtracker.api.data.client;

import com.navigatingcancer.feign.utils.FeignUtils;
import com.navigatingcancer.healthtracker.api.data.model.surveyConfig.SigningRequest;
import feign.Headers;
import feign.Param;
import feign.RequestLine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DocusignServiceClient {


    private final String BASE_URL;

    @Autowired
    public DocusignServiceClient(@Value("${feign.docusign.url}") String url){
        BASE_URL = url;
    }

    public SigningRequest createSigningRequest(SigningRequest signingRequest){
        return FeignUtils.feign().target(DocusignServiceClient.FeignClient.class, BASE_URL)
                .createSigningRequest(signingRequest);
    }

    public void resendRequest(String id){
        FeignUtils.feign().target(DocusignServiceClient.FeignClient.class, BASE_URL)
                .resendSigningRequest(id);
    }

    public SigningRequest getSigningRequest(@Param("id") String id) {
        return FeignUtils.feign().target(DocusignServiceClient.FeignClient.class, BASE_URL)
                .getSigningRequest(id);
    }

    // TODO: replace with queue?
    public static interface FeignClient{

        @RequestLine("POST /signing_requests")
        @Headers("Content-Type: application/json")
        SigningRequest createSigningRequest(SigningRequest signingRequest);

        @RequestLine("GET /signing_requests/{id}")
        @Headers("Content-Type: application/json")
        SigningRequest getSigningRequest(@Param("id") String id);

        @RequestLine("PUT /signing_requests/{id}/resend")
        @Headers("Content-Type: application/json")
        void resendSigningRequest(@Param("id") String id);

    }

}
