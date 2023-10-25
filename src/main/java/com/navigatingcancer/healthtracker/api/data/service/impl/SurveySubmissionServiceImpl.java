package com.navigatingcancer.healthtracker.api.data.service.impl;

import com.navigatingcancer.healthtracker.api.data.client.gc.GcApiClient;
import com.navigatingcancer.healthtracker.api.data.model.CheckIn;
import com.navigatingcancer.healthtracker.api.data.model.CheckInStatus;
import com.navigatingcancer.healthtracker.api.data.model.Enrollment;
import com.navigatingcancer.healthtracker.api.data.model.PracticeCheckIn;
import com.navigatingcancer.healthtracker.api.data.model.survey.SurveyId;
import com.navigatingcancer.healthtracker.api.data.model.survey.SurveyInstance;
import com.navigatingcancer.healthtracker.api.data.model.survey.SurveyItemPayload;
import com.navigatingcancer.healthtracker.api.data.model.survey.SurveyPayload;
import com.navigatingcancer.healthtracker.api.data.model.survey.SurveyPayloadContent;
import com.navigatingcancer.healthtracker.api.data.model.survey.SurveySubmission;
import com.navigatingcancer.healthtracker.api.data.repo.CheckInRepository;
import com.navigatingcancer.healthtracker.api.data.repo.EnrollmentRepository;
import com.navigatingcancer.healthtracker.api.data.repo.surveyInstance.SurveyInstanceRepository;
import com.navigatingcancer.healthtracker.api.data.service.CheckInService;
import com.navigatingcancer.healthtracker.api.data.service.SurveySubmissionService;
import com.navigatingcancer.healthtracker.api.data.util.MongoUtils;
import com.navigatingcancer.healthtracker.api.rest.exception.BadDataException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SurveySubmissionServiceImpl implements SurveySubmissionService {
  @Autowired private CheckInService checkInService;
  @Autowired private CheckInRepository checkInRepository;
  @Autowired private EnrollmentRepository enrollmentRepository;
  @Autowired private SurveyInstanceRepository surveyInstanceRepository;
  @Autowired private GcApiClient gcApiClient;

  private static final String PRACTICE_SUCCESS = "Practice check recorded";
  private static final String SUBMISSION_SUCCESS = "Survey submission success";

  @Override
  public String process(SurveySubmission submission) throws BadDataException {
    if (submission.isPracticeCheckIn()) {
      PracticeCheckIn pc = new PracticeCheckIn();
      pc.setStatus(PracticeCheckIn.Status.COMPLETED);
      checkInService.persistPracticeCheckInData(pc);
      return PRACTICE_SUCCESS;
    }

    CheckIn checkIn =
        MongoUtils.getOrThrowBadData(
            checkInRepository.findById(submission.getCheckInId()),
            String.format("Unable to find checkIn with id %s", submission.getCheckInId()));

    Enrollment enrollment =
        MongoUtils.getOrThrowBadData(
            enrollmentRepository.findById(checkIn.getEnrollmentId()),
            String.format("Unable to find enrollment with id %s", checkIn.getEnrollmentId()));

    SurveyInstance surveyInstance =
        MongoUtils.getOrThrowBadData(
            surveyInstanceRepository.findById(checkIn.getSurveyInstanceId()),
            String.format(
                "Unable to find surveyInstance with id %s", checkIn.getSurveyInstanceId()));

    switch (surveyInstance.getSurveyId()) {
      case SurveyId.HEALTH_TRACKER_CX:
      case SurveyId.HEALTH_TRACKER_PX:
      case SurveyId.PROCTCAE_CX:
      case SurveyId.PROCTCAE_PX:
        buildAndSubmitLegacySymptomSurvey(enrollment, submission);
        break;
      case SurveyId.ORAL_ADHERENCE_CX:
      case SurveyId.ORAL_ADHERENCE_PX:
        buildAndSubmitLegacyOralSurvey(enrollment, submission);
        break;
      case SurveyId.NCCN_2022_DISTRESS:
        forwardSurveyToGC(enrollment, submission, surveyInstance.getSurveyId());
        break;
      default:
        throw new BadDataException(
            String.format("Unknown surveyId: %s", surveyInstance.getSurveyId()));
    }

    return SUBMISSION_SUCCESS;
  }

  private void buildAndSubmitLegacyOralSurvey(
      Enrollment enrollment, SurveySubmission surveySubmission) {

    SurveyItemPayload oralPayload = new SurveyItemPayload();
    oralPayload.setId(surveySubmission.getCheckInId());
    oralPayload.setPayload(surveySubmission.getSurveyPayload());

    var oralSurveyItems = new ArrayList<SurveyItemPayload>();
    oralSurveyItems.add(oralPayload);
    oralSurveyItems.addAll(surveySubmission.getMissedCheckIns());

    SurveyPayload surveyPayload = buildBaseLegacySurveyPayload(enrollment);
    surveyPayload.setOral(oralSurveyItems);

    checkInService.checkIn(surveyPayload);
  }

  private void buildAndSubmitLegacySymptomSurvey(
      Enrollment enrollment, SurveySubmission surveySubmission) {

    SurveyItemPayload symptomPayload = new SurveyItemPayload();
    symptomPayload.setId(surveySubmission.getCheckInId());
    symptomPayload.setDeclineACall(surveySubmission.isDeclineACall());
    symptomPayload.setDeclineACallComment(surveySubmission.getDeclineACallComment());
    symptomPayload.setPayload(surveySubmission.getSurveyPayload());

    SurveyPayload surveyPayload = buildBaseLegacySurveyPayload(enrollment);
    surveyPayload.setSymptoms(new ArrayList<>(List.of(symptomPayload)));

    checkInService.checkIn(surveyPayload);
  }

  private SurveyPayload buildBaseLegacySurveyPayload(Enrollment enrollment) {
    SurveyPayload payload = new SurveyPayload();
    SurveyPayloadContent content = new SurveyPayloadContent();
    content.setEnrollmentId(enrollment.getId());
    payload.setContent(content);
    return payload;
  }

  private void forwardSurveyToGC(
      Enrollment enrollment, SurveySubmission surveySubmission, String surveyId) {
    Map<String, Object> checkInUpdate = new HashMap<String, Object>();
    checkInUpdate.put("payload", surveySubmission.getSurveyPayload());
    checkInUpdate.put("status", CheckInStatus.COMPLETED);
    checkInRepository.updateFieldsById(surveySubmission.getCheckInId(), checkInUpdate);

    gcApiClient.submitSurvey(
        surveySubmission.getSurveyPayload(), surveyId, enrollment.getPatientId());
  }
}
