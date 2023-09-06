package com.navigatingcancer.healthtracker.api.data.service;


import com.navigatingcancer.healthtracker.api.data.model.surveyConfig.SigningRequest;

public interface DocusignService {

    String sendSigningRequest(SigningRequest signingRequest);
    void resendSigningRequest(String id);
    String getSigningRequestStatus(String id);
}
