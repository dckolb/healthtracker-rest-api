package com.navigatingcancer.healthtracker.api.data.service;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.navigatingcancer.healthtracker.api.TestConfig;
import com.navigatingcancer.healthtracker.api.data.client.PatientInfoServiceClient;
import com.navigatingcancer.healthtracker.api.data.model.Enrollment;
import com.navigatingcancer.healthtracker.api.data.model.LanguageType;
import com.navigatingcancer.healthtracker.api.data.model.patientInfo.PatientInfo;
import com.navigatingcancer.healthtracker.api.data.model.surveyConfig.ConsentType;
import com.navigatingcancer.healthtracker.api.data.model.surveyConfig.ProgramConfig;
import com.navigatingcancer.healthtracker.api.data.model.surveyConfig.SigningRequest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.UUID;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
@Import(TestConfig.class)
public class ConsentServiceTest {

  @MockBean private DocusignService docusignService;

  @MockBean private PatientInfoServiceClient patientInfoClient;

  @Autowired private ConsentService consentService;

  @Test
  public void givenId_shouldCallDocusign() {
    doNothing().when(docusignService).resendSigningRequest(anyString());

    consentService.resendRequest(UUID.randomUUID().toString());

    verify(docusignService, times(1)).resendSigningRequest(anyString());
  }

  @Test
  public void givenId_shouldFindSigningRequest() {

    SigningRequest sr = new SigningRequest();
    sr.setStatus("COMPLETED");
    when(docusignService.getSigningRequestStatus(anyString())).thenReturn(sr.getStatus());

    String status = consentService.getStatus(UUID.randomUUID().toString());

    Assert.assertEquals("status should be COMPLETED", "COMPLETED", status);
  }

  @Test
  public void givenNonGT_shouldNotDoConsent() {
    Enrollment en = new Enrollment();
    ProgramConfig.ProgramConsent surveyConsent = null;

    String id = consentService.processSurveyConsentForEnrollment(surveyConsent, en);

    Assert.assertNull(id);
  }

  @Test
  public void givenGT_shouldReturnID() {
    PatientInfoServiceClient.FeignClient fMock = mock(PatientInfoServiceClient.FeignClient.class);
    Enrollment en = new Enrollment();
    en.setClinicId(1l);
    en.setPatientId(3l);

    ProgramConfig.ProgramConsent programConsent = new ProgramConfig.ProgramConsent();
    programConsent.setType(ConsentType.DOCUSIGN.name());
    programConsent.setRoles(new ArrayList<>());
    programConsent.setConsentContent(new HashMap<String, ProgramConfig.ConsentContent>());
    ProgramConfig.ConsentContent content = new ProgramConfig.ConsentContent();
    content.setEmailBlurb(UUID.randomUUID().toString());
    content.setEmailSubject(UUID.randomUUID().toString());
    content.setTemplateId(UUID.randomUUID().toString());
    programConsent.getConsentContent().put(LanguageType.EN.name(), content);

    PatientInfo pi = new PatientInfo();
    when(patientInfoClient.getApi()).thenReturn(fMock);
    when(fMock.getPatients(anyLong(), anyLong())).thenReturn(Arrays.asList(pi));

    String id = consentService.processSurveyConsentForEnrollment(programConsent, en);

    Assert.assertNull(id);
  }
}
