package com.navigatingcancer.healthtracker.api.data.service.impl;

import com.navigatingcancer.healthtracker.api.data.model.*;
import com.navigatingcancer.healthtracker.api.data.repo.CheckInRepository;
import com.navigatingcancer.healthtracker.api.data.repo.EnrollmentRepository;
import com.navigatingcancer.healthtracker.api.data.service.CheckInScheduleService;
import com.navigatingcancer.healthtracker.api.rest.exception.RecordNotFoundException;
import com.navigatingcancer.healthtracker.api.rest.exception.UnknownEnrollmentException;
import com.navigatingcancer.healthtracker.api.rest.representation.CheckInResponse;
import com.navigatingcancer.healthtracker.api.rest.representation.ScheduleCheckInDetailsResponse;
import java.util.Objects;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CheckInScheduleServiceImpl implements CheckInScheduleService {
  private final EnrollmentRepository enrollmentRepository;
  private final CheckInRepository checkInRepository;

  @Autowired
  public CheckInScheduleServiceImpl(
      EnrollmentRepository enrollmentRepository, CheckInRepository checkInRepository) {
    this.enrollmentRepository = enrollmentRepository;
    this.checkInRepository = checkInRepository;
  }

  @Override
  public ScheduleCheckInDetailsResponse getCheckInDetailsBySchedule(
      String enrollmentId, String scheduleId, Optional<CheckInStatus> checkInStatus) {

    var enrollment =
        enrollmentRepository
            .findById(enrollmentId)
            .orElseThrow(() -> new UnknownEnrollmentException("unknown enrollment"));

    var schedule =
        enrollment.getSchedules().stream()
            .filter(s -> Objects.equals(s.getId(), scheduleId))
            .findFirst()
            .orElseThrow(
                () -> new RecordNotFoundException("unknown schedule by id for enrollment"));

    return buildScheduleCheckInDetails(enrollment, schedule, checkInStatus);
  }

  @Override
  public ScheduleCheckInDetailsResponse getCheckInDetailsBySchedule(
      String enrollmentId, CheckInType checkInType, Optional<CheckInStatus> checkInStatus) {

    var enrollment =
        enrollmentRepository
            .findById(enrollmentId)
            .orElseThrow(() -> new UnknownEnrollmentException("unknown enrollment"));

    var schedule =
        enrollment.getSchedules().stream()
            .filter(s -> checkInType == s.getCheckInType())
            .findFirst()
            .orElseThrow(
                () ->
                    new RecordNotFoundException(
                        "unknown schedule by check-in type for enrollment"));

    return buildScheduleCheckInDetails(enrollment, schedule, checkInStatus);
  }

  @Override
  public ScheduleCheckInDetailsResponse getCheckInDetailsBySchedule(
      String checkInScheduleId, Optional<CheckInStatus> checkInStatus) {

    var schedule = enrollmentRepository.getCheckInScheduleById(checkInScheduleId);
    var enrollment = enrollmentRepository.findById(schedule.getEnrollmentId()).orElseThrow();

    return buildScheduleCheckInDetails(enrollment, schedule, checkInStatus);
  }

  private ScheduleCheckInDetailsResponse buildScheduleCheckInDetails(
      Enrollment enrollment, CheckInSchedule schedule, Optional<CheckInStatus> checkInStatus) {

    var checkIns = checkInRepository.findCheckInsBySchedule(enrollment.getId(), schedule);
    var dates = CheckInDates.forSchedule(enrollment, schedule, checkIns);

    // filter resulting check-ins by status if requested
    if (checkInStatus.isPresent()) {
      checkIns =
          checkIns.stream().filter(checkIn -> checkIn.getStatus() == checkInStatus.get()).toList();
    }

    var res = new ScheduleCheckInDetailsResponse();
    res.setCheckInScheduleId(schedule.getId());
    res.setEnrollmentId(enrollment.getId());
    res.setLastCheckInDate(dates.getLastCheckInDate());
    res.setNextCheckInDate(dates.getNextCheckInDate());
    res.setCheckIns(checkIns.stream().map(checkIn -> new CheckInResponse(checkIn)).toList());
    return res;
  }
}
