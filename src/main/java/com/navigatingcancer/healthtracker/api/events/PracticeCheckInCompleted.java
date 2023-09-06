package com.navigatingcancer.healthtracker.api.events;

import lombok.Data;

import java.time.Instant;

@Data
public class PracticeCheckInCompleted {
    Long clinicId;
    Long patientId;
    Instant date;
    String by;
}
