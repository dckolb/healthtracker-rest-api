package com.navigatingcancer.healthtracker.api.events;

import lombok.Data;

@Data
public class EnrollmentStopped extends EnrollmentEvent {
    String reason;
    String note;
}
