package com.navigatingcancer.healthtracker.api.events;

import static java.time.temporal.ChronoUnit.DAYS;

import com.navigatingcancer.date.utils.DateTimeUtils;
import com.navigatingcancer.healthtracker.api.data.model.*;
import com.navigatingcancer.healthtracker.api.data.service.CheckInCreationService;
import com.navigatingcancer.healthtracker.api.data.service.impl.SchedulingServiceImpl;
import com.navigatingcancer.scheduler.client.domain.TriggerEvent;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class QuartzSystemEvent extends QuartzCheckinEvent {

  private CheckInCreationService checkInCreationService;

  public QuartzSystemEvent(TriggerEvent triggerEvent, SchedulingServiceImpl ss) {
    super(triggerEvent, ss);
    this.checkInCreationService = ss.getCheckInCreationService();
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
      // FIXME: review usage of check-in type here --
      //        how does this break with survey instances?
      //        why only symptom checkins?
      //        see https://navigatingcancer.atlassian.net/browse/HT-874
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
            getCycleNumberForDate(enrollment, lastCompleted.getScheduleDate());
        int scheduleDateCycle = getCycleNumberForDate(enrollment, scheduledDateTime.toLocalDate());
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
    if (!skipNextCheckin && isNotInThePast(scheduledDateTime, getReminderTimeZoneId())) {

      var schedule =
          enrollment.getSchedules().stream()
              .filter(
                  s -> {
                    // match schedule by id if available
                    if (triggerPayload.getCheckInScheduleId() != null) {
                      return Objects.equals(s.getId(), triggerPayload.getCheckInScheduleId());

                      // otherwise, match on check in type
                    } else {
                      return s.getCheckInType() == triggerPayload.getCheckInType();
                    }
                  })
              .findFirst();

      if (schedule.isEmpty()) {
        log.warn(
            "skipping check-in creation, unable to match schedule to payload {}", triggerPayload);
        return;
      }

      checkInCreationService.createCheckInForSchedule(
          enrollment,
          schedule.get(),
          LocalDateTime.of(scheduledDateTime.toLocalDate(), triggerPayload.getCheckInTime()));

      updateNextScheduleDate();
    }
  }

  // Return TRUE if the date is today or some time in the future
  private static boolean isNotInThePast(LocalDateTime d, ZoneId tz) {
    return !d.isBefore(LocalDate.now(tz).atStartOfDay());
  }

  private static int getCycleNumberForDate(Enrollment enrollment, LocalDate date) {
    LocalDate startDate = enrollment.getStartDate();
    if (startDate == null) return 1;
    Long daysFromStart = DAYS.between(startDate.atStartOfDay(), date.atStartOfDay());
    return (int) (daysFromStart / enrollment.getDaysInCycle() + 1);
  }

  private void updateNextScheduleDate() {
    if (htStatus
        != null) { // TODO. HT Status can be null only in some tests. We should fix tests instead.
      Date nextScheduledDateTime = triggerEvent.getNextFireTime();
      LocalDateTime date = null;
      if (nextScheduledDateTime != null) {
        date =
            DateTimeUtils.toLocalDateTime(nextScheduledDateTime, enrollment.getReminderTimeZone());
      }
      htStatus = healthTrackerStatusRepository.updateNextScheduleDate(htStatus.getId(), date);
    }
  }
}
