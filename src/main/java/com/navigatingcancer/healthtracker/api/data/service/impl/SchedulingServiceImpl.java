package com.navigatingcancer.healthtracker.api.data.service.impl;

import static java.time.temporal.ChronoUnit.DAYS;

import com.navigatingcancer.date.utils.DateTimeUtils;
import com.navigatingcancer.healthtracker.api.data.model.*;
import com.navigatingcancer.healthtracker.api.data.model.schedule.TriggerPayload;
import com.navigatingcancer.healthtracker.api.data.model.schedule.TriggerPayload.TriggerType;
import com.navigatingcancer.healthtracker.api.data.repo.CheckInRepository;
import com.navigatingcancer.healthtracker.api.data.repo.CustomCheckInRepository;
import com.navigatingcancer.healthtracker.api.data.repo.EnrollmentRepository;
import com.navigatingcancer.healthtracker.api.data.repo.HealthTrackerEventsRepository;
import com.navigatingcancer.healthtracker.api.data.repo.HealthTrackerStatusRepository;
import com.navigatingcancer.healthtracker.api.data.service.EnrollmentService;
import com.navigatingcancer.healthtracker.api.events.QuartzEvent;
import com.navigatingcancer.healthtracker.api.metrics.MetersService;
import com.navigatingcancer.healthtracker.api.processor.HealthTrackerStatusService;
import com.navigatingcancer.healthtracker.api.rest.exception.UnknownEnrollmentException;
import com.navigatingcancer.scheduler.client.domain.SchedulePayload;
import com.navigatingcancer.scheduler.client.domain.SchedulePayload.ScheduleItemPayload;
import com.navigatingcancer.scheduler.client.domain.SchedulePayloadBuilder;
import com.navigatingcancer.scheduler.client.domain.TriggerEvent;
import com.navigatingcancer.scheduler.client.service.SchedulerServiceClient;
import com.navigatingcancer.sqs.SqsListener;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;

@SqsListener(queueName = SchedulingServiceImpl.QUEUE_VAR_NAME)
@Service
@Slf4j
public class SchedulingServiceImpl implements Consumer<TriggerEvent> {

  @Value(QUEUE_VAR_NAME)
  String queueName;

  static final String QUEUE_VAR_NAME = "${ht-reminder-queue}";

  @Autowired private EnrollmentRepository enrollmentRepository;

  @Autowired private EnrollmentService enrollmentService;

  @Autowired private CheckInRepository checkInRepository;

  @Autowired private CustomCheckInRepository customCheckInRepository;

  @Autowired private HealthTrackerStatusRepository healthTrackerStatusRepository;

  @Autowired private SchedulerServiceClient scheduleServiceClient;

  @Autowired private NotificationService notificationService;

  // TODO: remove lazy annoation and refactor to avoid circular dependancy
  @Autowired @Lazy private HealthTrackerStatusService healthTrackerStatusService;

  @Autowired private HealthTrackerEventsRepository healthTrackerEventsRepository;

  @Autowired private MetersService metersService;

  // Expose dependant services to code outside of the scope of this service
  public EnrollmentRepository getEnrollmentRepository() {
    return enrollmentRepository;
  }

  public EnrollmentService getEnrollmentService() {
    return enrollmentService;
  }

  public HealthTrackerStatusService getStatusService() {
    return healthTrackerStatusService;
  }

  public CheckInRepository getCheckinRepository() {
    return checkInRepository;
  }

  public CustomCheckInRepository getCustomCheckinRepository() {
    return customCheckInRepository;
  }

  public HealthTrackerStatusRepository getHealthTrackerStatusRepository() {
    return healthTrackerStatusRepository;
  }

  public NotificationService getNotificationService() {
    return notificationService;
  }

  public HealthTrackerStatusService getHealthTrackerStatusService() {
    return healthTrackerStatusService;
  }

  public HealthTrackerEventsRepository getEventsRepository() {
    return healthTrackerEventsRepository;
  }

  public MetersService getMetersService() {
    return metersService;
  }

