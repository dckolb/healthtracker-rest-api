package com.navigatingcancer.healthtracker.api.events;

import com.navigatingcancer.healthtracker.api.data.model.CallAttempt;

import lombok.Data;

@Data
public class CallAttemptEvent extends CheckinEvent {
    CallAttempt callAttemptData;
}
