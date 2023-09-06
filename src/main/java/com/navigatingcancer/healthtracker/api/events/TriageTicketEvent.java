package com.navigatingcancer.healthtracker.api.events;

import java.time.Instant;

import com.navigatingcancer.healthtracker.api.data.model.HealthTrackerStatusCategory;

import lombok.Data;

@Data
public class TriageTicketEvent {
    public enum Type {
        CREATED, CLOSED, MARKED_AS_ERROR, REQUEST_CALL, REQUEST_REFILL
    };

    Type type;
    String enrollmentId;
    Long clinicId;
    Long patientId;
    Instant date;
    String clinicianId;
    String clinicianName;
    String note;
    HealthTrackerStatusCategory status;
}
