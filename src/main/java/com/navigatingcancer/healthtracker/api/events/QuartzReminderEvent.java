package com.navigatingcancer.healthtracker.api.events;

import java.util.List;
import java.util.stream.Collectors;

import com.navigatingcancer.healthtracker.api.data.model.CheckIn;
import com.navigatingcancer.healthtracker.api.data.model.CheckInSchedule;
import com.navigatingcancer.healthtracker.api.data.model.Enrollment;
import com.navigatingcancer.healthtracker.api.data.model.HealthTrackerEvent;
import com.navigatingcancer.healthtracker.api.data.model.schedule.TriggerPayload.TriggerType;
import com.navigatingcancer.healthtracker.api.data.service.impl.NotificationService;
import com.navigatingcancer.healthtracker.api.data.service.impl.SchedulingServiceImpl;
import com.navigatingcancer.scheduler.client.domain.SchedulePayloadBuilder;
import com.navigatingcancer.scheduler.client.domain.TriggerEvent;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class QuartzReminderEvent extends QuartzCheckinEvent {

    protected NotificationService notificationService;

	public QuartzReminderEvent(TriggerEvent triggerEvent, SchedulingServiceImpl ss) {
        super(triggerEvent, ss);
        this.notificationService = ss.getNotificationService();
	}

    protected QuartzReminderEvent(Enrollment en, CheckInSchedule cis) {
        super(en, cis);
    }

    @Override
	public HealthTrackerEvent prepareEventRecord() {
        HealthTrackerEvent event = new HealthTrackerEvent();
        event.setType(HealthTrackerEvent.Type.REMINDER_SENT);
        event.setEvent("Check-in reminder sent");
        List<CheckIn> cins = checkInRepository.findPastDue(enrollment.getId(), scheduledDateTime.toLocalDate());
        List<String> checkinIds = cins.stream().map(c -> c.getId()).collect(Collectors.toList());
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
                NotificationService.Category.CHECK_IN_REMINDER.toString()
            );

            logEvent();
        } else {
            log.info("Ignore reminder for auto-closed cycle of enrollment {}", enrollment);
        }
    }

    @Override
    public TriggerType getTriggerType() {
        return TriggerType.REMINDER;
    }

    @Override
    public SchedulePayloadBuilder makeSchedulePayloadBuilder(CheckInSchedule checkInSchedule) {
        String triggerId = SchedulingServiceImpl.getTriggerId(checkInSchedule.getCheckInType(), TriggerType.REMINDER);
        // Note. triggerPayload should have been created before this call
        SchedulePayloadBuilder builder = SchedulePayloadBuilder.create(triggerId, triggerPayload); 
        builder.at(getReminderTime(), enrollment.getReminderTimeZone());
        // don't need notifications for past due reminders
        builder.ignoreMisfires();
        return builder;
    }

}
