package com.navigatingcancer.healthtracker.api.events;

import java.util.List;

import lombok.Data;

@Data
public class EnrollmentUpdated extends EnrollmentEvent {
    List<String> diffs;    
}
