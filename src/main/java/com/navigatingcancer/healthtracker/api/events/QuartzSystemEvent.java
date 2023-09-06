package com.navigatingcancer.healthtracker.api.events;

import com.navigatingcancer.date.utils.DateTimeUtils;
import com.navigatingcancer.healthtracker.api.data.model.CheckIn;
import com.navigatingcancer.healthtracker.api.data.model.CheckInSchedule;
import com.navigatingcancer.healthtracker.api.data.model.CheckInStatus;
import com.navigatingcancer.healthtracker.api.data.model.CheckInType;
import com.navigatingcancer.healthtracker.api.data.model.Enrollment;
import com.navigatingcancer.healthtracker.api.data.model.schedule.TriggerPayload.TriggerType;
import com.navigatingcancer.healthtracker.api.data.service.impl.SchedulingServiceImpl;
import com.navigatingcancer.healthtracker.api.metrics.HealthTrackerCounterMetric;
import com.navigatingcancer.healthtracker.api.metrics.MetersService;
import com.navigatingcancer.scheduler.client.domain.SchedulePayloadBuilder;
import com.navigatingcancer.scheduler.client.domain.TriggerEvent;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class QuartzSystemEvent extends QuartzCheckinEvent {

  private MetersService metersService;

  public QuartzSystemEvent(TriggerEvent triggerEvent, SchedulingServiceImpl ss) {
    super(triggerEvent, ss);
    this.metersService = ss.getMetersService();
  }

  protected QuartzSystemEvent(Enrollment en, CheckInSchedule cis) {
    super(en, cis);
  }

  @Override
  public void onEvent() {
    if (!Boolean.TRUE.equals(isEnrollmentActive())) {
      log.info("Ignore system event since enrollment status is not active {}", enrollment);
      return;
    }

    String enrollmentId = enrollment.getId();

    boolean skipNextCheckin = false;
    if (isAutoClosed()) {
      // need to see if this ended in previous cycle, regardless of day in current
      // cycle
      // find last completed SYMPTOM checkin
      CheckIn lastCompleted =
          checkInRepository
              .findByEnrollmentIdAndCheckInTypeAndStatusOrderByScheduleDateDesc(
                  enrollmentId, CheckInType.SYMPTOM, CheckInStatus.COMPLETED)
              .findFirst()
              .orElse(null);
      if (lastCompleted == null) {
        // should be in here
        log.error(
            "seems that endCurrentCycle is set with no previous checkin for enrollment {}",
            enrollmentId);
        skipNextCheckin = false;
      } else {
        int lastCompletedCheckInCycle =
            SchedulingServiceImpl.getCycleNumberForDate(
                enrollment, lastCompleted.getScheduleDate());
        int scheduleDateCycle =
            SchedulingServiceImpl.getCycleNumberForDate(
                enrollment, scheduledDateTime.toLocalDate());
        log.debug(
            "last completed has cycle number of {} and schedule date is cycle number {}",
            lastCompletedCheckInCycle,
            scheduleDateCycle);
        if (scheduleDateCycle > lastCompletedCheckInCycle) {
          htStatus = healthTrackerStatusRepository.setEndCurrentCycle(htStatus.getId(), false);
        } else {
          skipNextCheckin = true;
        }
      }
    }
    log.debug("checking if in past");
    // Create new checkin if enrollment is not auto-closed and if checkin date is
    // today or in the future
    if (!skipNextCheckin
        && SchedulingServiceImpl.isNotInThePast(scheduledDateTime, getReminderTimeZoneId())) {
      log.debug("creating checkin");
      CheckIn checkIn = new CheckIn(enrollment);
      checkIn.setCheckInType(triggerPayload.getCheckInType());
      checkIn.setStatus(CheckInStatus.PENDING);
      checkIn.setScheduleDate(scheduledDateTime.toLocalDate());
      checkIn.setScheduleTime(triggerPayload.getCheckInTime());
      log.debug("saving checkin");
      customCheckInRepository.upsertByNaturalKey(checkIn);
      // checkInRepository.save(checkIn);

      // Send the count to DD
      metersService.incrementCounter(
          enrollment.getClinicId(), HealthTrackerCounterMetric.CHECKIN_CREATED);

      if (htStatus
          != null) { // TODO. HT Status can be null only in some tests. We should fix tests instead.
        Date nextScheduledDateTime = triggerEvent.getNextFireTime();
        LocalDateTime date = null;
        if (nextScheduledDateTime != null) {
          date =
              DateTimeUtils.toLocalDateTime(
                  nextScheduledDateTime, enrollment.getReminderTimeZone());
        }
        htStatus = healthTrackerStatusRepository.updateNextScheduleDate(htStatus.getId(), date);
      }
    }
  }

  @Override
  public TriggerType getTriggerType() {
    return TriggerType.SYSTEM;
  }

  @Override
  public SchedulePayloadBuilder makeSchedulePayloadBuilder(CheckInSchedule checkInSchedule) {
    String triggerId =
        SchedulingServiceImpl.getTriggerId(checkInSchedule.getCheckInType(), TriggerType.SYSTEM);
    SchedulePayloadBuilder builder = SchedulePayloadBuilder.create(triggerId, triggerPayload);
    builder.at(LocalTime.MIDNIGHT, enrollment.getReminderTimeZone());
    return builder;
  }
}
