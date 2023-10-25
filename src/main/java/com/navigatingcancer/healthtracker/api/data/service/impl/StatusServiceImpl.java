package com.navigatingcancer.healthtracker.api.data.service.impl;

import com.navigatingcancer.healthtracker.api.data.model.CheckInData;
import com.navigatingcancer.healthtracker.api.data.model.CheckInType;
import com.navigatingcancer.healthtracker.api.data.model.Enrollment;
import com.navigatingcancer.healthtracker.api.data.model.HealthTrackerStatus;
import com.navigatingcancer.healthtracker.api.data.repo.CheckInRepository;
import com.navigatingcancer.healthtracker.api.data.repo.HealthTrackerStatusRepository;
import com.navigatingcancer.healthtracker.api.data.service.CheckInService;
import com.navigatingcancer.healthtracker.api.data.service.StatusService;
import com.navigatingcancer.healthtracker.api.rest.representation.HealthTrackerStatusResponse;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class StatusServiceImpl implements StatusService {

  @Autowired private HealthTrackerStatusRepository statusRepository;

  @Autowired private CheckInRepository checkInRepository;

  @Autowired private CheckInService checkInService;

  @Override
  public List<HealthTrackerStatus> getByIds(Long clinicId, List<String> ids) {
    log.debug("StatusService::getByIds");
    return statusRepository.findByIds(clinicId, ids);
  }

  @Override
  public List<HealthTrackerStatusResponse> getManualCollectDueByIds(
      Long clinicId, List<String> ids) {
    log.debug("StatusService::getManualCollectDueByIds");
    List<HealthTrackerStatusResponse> results = new ArrayList<>();
    // find enrollments that have pending/missed checkins
    List<String> dueIds = findCheckInsDue(ids);

    if (dueIds == null || dueIds.size() == 0) {
      // nothing to find
      return results;
    }

    List<CheckInData> checkins = checkInService.getCheckInDataByEnrollmentIDs(dueIds, false);
    // get these enrollments and put in map
    Map<String, CheckInData> enrollments = new HashMap<>();
    checkins.forEach(
        checkin -> {
          log.debug("checkin: {}", checkin);
          Enrollment enrollment = checkin.getEnrollment();
          log.debug("enrollment: {}", enrollment);
          enrollments.put(enrollment.getId(), checkin);
        });
    log.debug("enrollments: {}", enrollments);
    // get these status and build list of response objects
    statusRepository
        .findByIds(clinicId, dueIds)
        .forEach(
            status -> {
              CheckInData checkInData = enrollments.get(status.getId());
              if (checkInData != null) {
                log.debug("checkInData: {}", checkInData);
                Enrollment enrollment = checkInData.getEnrollment();
                log.debug("enrollment: {}", enrollment);
                results.add(
                    new HealthTrackerStatusResponse(
                        status, checkInData.getEnrollment(), checkInData));
              }
            });
    return results;
  }

  // Finds checkins that could be completed now.
  // Returns enrollment ids if last symptom check in is pending,
  // or is last oral checkin is pending or missed
  public List<String> findCheckInsDue(List<String> enrollmentIds) {
    Set<String> resultIds = new HashSet<>();
    List<String> symptomStatuses = Arrays.asList("PENDING");
    List<String> oralStatuses = Arrays.asList("PENDING", "MISSED");
    resultIds.addAll(
        checkInRepository.findCheckIns(enrollmentIds, symptomStatuses, CheckInType.SYMPTOM));
    resultIds.addAll(checkInRepository.findCheckIns(enrollmentIds, oralStatuses, CheckInType.ORAL));
    return new ArrayList<>(resultIds);
  }
}
