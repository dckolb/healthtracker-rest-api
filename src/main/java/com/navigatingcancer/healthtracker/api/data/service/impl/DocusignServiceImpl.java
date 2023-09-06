package com.navigatingcancer.healthtracker.api.data.service.impl;


import com.navigatingcancer.healthtracker.api.data.client.DocusignServiceClient;
import com.navigatingcancer.healthtracker.api.data.model.surveyConfig.SigningRequest;
import com.navigatingcancer.healthtracker.api.data.service.DocusignService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DocusignServiceImpl implements DocusignService  {

    private final DocusignServiceClient client;

    @Autowired
    public DocusignServiceImpl(DocusignServiceClient client) {
        this.client = client;
    }

    @Override
    public String sendSigningRequest(SigningRequest signingRequest) {

        SigningRequest signingRequestResponse =
                this.client.createSigningRequest(signingRequest);
        if (signingRequestResponse == null) {
            return null;
        }
        return signingRequestResponse.getId();
    }

    @Override
    public void resendSigningRequest(String id) {
        this.client.resendRequest(id);
    }

    @Override
    public String getSigningRequestStatus(String id) {
        SigningRequest signingRequest =
                this.client.getSigningRequest(id);

        return signingRequest.getStatus();
    }


}
