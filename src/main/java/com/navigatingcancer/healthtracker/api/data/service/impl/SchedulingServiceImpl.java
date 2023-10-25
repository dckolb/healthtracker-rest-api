package com.navigatingcancer.healthtracker.api.data.service.impl;

import com.navigatingcancer.date.utils.DateTimeUtils;
import com.navigatingcancer.healthtracker.api.data.model.*;
import com.navigatingcancer.healthtracker.api.data.model.schedule.Scheduling;
import com.navigatingcancer.healthtracker.api.data.model.schedule.TriggerPayload;
import com.navigatingcancer.healthtracker.api.data.model.schedule.TriggerPayload.TriggerType;
import com.navigatingcancer.healthtracker.api.data.repo.*;
import com.navigatingcancer.healthtracker.api.data.service.CheckInCreationService;
import com.navigatingcancer.healthtracker.api.data.service.EnrollmentService;
import com.navigatingcancer.healthtracker.api.events.QuartzEvent;
import com.navigatingcancer.healthtracker.api.metrics.MetersService;
import com.navigatingcancer.healthtracker.api.processor.HealthTrackerStatusService;
import com.navigatingcancer.healthtracker.api.rest.exception.UnknownEnrollmentException;
import com.navigatingcancer.scheduler.client.domain.SchedulePayload;
import com.navigatingcancer.scheduler.client.domain.SchedulePayloadBuilder;
import com.navigatingcancer.scheduler.client.domain.TriggerEvent;
import com.navigatingcancer.scheduler.client.service.SchedulerServiceClient;
import com.navigatingcancer.sqs.SqsListener;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Consumer;
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

  @Value("${feign.scheduler.url:http://scheduler-service/scheduler-service}")
  private String apiUrl;

  static final String QUEUE_VAR_NAME = "${ht-reminder-queue}";

  @Autowired private EnrollmentRepository enrollmentRepository;

  @Autowired private EnrollmentService enrollmentService;

  @Autowired private CheckInRepository checkInRepository;

  @Autowired private HealthTrackerStatusRepository healthTrackerStatusRepository;

  @Autowired private SchedulerServiceClient scheduleServiceClient;

  @Autowired private NotificationService notificationService;

  @Autowired private CheckInCreationService checkInCreationService;

  // TODO: remove lazy annoation and refactor to avoid circular dependancy
  @Autowired @Lazy private HealthTrackerStatusService healthTrackerStatusService;

  @Autowired private HealthTrackerEventsRepository healthTrackerEventsRepository;

  @Autowired private MetersService metersService;

  // TODO use ioc to handle this -- the event handlers can get their own dependencies
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

  public CheckInCreationService getCheckInCreationService() {
    return checkInCreationService;
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

  public void schedule(Enrollment enrollment, boolean firstTime) {
    log.debug("SchedulingServiceImpl::schedule");
    if (!firstTime) {
      unschedule(enrollment);
    }

    if (enrollment.getStatus() != EnrollmentStatus.ACTIVE) {
      log.info("Skipping schedule for {} due status non-active", enrollment);
      return;
    }

    // ensure each schedule has a start and possibly end date
    enrollment.validateDates();

    SchedulePayload schedulePayload =
        Scheduling.createSchedulePayload(enrollment, enrollment.getSchedules());
    schedulePayload.setQueueName(queueName);

    log.debug("scheduling service call payload is {}", schedulePayload);
    if (schedulePayload.getItems() != null && schedulePayload.getItems().size() > 0)
      scheduleServiceClient.getApi(apiUrl).schedule(enrollment.getId(), schedulePayload);

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
        new TriggerPayload(enrollment.getId(), reminderTime, TriggerType.REMINDER);
    SchedulePayloadBuilder builder = SchedulePayloadBuilder.create(deDupeJobId, triggerPayload);

    LocalDate dateNow = LocalDate.now(tzId);
    builder.at(reminderTime, tzName).daily(dateNow, dateNow);
    schedulePayload.getItems().add(builder.build());

    scheduleServiceClient.getApi(apiUrl).schedule(deDupeJobId, schedulePayload);
  }

  private void unschedule(Enrollment enrollment) {
    scheduleServiceClient.getApi(apiUrl).stopJob(QUEUE_VAR_NAME, enrollment.getId());
  }
}
