package com.navigatingcancer.healthtracker.api.events;

import com.navigatingcancer.healthtracker.api.data.model.CheckIn;
import com.navigatingcancer.healthtracker.api.data.model.CheckInSchedule;
import com.navigatingcancer.healthtracker.api.data.model.Enrollment;
import com.navigatingcancer.healthtracker.api.data.model.HealthTrackerEvent;
import com.navigatingcancer.healthtracker.api.data.model.schedule.TriggerPayload.TriggerType;
import com.navigatingcancer.healthtracker.api.data.service.EnrollmentService;
import com.navigatingcancer.healthtracker.api.data.service.impl.SchedulingServiceImpl;
import com.navigatingcancer.healthtracker.api.metrics.HealthTrackerCounterMetric;
import com.navigatingcancer.healthtracker.api.metrics.MetersService;
import com.navigatingcancer.healthtracker.api.processor.HealthTrackerStatusService;
import com.navigatingcancer.scheduler.client.domain.SchedulePayloadBuilder;
import com.navigatingcancer.scheduler.client.domain.TriggerEvent;
import java.time.LocalDate;
import java.time.LocalTime;
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

  protected QuartzStatusEvent(Enrollment en, CheckInSchedule cis) {
    super(en, cis);
  }

  protected void closeEnrollmentIfDone(LocalDate checkDate) {
    // don't mark continuous schedules completed (scheduling service keeps going)
    if (enrollment.getCycles() > 0
        && SchedulingServiceImpl.isEnrollmentExpired(checkDate, enrollment)) {
      enrollment = enrollmentService.completeEnrollment(enrollment);
    }
  }

  private Long setMissedCheckins(LocalDate scheduledDate) {
    // Update status for missed check-in, return the number of changed records,
    // could be zero
    Long res = 0l;
    List<CheckIn> missed =
        customCheckInRepository.getMissedCheckins(enrollment.getId(), scheduledDate);
    if (missed.size() > 0) {
      // TODO. There is a race here. If some other thread does the same, we can get
      // different numbers in two places
      res = customCheckInRepository.setMissedCheckins(enrollment.getId(), scheduledDate);
      // Send the count to DD
      metersService.incrementCounter(
          enrollment.getClinicId(), HealthTrackerCounterMetric.CHECKIN_MISSED, res);
      // Prepare log record, it is returned to default action by the function below
      eventRecord = new HealthTrackerEvent();
      eventRecord.setType(HealthTrackerEvent.Type.CHECK_IN_MISSED);
      List<String> missedIds = missed.stream().map(c -> c.getId()).collect(Collectors.toList());
      eventRecord.setRelatedCheckinId(missedIds);
      // How many missed checkins since last completed checkin
      eventRecord.setMissedCheckinsCount(
          customCheckInRepository.getLastMissedCheckinsCount(enrollment.getId()));
    }
    return res;
  }

  public HealthTrackerEvent prepareEventRecord() {
    return eventRecord; // If there is something to report it should be here
  }

  @Override
  public void onEvent() {
    LocalDate checkDate = scheduledDateTime.toLocalDate();
    // Always close missed checkins even if enrollment is done
    Long missed = setMissedCheckins(checkDate);
    if (missed != 0l) {
      healthTrackerStatusRepository.updateMissedCheckinDate(htStatus.getId(), getEventInstant());
    }

    // Act on status only if enrollment is still open
    if (Boolean.TRUE.equals(isEnrollmentActive())) {
      closeEnrollmentIfDone(checkDate);
      if (missed != 0l) {
        // The only status change that we consider at this point is the missed checkins
        healthTrackerStatusService.processStatus(enrollment.getId(), null, null);
      }
    } else {
      log.info("Ignore status event since enrollment status is not active {}", enrollment);
    }

    // Always persist missed checkins event
    if (missed != 0l) {
      logEvent();
    }
  }

  @Override
  public TriggerType getTriggerType() {
    return TriggerType.STATUS;
  }

  @Override
  public SchedulePayloadBuilder makeSchedulePayloadBuilder(CheckInSchedule checkInSchedule) {
    String triggerId =
        SchedulingServiceImpl.getTriggerId(checkInSchedule.getCheckInType(), TriggerType.STATUS);
    SchedulePayloadBuilder builder = SchedulePayloadBuilder.create(triggerId, triggerPayload);
    builder.at(LocalTime.MAX, enrollment.getReminderTimeZone());
    builder.ignoreMisfires();
    return builder;
  }
}
