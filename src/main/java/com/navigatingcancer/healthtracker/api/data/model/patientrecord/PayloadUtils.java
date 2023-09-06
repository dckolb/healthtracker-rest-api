package com.navigatingcancer.healthtracker.api.data.model.patientrecord;

import com.navigatingcancer.healthtracker.api.data.model.HealthTrackerStatusCategory;

import java.util.Optional;

class PayloadUtils {
    static String htCategoryToNiceName(HealthTrackerStatusCategory cat) {
        return HealthTrackerStatusCategory.categoryNiceName(cat).toLowerCase();
    }

    static final String HEALTH_TRACKER_NAME = "Health Tracker";

    static String defaultCreatedBy(String createdBy) {
        return Optional.ofNullable(createdBy).orElse(HEALTH_TRACKER_NAME);
    }
}