  public void schedule(Enrollment enrollment, boolean firstTime) {
    log.debug("SchedulingServiceImpl::schedule");
    if (!firstTime) {
      unschedule(enrollment);
    }

    EnrollmentStatus enrollmentStatus = enrollment.getStatus();

    if (enrollmentStatus != EnrollmentStatus.ACTIVE) {
      log.info("Skipping schedule for {} due status non-active", enrollment);
      return;
    }

    String enrollmentId = enrollment.getId();

    SchedulePayload schedulePayload = new SchedulePayload();
    schedulePayload.setQueueName(queueName);

    // ensure each schedule has a start and possibly end date
    enrollment.validateDates();

    enrollment
        .getSchedules()
        .forEach(
            checkInSchedule -> {
              // Schedule system trigger which creates check-ins at mid-night
              addSchedule(
                  schedulePayload, enrollment, checkInSchedule, TriggerPayload.TriggerType.SYSTEM);
              // Schedule reminder trigger which sends notifications
              addSchedule(
                  schedulePayload,
                  enrollment,
                  checkInSchedule,
                  TriggerPayload.TriggerType.REMINDER);
              // Schedule status calculation
              addSchedule(
                  schedulePayload, enrollment, checkInSchedule, TriggerPayload.TriggerType.STATUS);
            });

    List<ScheduleItemPayload> cycleEvents =
        Stream.of(
                TriggerPayload.TriggerType.ENROLLMENT_END,
                    TriggerPayload.TriggerType.ENROLLMENT_START,
                TriggerPayload.TriggerType.CYCLE_END, TriggerPayload.TriggerType.CYCLE_START)
            .map(t -> QuartzEvent.newCycleEvent(t, enrollment))
            .filter(e -> e != null) // enrollment end may be not applicable, that it is null
            .map(e -> e.makeSchedule())
            .collect(Collectors.toList());
    schedulePayload.getItems().addAll(cycleEvents);

    log.debug("scheduling serivice call payload is {}", schedulePayload);
    if (schedulePayload.getItems() != null && schedulePayload.getItems().size() > 0)
      scheduleServiceClient.getApi().schedule(enrollmentId, schedulePayload);

    try {
      enrollmentRepository.save(enrollment);
    } catch (OptimisticLockingFailureException ex) {
      log.error("Failed to update. Concurrent update of the enrollment " + enrollment.getId(), ex);
      throw new RuntimeException(
          "Changes to the underlying enrollment data detected. Has it been modified meanwhile? Please refresh enrollment and try again.");
    }
  }

  public void remindMeNow(String enrollmentId) {
    log.debug("SchedulingServiceImpl::remindMeNow");

    String deDupeJobId = enrollmentId + "_reminder_me_now";
    setupReminder(deDupeJobId, enrollmentId, 0);
  }

  public void remindMeLater(String enrollmentId, int minutes) {
    log.debug("SchedulingServiceImpl::remindMeLater");

    String dedupeJobId = enrollmentId + "_reminder_me_later";
    setupReminder(dedupeJobId, enrollmentId, minutes);
  }

  private void setupReminder(
      final String deDupeJobId, final String enrollmentId, final int minutes) {

    Enrollment enrollment =
        enrollmentRepository
            .findById(enrollmentId)
            .orElseThrow(
                () ->
                    new UnknownEnrollmentException(
                        String.format("Unknown enrollment id: %s", enrollmentId)));

    SchedulePayload schedulePayload = new SchedulePayload();
    schedulePayload.setQueueName(queueName);

    String tzName = enrollment.getReminderTimeZone();
    ZoneId tzId = DateTimeUtils.toZoneId(tzName);
    LocalTime reminderTime = LocalTime.now(tzId).plusMinutes(minutes);

    TriggerPayload triggerPayload =
        new TriggerPayload(enrollment.getId(), null, reminderTime, TriggerType.REMINDER);
    SchedulePayloadBuilder builder = SchedulePayloadBuilder.create(deDupeJobId, triggerPayload);

    LocalDate dateNow = LocalDate.now(tzId);
    builder.at(reminderTime, tzName).daily(dateNow, dateNow);
    schedulePayload.getItems().add(builder.build());

    scheduleServiceClient.getApi().schedule(deDupeJobId, schedulePayload);
  }

  private void unschedule(Enrollment enrollment) {
    scheduleServiceClient.getApi().stopJob(QUEUE_VAR_NAME, enrollment.getId());
  }

  public Integer getNextTotalCheckInCount(Enrollment enrollment, LocalDate today) {
    log.debug("SchedulingServiceImpl::getNextTotalCheckInCount");
    LocalDate startDate = today;

    for (CheckInSchedule c : enrollment.getSchedules()) {
      if (c.getStartDate().isBefore(startDate)) {
        startDate = c.getStartDate();
      }
    }

    long d = Duration.between(startDate.atStartOfDay(), today.atStartOfDay()).toDays();

    int k = enrollment.getDaysInCycle();

    LocalDate date = startDate;

    if (d > k) {
      date = date.plusDays(d % k);
    }

    Integer result = 0;

    for (int i = 0; i < k; i++) {

      if (date.isAfter(today)) {
        for (CheckInSchedule c : enrollment.getSchedules()) {
          if (isScheduled(date, enrollment, c)) {
            result++;
            break;
          }
        }
        ;
      }

      date = date.plusDays(1);
    }

    log.debug("NextTotalCheckInCount=" + result);

    return result;
  }

