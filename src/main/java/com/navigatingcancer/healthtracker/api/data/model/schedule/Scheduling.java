package com.navigatingcancer.healthtracker.api.data.model.schedule;

import com.navigatingcancer.healthtracker.api.data.model.CheckInFrequency;
import com.navigatingcancer.healthtracker.api.data.model.CheckInSchedule;
import com.navigatingcancer.healthtracker.api.data.model.Enrollment;
import com.navigatingcancer.healthtracker.api.data.model.schedule.TriggerPayload.TriggerType;
import com.navigatingcancer.scheduler.client.domain.SchedulePayload;
import com.navigatingcancer.scheduler.client.domain.SchedulePayloadBuilder;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * Utility class responsible for transforming enrollments and check-in schedules into scheduled item
 * payloads used by the scheduler service. These payloads are used to configured scheduled events
 * triggers.
 */
@Slf4j
public class Scheduling {

  public static SchedulePayload createSchedulePayload(
      Enrollment enrollment, List<CheckInSchedule> checkInSchedules) {
    return new PayloadBuilder(enrollment, checkInSchedules).build();
  }

  private static class PayloadBuilder {
    private final Enrollment enrollment;
    private final List<CheckInSchedule> checkInSchedules;
    private final List<SchedulePayload.ScheduleItemPayload> items = new ArrayList<>();

    private PayloadBuilder(Enrollment enrollment, List<CheckInSchedule> checkInSchedules) {
      this.enrollment = enrollment;
      this.checkInSchedules = checkInSchedules;
    }

    private SchedulePayload build() {
      for (var schedule : checkInSchedules) {
        // Schedule system trigger which creates check-ins at midnight
        addCheckInSchedule(schedule, TriggerType.SYSTEM, LocalTime.MIDNIGHT, false);
        // Schedule reminder trigger which sends notifications
        addCheckInSchedule(schedule, TriggerType.REMINDER, getReminderTime(enrollment), true);
        // Schedule status calculation
        addCheckInSchedule(schedule, TriggerType.STATUS, LocalTime.MAX, true);
      }

      // schedule enrollment life-cycle events
      addEnrollmentEndSchedule();
      addEnrollmentStartSchedule();
      addCycleEndSchedule();
      addCycleStartSchedule();

      SchedulePayload payload = new SchedulePayload();
      payload.setItems(items);
      return payload;
    }

    private void addEnrollmentLifecycleSchedule(
        TriggerType messageType, Consumer<SchedulePayloadBuilder> customizer) {
      TriggerPayload triggerPayload =
          new TriggerPayload(enrollment.getId(), LocalTime.MIDNIGHT, messageType);
      String triggerId = getTriggerId(enrollment.getId(), messageType);
      SchedulePayloadBuilder builder = SchedulePayloadBuilder.create(triggerId, triggerPayload);
      builder.at(LocalTime.MIDNIGHT, enrollment.getReminderTimeZone());

      customizer.accept(builder);

      items.add(builder.build());
    }

    private void addCycleEndSchedule() {
      addEnrollmentLifecycleSchedule(
          TriggerType.CYCLE_END,
          builder -> {
            LocalDate startDate = enrollment.getStartDate().plusDays(enrollment.getDaysInCycle());
            LocalDate endDate = CheckInSchedule.getLastScheduledDate(enrollment.getSchedules());
            if (endDate != null && endDate.isBefore(startDate)) {
              endDate = startDate; // if there is only one cycle, the end date is last date + 1 day
            }
            builder.cycle(startDate, endDate, enrollment.getDaysInCycle());
          });
    }

    private void addCycleStartSchedule() {
      addEnrollmentLifecycleSchedule(
          TriggerType.CYCLE_START,
          builder -> {
            LocalDate startDate = enrollment.getStartDate();
            LocalDate endDate = CheckInSchedule.getLastScheduledDate(enrollment.getSchedules());

            builder.cycle(startDate, endDate, enrollment.getDaysInCycle());
          });
    }

    private void addEnrollmentEndSchedule() {
      var schedules = enrollment.getSchedules();

      if (CheckInSchedule.getLastScheduledDate(schedules) == null) {
        log.info(
            "skipping scheduling for ENROLLMENT_END event; no schedules found for enrollment {}",
            enrollment.getId());
        return;
      }

      addEnrollmentLifecycleSchedule(
          TriggerType.ENROLLMENT_END,
          builder -> {
            // Last status calculation at the start of the new date after the end of the
            // schedule
            LocalDate endDate = CheckInSchedule.getLastScheduledDate(enrollment.getSchedules());
            LocalDate checkDate = endDate.plusDays(1);
            builder.daily(checkDate, checkDate);
          });
    }

    private void addEnrollmentStartSchedule() {
      addEnrollmentLifecycleSchedule(
          TriggerType.ENROLLMENT_START,
          builder -> {
            LocalDate startDate = enrollment.getStartDate();
            builder.daily(startDate, startDate);
          });
    }

    private void addCheckInSchedule(
        CheckInSchedule checkInSchedule,
        TriggerType type,
        LocalTime scheduledTime,
        boolean ignoreMisfires) {

      if (checkInSchedule.getEndDate() != null
          && (LocalDate.now().isAfter(checkInSchedule.getEndDate())
              || checkInSchedule.getStartDate().isAfter(checkInSchedule.getEndDate()))) {
        return;
      }

      final TriggerPayload triggerPayload;
      if (checkInSchedule.getId() != null) {
        triggerPayload =
            new TriggerPayload(
                enrollment.getId(), checkInSchedule.getId(), getReminderTime(enrollment), type);
      } else {
        triggerPayload =
            new TriggerPayload(
                enrollment.getId(),
                checkInSchedule.getCheckInType(),
                getReminderTime(enrollment),
                type);
      }

      String triggerId = getTriggerId(checkInSchedule.getId(), type);

      SchedulePayloadBuilder builder = SchedulePayloadBuilder.create(triggerId, triggerPayload);

      builder.at(scheduledTime, enrollment.getReminderTimeZone());
      if (ignoreMisfires) {
        builder.ignoreMisfires();
      }

      // Day part of the schedule
      LocalDate startDate = checkInSchedule.getStartDate();
      if (checkInSchedule.getCheckInFrequency() == CheckInFrequency.DAILY) {
        builder.daily(startDate, checkInSchedule.getEndDate());
      } else if (checkInSchedule.getCheckInFrequency() == CheckInFrequency.WEEKDAY) {
        builder.weekdays(startDate, checkInSchedule.getEndDate());
      } else if (checkInSchedule.getCheckInFrequency() == CheckInFrequency.WEEKLY) {
        builder.weekly(startDate, checkInSchedule.getEndDate(), checkInSchedule.getWeeklyDays());
      } else if (checkInSchedule.getCheckInFrequency() == CheckInFrequency.CUSTOM) {
        builder.custom(
            startDate,
            checkInSchedule.getEndDate(),
            checkInSchedule.getCycleDays(),
            enrollment.getDaysInCycle());
      }

      items.add(builder.build());
    }

    private static String getTriggerId(String baseId, TriggerPayload.TriggerType type) {
      if (StringUtils.isBlank(baseId)) {
        baseId = UUID.randomUUID().toString();
      }
      return String.format("%s-%s", baseId, type.name());
    }

    private static LocalTime getReminderTime(Enrollment enrollment) {
      return LocalTime.parse(enrollment.getReminderTime(), DateTimeFormatter.ofPattern("H:mm"));
    }
  }
}
