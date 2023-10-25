package com.navigatingcancer.healthtracker.api.data.repo;

import com.navigatingcancer.healthtracker.api.data.model.HealthTrackerStatus;
import com.navigatingcancer.healthtracker.api.data.model.HealthTrackerStatusCategory;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

public interface CustomHealthTrackerStatusRepository {
  List<HealthTrackerStatus> findStatuses(
      List<Long> clinicIds, List<Long> locationIds, List<Long> patientIds);

  List<HealthTrackerStatus> findByIds(Long clinicId, List<String> ids);

  HealthTrackerStatus getById(String id);

  HealthTrackerStatus updateNextScheduleDate(String id, LocalDateTime date);

  HealthTrackerStatus setEndCurrentCycle(String id, boolean state);

  HealthTrackerStatus updateMissedCheckinDate(String id, Instant date);

  HealthTrackerStatus updateCategory(String id, HealthTrackerStatusCategory cat);

  HealthTrackerStatus findAndReplaceStatus(HealthTrackerStatus status);
}
