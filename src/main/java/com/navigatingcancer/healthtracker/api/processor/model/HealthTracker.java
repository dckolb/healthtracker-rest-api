package com.navigatingcancer.healthtracker.api.processor.model;

import com.navigatingcancer.healthtracker.api.data.model.CheckIn;
import com.navigatingcancer.healthtracker.api.data.model.Enrollment;
import com.navigatingcancer.healthtracker.api.data.model.HealthTrackerStatus;
import com.navigatingcancer.healthtracker.api.data.model.HealthTrackerStatusCategory;
import com.navigatingcancer.healthtracker.api.data.model.survey.SurveyItemPayload;
import com.navigatingcancer.healthtracker.api.data.model.survey.SurveyPayload;
import com.navigatingcancer.patientinfo.domain.PatientInfo;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Data
@Slf4j
public class HealthTracker {
	private Enrollment enrollment;
	// sorted by time taken descending
	private List<CheckIn> checkIns;
	private PatientInfo patientInfo;
	private HealthTrackerStatus healthTrackerStatus;
	private CheckInAggregator checkInAggregator;
	private ProFormatManager proFormatManager;
	private SymptomDetailsAggregator symptomDetailsAggregator;
	private SurveyPayload surveyPayload;
	private Boolean followsCtcaeStandard;

	public HealthTracker(Enrollment enrollment, List<CheckIn> checkIns, HealthTrackerStatus healthTrackerStatus,
						 ProFormatManager proFormatManager, SurveyPayload surveyPayload) {


		this.followsCtcaeStandard = proFormatManager.followsCtcaeStandard(enrollment);
		this.enrollment = enrollment;
		this.checkIns = checkIns;
		this.patientInfo = healthTrackerStatus.getPatientInfo();
		this.healthTrackerStatus = healthTrackerStatus;
		this.proFormatManager = proFormatManager;
		this.checkInAggregator = new CheckInAggregator(checkIns, healthTrackerStatus, followsCtcaeStandard);
		this.healthTrackerStatus.clear();

		List<SurveyItemPayload> inputList = new ArrayList<>();
		if(surveyPayload != null) {
			inputList = surveyPayload.getSymptoms();
		}
		this.surveyPayload = surveyPayload;

		this.symptomDetailsAggregator = new SymptomDetailsAggregator(inputList, followsCtcaeStandard);
	}
}
