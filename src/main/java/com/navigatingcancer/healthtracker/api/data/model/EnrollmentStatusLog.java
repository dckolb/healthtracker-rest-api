package com.navigatingcancer.healthtracker.api.data.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class EnrollmentStatusLog {
	
	EnrollmentStatus status;
	String reason;
	String note;
	Instant date;
	String clinicianId;
	String clinicianName;
	
}
