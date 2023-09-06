package com.navigatingcancer.healthtracker.api.events;

import java.time.Instant;

import com.navigatingcancer.healthtracker.api.data.model.Enrollment;

import lombok.Data;

@Data
public class EnrollmentEvent {
    Enrollment enrollment;
    Instant date;
    String clinicianId;
    String clinicianName;        
}
