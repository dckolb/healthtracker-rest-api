package com.navigatingcancer.healthtracker.api.events;

import com.navigatingcancer.healthtracker.api.data.model.AbstractDocument;
import com.navigatingcancer.healthtracker.api.data.model.CheckIn;
import com.navigatingcancer.healthtracker.api.data.model.HealthTrackerEvent;
import com.navigatingcancer.healthtracker.api.data.service.impl.NotificationService;
import com.navigatingcancer.healthtracker.api.data.service.impl.SchedulingServiceImpl;
import com.navigatingcancer.scheduler.client.domain.TriggerEvent;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class QuartzReminderEvent extends QuartzCheckinEvent {

  protected NotificationService notificationService;

  public QuartzReminderEvent(TriggerEvent triggerEvent, SchedulingServiceImpl ss) {
    super(triggerEvent, ss);
    this.notificationService = ss.getNotificationService();
  }

  @Override
  public HealthTrackerEvent prepareEventRecord() {
    HealthTrackerEvent event = new HealthTrackerEvent();
    event.setType(HealthTrackerEvent.Type.REMINDER_SENT);
    event.setEvent("Check-in reminder sent");
    List<CheckIn> cins =
        checkInRepository.findPastDue(enrollment.getId(), scheduledDateTime.toLocalDate());
    List<String> checkinIds =
        cins.stream().map(AbstractDocument::getId).collect(Collectors.toList());
    event.setRelatedCheckinId(checkinIds);
    return event;
  }

  @Override
  public void onEvent() {
    if (!Boolean.TRUE.equals(isEnrollmentActive())) {
      log.info("Ignore reminder event since enrollment status is not active {}", enrollment);
      return;
    }
    if (Boolean.TRUE.equals(getEnrollment().isManualCollect())) {
      log.info("Ignore reminder for clinic collect enrollment {}", enrollment);
      return;
    }
    if (!isAutoClosed()) {
      log.info("Sending check in reminder for enrollment {}", enrollment);
      notificationService.sendNotification(
          triggerPayload.getEnrollmentId() + triggerEvent.getScheduledFireTime().getTime(),
          enrollment,
          NotificationService.Event.REMINDER,
          NotificationService.Category.CHECK_IN_REMINDER);

      logEvent();
    } else {
      log.info("Ignore reminder for auto-closed cycle of enrollment {}", enrollment);
    }
  }
}
