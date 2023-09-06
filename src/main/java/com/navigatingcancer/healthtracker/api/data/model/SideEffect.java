package com.navigatingcancer.healthtracker.api.data.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class SideEffect {
	@JsonProperty("symptom")
	String symptomType;

	@JsonProperty("frequency")
	String frequency;

	@JsonProperty("severity")
	String severity;

	@JsonProperty("rawSeverity")
	String rawSeverity;

	@JsonProperty("interference")
	String interference;

	@JsonProperty("location")
	String location;

	@JsonProperty("occurrence")
	String occurrence;
}
