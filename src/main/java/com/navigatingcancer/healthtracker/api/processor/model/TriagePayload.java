package com.navigatingcancer.healthtracker.api.processor.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.navigatingcancer.healthtracker.api.data.model.Adherence;
import com.navigatingcancer.healthtracker.api.data.model.Enrollment;
import com.navigatingcancer.healthtracker.api.data.model.HealthTrackerStatus;
import com.navigatingcancer.healthtracker.api.data.model.HealthTrackerStatusCategory;
import com.navigatingcancer.healthtracker.api.data.model.survey.SurveyItemPayload;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.LocalDate;
import java.time.ZoneId;
import com.google.common.base.Strings;

@Data
@Slf4j
public class TriagePayload {
	public final static String ADHERENCE_REASON_TYPE = "adherence";
	public final static String SYMPTOM_REASON_TYPE = "symptom";

	@Data
	public static class Reason {

		@JsonProperty("reason_type")
		String reasonType;

		@JsonProperty("description")
		String description;

		@JsonProperty("symptom_type")
		String symptomType;

		@JsonProperty("other_symptom")
		String otherSymptom;

		@Setter(AccessLevel.NONE)
		@JsonProperty("severity")
		String severity;

		@JsonProperty("medication_was_taken")
		Boolean medicationTaken;

		@JsonProperty("details")
		List<String> details;

		public void setSeverity(String severity) {
			if ("very severe".equalsIgnoreCase(severity)) {
				this.severity = "very-severe";
			} else if (!"none".equalsIgnoreCase(severity)) {
				this.severity = severity;
			}
		}

	}

	@JsonProperty("patient_id")
	Long patientId;

	@JsonProperty("clinic_id")
	Long clinicId;

	@JsonProperty("location_id")
	Long locationId;

	@JsonProperty("provider_id")
	Long providerId;

	@JsonProperty("alert_level")
	String alertLevel = "immediate";

	@JsonProperty("reasons")
	List<Reason> reasons = new ArrayList<>();

	@JsonProperty("call_back_declined")
	Boolean callBackDeclined;

	@JsonProperty("details")
	String details;

	/**
	 * @param status     HealthTrackerStatus object
	 * @param enrollment Enrollment
	 * @return TriagePayload if a triage ticket should be created.
	 */
	public static TriagePayload createTriageIfNeeded(HealthTrackerStatus status, Enrollment enrollment) {
		TriagePayload triagePayload = null;
		if (status.getCategory() == HealthTrackerStatusCategory.TRIAGE) {

			triagePayload = new TriagePayload();
			triagePayload.setPatientId(enrollment.getPatientId());
			triagePayload.setClinicId(status.getClinicId());
			triagePayload.setLocationId(enrollment.getLocationId());
			triagePayload.setProviderId(enrollment.getProviderId());

			List<SymptomDetails> symptomDetails = status.getSymptomDetails();

			AdherenceParser adherenceParser = new AdherenceParser();
			String medicationName = enrollment.getMedication();

			List<Adherence> adherenceDetails = adherenceParser
					.parse(status.getSurveyPayloadOrals(), medicationName);

			for (Adherence adherence : adherenceDetails) {
				TriagePayload.Reason reason = buildAdherenceReason(adherence, enrollment);
				triagePayload.getReasons().add(reason);
			}

			for (SymptomDetails symptom : symptomDetails) {
				TriagePayload.Reason reason = buildSymptomReason(symptom);
				triagePayload.getReasons().add(reason);
			}

			List<SurveyItemPayload> payloads =  new ArrayList<>();
			if(status.getSurveyPayloadOrals() != null) {
				payloads.addAll(status.getSurveyPayloadOrals());
			}
			if(status.getSurveyPayloadSymptoms() != null){
				payloads.addAll(status.getSurveyPayloadSymptoms());
			}
			DeclineACallDetails declineACallDetails = DeclineACallDetails.fromPayloads(payloads);

			triagePayload.setCallBackDeclined(declineACallDetails.isCallBackDeclined());

			String comments = declineACallDetails.getComments();

			if(comments != null ) {
				triagePayload.setDetails(String.format("\"%s\"", comments));
			}
		}

		return triagePayload;
	}

