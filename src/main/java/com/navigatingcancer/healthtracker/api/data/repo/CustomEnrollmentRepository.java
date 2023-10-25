package com.navigatingcancer.healthtracker.api.data.repo;

import com.navigatingcancer.healthtracker.api.data.model.CheckInSchedule;
import com.navigatingcancer.healthtracker.api.data.model.Enrollment;
import com.navigatingcancer.healthtracker.api.data.model.EnrollmentStatus;
import com.navigatingcancer.healthtracker.api.data.model.EnrollmentStatusLog;
import com.navigatingcancer.healthtracker.api.rest.QueryParameters.EnrollmentQuery;
import java.util.Date;
import java.util.List;

public interface CustomEnrollmentRepository {
  List<Enrollment> findEnrollmentsByIds(Long clinicId, List<String> ids);

  List<Enrollment> findEnrollments(EnrollmentQuery params);

  Boolean activeEnrollmentExists(long patientId, long clinicId);

  List<Enrollment> getCurrentEnrollments(
      List<Long> clinicIds,
      List<Long> locationIds,
      List<Long> providerIds,
      Boolean isManualCollect);

  Enrollment setStatus(String id, EnrollmentStatus status);

  Enrollment updateConsentStatus(String consentRequestId, String consentStatus, Date updatedDate);

  Enrollment appendStatusLog(String id, EnrollmentStatusLog log);

  /**
   * Update a check-in schedule embedded in an enrollment by its CheckInType.
   *
   * <p>Note this method is intended to be used only for adding IDs to a CheckInSchedule, updating
   * schedules *with* IDs should be done by targeting those specific IDs.
   *
   * @param enrollment
   * @param schedule
   * @return whether an update was applied or not
   * @throws IllegalArgumentException if {@code schedule} doesn't have a check-in type
   */
  boolean updateCheckInScheduleByCheckInType(Enrollment enrollment, CheckInSchedule schedule);

  CheckInSchedule getCheckInScheduleById(String checkInScheduleId);
}
