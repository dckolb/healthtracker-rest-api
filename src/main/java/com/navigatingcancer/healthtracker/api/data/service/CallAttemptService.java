package com.navigatingcancer.healthtracker.api.data.service;

import java.util.List;

import com.navigatingcancer.healthtracker.api.data.model.CallAttempt;

public interface CallAttemptService {
    CallAttempt saveCallAttempt(CallAttempt callAttempt);
    List<CallAttempt> getCallAttempts(List<String> checkInIds);
}