package com.navigatingcancer.healthtracker.api.data.service;

import com.navigatingcancer.healthtracker.api.data.model.Enrollment;
import com.navigatingcancer.healthtracker.api.data.model.surveyConfig.ProgramConfig;
import com.navigatingcancer.healthtracker.api.data.model.surveyConfig.SurveyConsent;

public interface ConsentService {
    String getStatus(String id);
    String processSurveyConsentForEnrollment(ProgramConfig.ProgramConsent consent, Enrollment enrollment);
    void resendRequest(String consentId);
}
