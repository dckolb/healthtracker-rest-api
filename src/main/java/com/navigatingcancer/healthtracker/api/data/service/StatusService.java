package com.navigatingcancer.healthtracker.api.data.service;

import com.navigatingcancer.healthtracker.api.data.model.HealthTrackerStatus;
import com.navigatingcancer.healthtracker.api.rest.representation.HealthTrackerStatusResponse;

import java.util.List;

public interface StatusService {

    List<HealthTrackerStatus> getByIds(Long clinicId, List<String> ids);
    List<HealthTrackerStatusResponse> getManualCollectDueByIds(Long clinicId, List<String> ids);
}
