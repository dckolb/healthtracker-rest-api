package com.navigatingcancer.healthtracker.api.data.model.survey;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SurveyItemPayload {

	String id;
	Map<String, Object> payload;
	boolean declineACall = false;
	String declineACallComment = null;

	public boolean hasNullOrBlankId() {
		return this.getId() == null || this.getId().isBlank();
	}
	
}