	private static TriagePayload.Reason buildAdherenceReason(Adherence adherence, Enrollment enrollment) {
		TriagePayload.Reason reason = new TriagePayload.Reason();

		List<String> details = new ArrayList<>();

		String adherenceReason = adherence.getReason();

		if (adherenceReason != null && !adherenceReason.isBlank()) {
			details.add(adherenceReason);
		}

		String reasonDetails = adherence.getReasonDetails();

		if (reasonDetails != null && !reasonDetails.isBlank()) {
			details.add(reasonDetails);
		}

		reason.setReasonType(ADHERENCE_REASON_TYPE);
		reason.setDetails(details);
		reason.setDescription(adherenceDescription(adherence, enrollment));
		reason.setMedicationTaken(getMedicationTakenOrStarted(adherence));

		return reason;
	}

	private static Boolean getMedicationTakenOrStarted(Adherence adherence) {
		boolean medicationTaken = adherence.getStatus().equals(AdherenceParser.MEDICATION_TAKEN_YES);
		if (!medicationTaken) medicationTaken = adherence.getStatus().equals(AdherenceParser.MEDICATION_STARTED_YES);
		return medicationTaken;
	}

	private static String adherenceDescription(Adherence adherence, Enrollment enrollment) {
		boolean medicationTaken = adherence.getStatus().equals(AdherenceParser.MEDICATION_TAKEN_YES);
		boolean medicationNotTaken = adherence.getStatus().equals(AdherenceParser.MEDICATION_TAKEN_NO);
		boolean medicationStarted = adherence.getStatus().equals(AdherenceParser.MEDICATION_STARTED_YES);
		boolean medicationNotStarted = adherence.getStatus().equals(AdherenceParser.MEDICATION_STARTED_NO);
		String medication = adherence.getMedication();

		if (medicationStarted)
			return String.format("%s started", medication);

		String scheduleDate = adherence.getScheduleDate();
		String description = "";

		if (medicationTaken)
			description = String.format("Yes, I took my %s", medication);
		if (medicationNotStarted)
			description = String.format("%s not started", medication);
		if (medicationNotTaken)
			description = String.format("No, I didn't take my %s", medication);

		if (description.length() > 0) {
			// initialize parsedScheduleDate to now b/c checkins completed on-time don't
			// have a scheduleDate generated in the survey payload
			ZoneId enrollmentTZ = ZoneId.of(enrollment.getReminderTimeZone());
			LocalDate parsedScheduleDate = LocalDate.now(enrollmentTZ);

			if (!Strings.isNullOrEmpty(scheduleDate)) {
				parsedScheduleDate = LocalDate.parse(scheduleDate);
			}
			String formattedScheduleDate = DateTimeFormatter
				.ofLocalizedDate(FormatStyle.LONG)
				.format(parsedScheduleDate);

			description = String.format("%s on %s", description, formattedScheduleDate);
		}

		return description;
	}

	private static TriagePayload.Reason buildSymptomReason(SymptomDetails symptom) {
		TriagePayload.Reason reason = new TriagePayload.Reason();
		String symptomType = symptom.getSymptomType();

		List<String> details = new ArrayList<>();
		reason.setReasonType(SYMPTOM_REASON_TYPE);
		reason.setSymptomType(StringUtils.lowerCase(symptomType));

		reason.setSeverity(StringUtils.lowerCase(symptom.getSeverity()));

		reason.setDescription(symptom.getTitle());

		String comment = symptom.getComment();

		if (!StringUtils.isBlank(comment)) {
			details.add(String.format("\"%s\"", comment));
			if (!StringUtils.isBlank(symptomType) && symptomType.equalsIgnoreCase("OTHER")) {
				reason.setOtherSymptom(comment);
			}
		}

		details.addAll(symptom.getDetailList());
		reason.setDetails(details);

		return reason;
	}

}
