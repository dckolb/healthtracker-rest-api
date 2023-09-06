package com.navigatingcancer.healthtracker.api.data.model.schedule;

import java.time.LocalTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.navigatingcancer.healthtracker.api.data.model.CheckInType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TriggerPayload {
	
	public static enum TriggerType {
		SYSTEM, REMINDER, STATUS,
		ENROLLMENT_START, ENROLLMENT_END,
		CYCLE_START, CYCLE_END
	}

	String enrollmentId;
	CheckInType checkInType;
	LocalTime checkInTime;
	TriggerType type;

}
