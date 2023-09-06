package com.navigatingcancer.healthtracker.api.events;

import java.time.Instant;
import java.util.List;

import com.navigatingcancer.healthtracker.api.data.model.CheckIn;
import com.navigatingcancer.healthtracker.api.data.model.Enrollment;

import lombok.Data;

@Data
public class CheckinEvent {
    Enrollment enrollment;
    List<CheckIn> checkin;
    Instant date;
    String by;
}
