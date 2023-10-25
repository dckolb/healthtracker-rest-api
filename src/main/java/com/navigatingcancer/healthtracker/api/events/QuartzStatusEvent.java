package com.navigatingcancer.healthtracker.api.events;

import com.google.common.collect.ImmutableList;
import com.navigatingcancer.healthtracker.api.data.model.*;
import com.navigatingcancer.healthtracker.api.data.service.EnrollmentService;
import com.navigatingcancer.healthtracker.api.data.service.impl.SchedulingServiceImpl;
import com.navigatingcancer.healthtracker.api.metrics.HealthTrackerCounterMetric;
import com.navigatingcancer.healthtracker.api.metrics.MetersService;
import com.navigatingcancer.healthtracker.api.processor.HealthTrackerStatusService;
import com.navigatingcancer.scheduler.client.domain.TriggerEvent;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class QuartzStatusEvent extends QuartzCheckinEvent {

  protected EnrollmentService enrollmentService;
  protected HealthTrackerStatusService healthTrackerStatusService;
  private HealthTrackerEvent eventRecord;
  private MetersService metersService;

  public QuartzStatusEvent(TriggerEvent triggerEvent, SchedulingServiceImpl ss) {
    super(triggerEvent, ss);
    this.enrollmentService = ss.getEnrollmentService();
    this.healthTrackerStatusService = ss.getHealthTrackerStatusService();
    this.metersService = ss.getMetersService();
    this.eventRecord = null;
  }

  protected void closeEnrollmentIfDone(LocalDate checkDate) {
    // don't mark continuous schedules completed (scheduling service keeps going)
    if (enrollment.getCycles() > 0 && isEnrollmentExpired(checkDate, enrollment)) {
      enrollment = enrollmentService.completeEnrollment(enrollment);
    }
  }

  // Function to check if the (current) date is beyond all schedules last date or
  // is the last date
  public static final boolean isEnrollmentExpired(
      final LocalDate date, final Enrollment enrollment) {
    // Find if there is any active schedule
    boolean anyActive =
        enrollment.getSchedules().stream().anyMatch(c -> !isScheduleExpired(date, enrollment, c));
    return !anyActive;
  }

  // Function to check if the (current) date is beyond the schedule last date or is the last date
  private static boolean isScheduleExpired(
      LocalDate date, Enrollment enrollment, CheckInSchedule c) {
    if (enrollment.getSchedules().isEmpty()) return false;
    Integer cycles = enrollment.getCycles();
    if (cycles == null || cycles.intValue() == 0) {
      log.warn(
          "isScheduleExpired expected to get called against schedules that can expire, {}",
          enrollment);
      return false; // no cycles defined - no expiration date
    }

    LocalDate endDate = c.getEndDate();
    if (endDate == null) {
      long treatmentDays = daysInCycles(enrollment);
      endDate = c.getStartDate().plusDays(treatmentDays - 1);
    }
    return endDate.isBefore(date) || endDate.isEqual(date);
  }

  private static long daysInCycles(Enrollment enrollment) {
    long treatmentDays = 0;
    Integer cycles = enrollment.getCycles();
    if (cycles == null || cycles.intValue() == 0) {
      treatmentDays = Integer.MAX_VALUE; // no cycles defined - no end to schedule
    } else {
      Integer daysInCycle = enrollment.getDaysInCycle();
      treatmentDays =
          (long) cycles
              * (daysInCycle == null
                  ? 1
                  : daysInCycle.intValue()); // TODO. What if "days in cycles" is null?
    }
    return treatmentDays;
  }

  /**
   * Update status for missed check-in, return the updated checkins.
   *
   * @param scheduledDate
   * @return
   */
  private List<CheckIn> setMissedCheckins(LocalDate scheduledDate) {
    List<CheckIn> missed = checkInRepository.getMissedCheckins(enrollment.getId(), scheduledDate);

    if (missed.isEmpty()) {
      return List.of();
    }

    // TODO. There is a race here. If some other thread does the same, we can get
    // different numbers in two places
    long res = checkInRepository.setMissedCheckins(enrollment.getId(), scheduledDate);
    // Send the count to DD
    metersService.incrementCounter(
        enrollment.getClinicId(), HealthTrackerCounterMetric.CHECKIN_MISSED, res);

    // Prepare log record, it is returned to default action by the function below
    eventRecord = new HealthTrackerEvent();
    eventRecord.setType(HealthTrackerEvent.Type.CHECK_IN_MISSED);

    List<String> missedIds =
        missed.stream().map(AbstractDocument::getId).collect(Collectors.toList());
    eventRecord.setRelatedCheckinId(missedIds);

    // How many missed checkins since last completed checkin
    eventRecord.setMissedCheckinsCount(
        checkInRepository.getLastMissedCheckinsCount(enrollment.getId()));

    // reload the checkins
    return ImmutableList.copyOf(checkInRepository.findAllById(missedIds));
  }

  @Override
  public HealthTrackerEvent prepareEventRecord() {
    return eventRecord; // If there is something to report it should be here
  }

  @Override
  public void onEvent() {
    LocalDate checkDate = scheduledDateTime.toLocalDate();
    // Always close missed checkins even if enrollment is done
    List<CheckIn> missed = setMissedCheckins(checkDate);
    if (!missed.isEmpty()) {
      healthTrackerStatusRepository.updateMissedCheckinDate(htStatus.getId(), getEventInstant());
    }

    // Act on status only if enrollment is still open
    if (Boolean.TRUE.equals(isEnrollmentActive())) {
      closeEnrollmentIfDone(checkDate);
      if (!missed.isEmpty()) {
        // The only status change that we consider at this point is the missed checkins
        healthTrackerStatusService.processStatus(
            enrollment.getId(),
            null,
            missed.stream().map(AbstractDocument::getId).collect(Collectors.toList()));
      }
    } else {
      log.info("Ignore status event since enrollment status is not active {}", enrollment);
    }

    // Always persist missed checkins event
    if (!missed.isEmpty()) {
      logEvent();
    }
  }
}
