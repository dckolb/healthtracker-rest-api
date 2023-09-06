package com.navigatingcancer.healthtracker.api.data.service;

import com.navigatingcancer.healthtracker.api.rest.representation.PatientRequest;

public interface PatientRequestService {

    void requestCall(PatientRequest callRequest);
    void requestRefill(PatientRequest refillRequest);
}
