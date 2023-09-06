package com.navigatingcancer.healthtracker.api.rest.representation;

import com.navigatingcancer.healthtracker.api.data.model.CheckInData;
import com.navigatingcancer.healthtracker.api.data.model.Enrollment;
import com.navigatingcancer.healthtracker.api.data.model.HealthTrackerStatus;
import lombok.AllArgsConstructor;

import lombok.Getter;
import lombok.NoArgsConstructor;


@NoArgsConstructor
@AllArgsConstructor
public class HealthTrackerStatusResponse {
    @Getter
    private HealthTrackerStatus healthTrackerStatus;
    @Getter
    private Enrollment enrollment;
    @Getter
    private CheckInData checkInData;
}
