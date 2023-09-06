package com.navigatingcancer.healthtracker.api.data.service;

import com.navigatingcancer.healthtracker.api.data.model.CheckIn;
import com.navigatingcancer.healthtracker.api.data.model.CheckInData;
import com.navigatingcancer.healthtracker.api.data.model.PracticeCheckIn;
import com.navigatingcancer.healthtracker.api.data.model.survey.SurveyPayload;
import com.navigatingcancer.healthtracker.api.rest.QueryParameters.EnrollmentQuery;

import java.util.List;

public interface CheckInService {

	String MEDICATION_TAKEN_QUESTION_ID = "medicationTaken";
	String MEDICATION_TAKEN_DATE = "medicationTakenDate";
	String MEDICATION_TAKEN_ANSWER_ID = "yes";
	String MEDICATION_NOT_TAKEN_ANSWER_ID = "no";
	String MEDICATION_NOT_TAKEN_REASON = "medicationSkipReason";
	String MEDICATION_STARTED_DATE_QUESTIONS_ID = "medicationStartedDate";
	String MEDICATION_STARTED_QUESTION_ID = "medicationStarted";
	String SYMPTOM_CHECKIN_QUESTION_ID = "haveSymptoms";
	String SYMPTOM_NONE_REPORTED_ANSWER_ID = "no";
	String SYMPTOM_SWELLING_SEVERITY = "swellingSeverity";
	String DECLINE_A_CALL = "declineACall";
	String DECLINE_A_CALL_COMMENT = "declineACallComment";

	String SYMPTOM_FEVER_SEVERITY = "feverSeverity";
	String PAIN_FEVER_SEVERITY = "painSeverity";
	String OTHER_FEVER_SEVERITY = "otherSeverity";

	String VERY_SEVERE = "4";
	String SEVERE = "3";
	String MODERATE = "2";
	String MILD = "1";

	String PAIN_SEVERE = "7";



	void checkIn(SurveyPayload surveyPayload);

	CheckIn checkInBackfill(CheckIn backfillCheckIn);

	List<CheckInData> getCheckInDataByEnrollmentIDs(List<String> enrollmentIds);

	List<CheckInData> getCheckInDataByEnrollmentIDs(List<String> enrollmentIds, boolean includePatientInfo);

	CheckInData getCheckInData(EnrollmentQuery query);

	CheckInData getCheckInData(String enrollmentId);

	PracticeCheckIn getPracticeCheckInData(Long clinicId, Long patientId);
	PracticeCheckIn persistPracticeCheckInData(PracticeCheckIn practiceCheckIn);

}
