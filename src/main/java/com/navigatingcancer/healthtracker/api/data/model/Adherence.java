package com.navigatingcancer.healthtracker.api.data.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class Adherence {
	@JsonProperty("status")
	String status;

	@JsonProperty("medication")
	String medication;

	@JsonProperty("reason")
	String reason = "";

	@JsonProperty("reason_details")
	String reasonDetails = "";

	@JsonProperty("patient_reported_start_date")
	String patientReportedStartDate = "";

	@JsonProperty("schedule_date")
	String scheduleDate = "";
}