  public LocalDate getNextCheckInDate(Enrollment enrollment) {
    return getNextCheckInDate(enrollment, null);
  }

  public LocalDate getNextCheckInDate(Enrollment enrollment, CheckInType checkInType) {
    ZoneId zoneId = DateTimeUtils.toZoneId(enrollment.getReminderTimeZone());
    LocalDate fromDate = LocalDate.now(zoneId); // Look from this date onwards
    LocalDate res = null;

    List<CheckIn> checkins =
        checkInRepository.findByEnrollmentIdOrderByScheduleDateDesc(enrollment.getId());
    // If the dates for a specific checkin type wanted, filter down the list for
    // that checkin type
    if (checkInType != null) {
      checkins =
          checkins.stream()
              .filter(c -> checkInType.equals(c.getCheckInType()))
              .collect(Collectors.toList());
    }

    if (checkins != null && checkins.size() > 0) {
      // find the last finished checkin before reverting the list
      Optional<CheckIn> lastCheckin =
          checkins.stream().filter(c -> c.getStatus() != CheckInStatus.PENDING).findFirst();
      // If there is a pending checkin(s), use earliest pending date that is not before now
      Collections.reverse(checkins);
      final LocalDate fromDateForFilter = fromDate; // need final to make java happy
      Optional<CheckIn> nextcheckin =
          checkins.stream()
              .filter(
                  c ->
                      c.getStatus() == CheckInStatus.PENDING
                          && !c.getScheduleDate().isBefore(fromDateForFilter))
              .findFirst();
      if (nextcheckin.isPresent()) {
        LocalDate dt = nextcheckin.get().getScheduleDate();
        if (dt.isAfter(fromDate) || dt.equals(fromDate)) {
          res = dt;
        }
      } else {
        if (lastCheckin.isPresent()) {
          // If there are no pending checkins, find the date of the last completed or
          // missed checkin.
          // Use next date as the next checkin date candidate, if it is later than previous
          // candidate
          LocalDate fromDate2 = lastCheckin.get().getScheduleDate().plusDays(1);
          if (fromDate2.isAfter(fromDate)) {
            fromDate = fromDate2;
          }
        }
      }
    }
    if (res == null) {
      res = getNextCheckInDate(enrollment, fromDate, checkInType);
    }
    return res;
  }

  private LocalDate getNextCheckInDate(
      Enrollment enrollment, LocalDate date, CheckInType checkInType) {
    for (int i = 0; i < enrollment.getDaysInCycle(); i++) {
      for (CheckInSchedule c : enrollment.getSchedules()) {
        if ((checkInType == null || c.getCheckInType() == checkInType)
            && isScheduled(date, enrollment, c)) {
          return date;
        }
      }
      date = date.plusDays(1);
    }
    return null;
  }

  public LocalDate getLastCheckInDate(Enrollment enrollment) {
    log.debug("SchedulingServiceImpl::getLastCheckInDate");
    return getLastCheckInDate(enrollment, null);
  }

  public LocalDate getLastCheckInDate(Enrollment enrollment, CheckInType checkInType) {
    log.debug("SchedulingServiceImpl::getLastCheckInDate");
    ZoneId zoneId = DateTimeUtils.toZoneId(enrollment.getReminderTimeZone());

    LocalDate now = LocalDate.now(zoneId);
    LocalDate date = now;
    for (int i = enrollment.getDaysInCycle(); i > 0; i--) {
      for (CheckInSchedule c : enrollment.getSchedules()) {

        if ((checkInType == null || c.getCheckInType() == checkInType)
            && isScheduled(date, enrollment, c)) {
          return date;
        }
      }
      date = date.minusDays(1);
    }
    return null;
  }

