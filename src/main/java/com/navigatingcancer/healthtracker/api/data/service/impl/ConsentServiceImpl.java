package com.navigatingcancer.healthtracker.api.data.service.impl;

import com.navigatingcancer.healthtracker.api.data.client.PatientInfoServiceClient;
import com.navigatingcancer.healthtracker.api.data.model.Enrollment;
import com.navigatingcancer.healthtracker.api.data.model.patientInfo.PatientInfo;
import com.navigatingcancer.healthtracker.api.data.model.surveyConfig.*;
import com.navigatingcancer.healthtracker.api.data.repo.EnrollmentRepository;
import com.navigatingcancer.healthtracker.api.data.service.ConsentService;
import com.navigatingcancer.healthtracker.api.data.service.DocusignService;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ConsentServiceImpl implements ConsentService {

  private DocusignService docusignService;

  private EnrollmentRepository enrollmentRepository;

  private PatientInfoServiceClient patientInfoClient;

  private String defaultLanguage;

  @Autowired
  public ConsentServiceImpl(
      DocusignService docusignService,
      EnrollmentRepository enrollmentRepository,
      PatientInfoServiceClient patientInfoClient) {
    this.docusignService = docusignService;
    this.enrollmentRepository = enrollmentRepository;
    this.patientInfoClient = patientInfoClient;
  }

  @Override
  public String getStatus(String id) {
    return docusignService.getSigningRequestStatus(id);
  }

  @Override
  public String processSurveyConsentForEnrollment(
      ProgramConfig.ProgramConsent consent, Enrollment enrollment) {
    if (consent == null) return null;

    if (ConsentType.DOCUSIGN.name().equalsIgnoreCase(consent.getType())) {
      List<PatientInfo> patients =
          this.patientInfoClient
              .getApi()
              .getPatients(enrollment.getClinicId(), enrollment.getPatientId());
      // should only match one
      if (patients.size() == 0) throw new RuntimeException("no patient found");

      PatientInfo patientInfo = patients.get(0);
      // assume patient only for now
      SigningRequest signingRequest = new SigningRequest();
      signingRequest.setPatientId(enrollment.getPatientId());
      signingRequest.setClinicId(enrollment.getClinicId());
      signingRequest.setLocationId(enrollment.getLocationId());
      ProgramConfig.ConsentContent consentContent =
          consent.getConsentContent().get(enrollment.getDefaultLanguage().toString());
      signingRequest.setTemplateId(consentContent.getTemplateId());
      signingRequest.setEmailBlurb(consentContent.getEmailBlurb());
      signingRequest.setEmailSubject(consentContent.getEmailSubject());

      for (String role : consent.getRoles()) {
        SigningRequestRole signingRequestRole = new SigningRequestRole();
        signingRequestRole.setRole(role);
        signingRequestRole.setName(
            String.format("%s %s", patientInfo.getFirstName(), patientInfo.getLastName()));
        signingRequestRole.setEmail(
            enrollment.getEmailAddress() != null
                ? enrollment.getEmailAddress()
                : patientInfo.getEmail());
        signingRequest.getRoles().add(signingRequestRole);
      }

      return this.docusignService.sendSigningRequest(signingRequest);
    }
    return null;
  }

  @Override
  public void resendRequest(String consentId) {
    this.docusignService.resendSigningRequest(consentId);
  }
}