  private boolean isScheduled(LocalDate date, Enrollment enrollment, CheckInSchedule c) {

    if (c.getEndDate() != null && c.getEndDate().isBefore(date)) {
      return false;
    }

    if (c.getStartDate() != null && c.getStartDate().isAfter(date)) {
      return false;
    }

    boolean result = false;

    // convert to Calendar style where week starts from Sunday
    int dayOfWeek;

    if (date.getDayOfWeek().getValue() == 7) {
      dayOfWeek = 1;
    } else {
      dayOfWeek = date.getDayOfWeek().getValue() + 1;
    }

    if (c.getCheckInFrequency() == CheckInFrequency.DAILY) {
      result = true;
    } else if (c.getCheckInFrequency() == CheckInFrequency.WEEKDAY) {
      result = dayOfWeek >= Calendar.MONDAY && dayOfWeek <= Calendar.FRIDAY;
    } else if (c.getCheckInFrequency() == CheckInFrequency.WEEKLY) {
      result = c.getWeeklyDays().contains(dayOfWeek);
    } else if (c.getCheckInFrequency() == CheckInFrequency.CUSTOM) {
      long daysDiff =
          Duration.between(c.getStartDate().atStartOfDay(), date.atStartOfDay()).toDays() + 1;

      long dayNumber = daysDiff % enrollment.getDaysInCycle();

      result = c.getCycleDays().contains((int) dayNumber);
    }

    return result;
  }

  private void addSchedule(
      SchedulePayload schedulePayload,
      Enrollment enrollment,
      CheckInSchedule checkInSchedule,
      TriggerPayload.TriggerType type) {

    if (checkInSchedule.getEndDate() != null
        && (LocalDate.now().isAfter(checkInSchedule.getEndDate())
            || checkInSchedule.getStartDate().isAfter(checkInSchedule.getEndDate()))) {
      return;
    }

    QuartzEvent event = QuartzEvent.newCheckinEvent(type, enrollment, checkInSchedule);
    ScheduleItemPayload item = event.makeSchedule();

    schedulePayload.getItems().add(item);
  }

  public static String getTriggerId(CheckInType checkInType, TriggerPayload.TriggerType type) {
    return String.format("%s-%s", checkInType.name(), type.name());
  }

  @Override
  public void accept(TriggerEvent triggerEvent) {
    log.debug("processing triggered event {}", triggerEvent);
    QuartzEvent event;
    try {
      event = QuartzEvent.fromTriggerPayload(triggerEvent, this);
    } catch (NoSuchElementException nse) {
      // TODO: should create metric here
      log.error("could not find enrollment, unable to process {}", triggerEvent);
      return;
    }
    event.onEvent();
  }

  // Return TRUE if the date is today or some time in the future
  public static final boolean isNotInThePast(LocalDateTime d, ZoneId tz) {
    LocalDateTime today = LocalDate.now(tz).atStartOfDay();
    log.debug("isNotInThePast today is {}", today);
    log.debug("isNotInThePast date to check is {}", d);
    return !d.isBefore(today);
  }

  private static final boolean isAtStartOfTheCycle(Enrollment enrollment, LocalDateTime d) {
    LocalDate startDate = enrollment.getStartDate();
    long daysDiff1 = ChronoUnit.DAYS.between(startDate, d);
    long daysInCycle = enrollment.getDaysInCycle();
    return (daysDiff1 % daysInCycle) == 0;
  }

  private static final long daysInCycles(Enrollment enrollment) {
    long treatmentDays = 0;
    Integer cycles = enrollment.getCycles();
    if (cycles == null || cycles.intValue() == 0) {
      treatmentDays = Integer.MAX_VALUE; // no cycles defined - no end to schedule
    } else {
      Integer daysInCycle = enrollment.getDaysInCycle();
      treatmentDays =
          cycles
              * (daysInCycle == null
                  ? 1
                  : daysInCycle.intValue()); // TODO. What if "days in cycles" is null?
    }
    return treatmentDays;
  }

  // Function to check if the (current) date is beyond the schedule last date or is the last date
  private static final boolean isScheduleExpired(
      LocalDate date, Enrollment enrollment, CheckInSchedule c) {
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

  // Function to check if the (current) date is beyond all schedules last date or
  // is the last date
  public static final boolean isEnrollmentExpired(
      final LocalDate date, final Enrollment enrollment) {
    // Find if there is any active schedule
    boolean anyActive =
        enrollment.getSchedules().stream().anyMatch(c -> !isScheduleExpired(date, enrollment, c));
    return !anyActive;
  }

  public static final int getCycleNumberForDate(Enrollment enrollment, LocalDate date) {
    LocalDate startDate = enrollment.getStartDate();
    if (startDate == null) return 1;
    Long daysFromStart = DAYS.between(startDate.atStartOfDay(), date.atStartOfDay());
    return (int) (daysFromStart / enrollment.getDaysInCycle() + 1);
  }
}
