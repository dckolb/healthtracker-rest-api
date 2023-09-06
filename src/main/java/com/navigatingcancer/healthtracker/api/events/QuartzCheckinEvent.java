package com.navigatingcancer.healthtracker.api.events;

import java.time.LocalDate;
import java.time.LocalTime;

import com.navigatingcancer.healthtracker.api.data.auth.AuthInterceptor;
import com.navigatingcancer.healthtracker.api.data.model.CheckInFrequency;
import com.navigatingcancer.healthtracker.api.data.model.CheckInSchedule;
import com.navigatingcancer.healthtracker.api.data.model.Enrollment;
import com.navigatingcancer.healthtracker.api.data.model.HealthTrackerEvent;
import com.navigatingcancer.healthtracker.api.data.model.schedule.TriggerPayload;
import com.navigatingcancer.healthtracker.api.data.service.impl.SchedulingServiceImpl;
import com.navigatingcancer.scheduler.client.domain.SchedulePayloadBuilder;
import com.navigatingcancer.scheduler.client.domain.TriggerEvent;
import com.navigatingcancer.scheduler.client.domain.SchedulePayload.ScheduleItemPayload;

public abstract class QuartzCheckinEvent extends QuartzEvent {

    protected CheckInSchedule checkInSchedule;

    protected QuartzCheckinEvent(TriggerEvent triggerEvent, SchedulingServiceImpl ss) {
        super(triggerEvent, ss);
    }

    protected QuartzCheckinEvent(Enrollment en, CheckInSchedule cis) {
        super(en);
        this.checkInSchedule = cis;
    }

    // Overrriding basic implementation, use upsert instead of save call
    @Override
    protected void logEvent() {
        HealthTrackerEvent event = prepareEventRecord();
        if (event != null) {
            Enrollment enr = getEnrollment();
            event.setEnrollmentId(enr.getId());
            event.setClinicId(enr.getClinicId());
            event.setPatientId(enr.getPatientId());
            event.setDate(getEventInstant());
            event.setBy(AuthInterceptor.HEALTH_TRACKER_NAME);
            eventsRepository.upsertCheckinEvent(event);
        }
    }

    // Trigger type
    public abstract TriggerPayload.TriggerType getTriggerType();

    // Function that starts building schedule for the specific event type
    public abstract SchedulePayloadBuilder makeSchedulePayloadBuilder(CheckInSchedule cis);

    @Override
    public ScheduleItemPayload makeSchedule() {
        // Make scheduler builder for the specific event type, that's basically time of day choice
        TriggerPayload.TriggerType type = getTriggerType();
        LocalTime reminderTime = getReminderTime();
        triggerPayload = new TriggerPayload(enrollment.getId(), checkInSchedule.getCheckInType(), reminderTime, type);
        SchedulePayloadBuilder builder = makeSchedulePayloadBuilder(checkInSchedule);
        // Day part of the schedule
        LocalDate startDate = checkInSchedule.getStartDate();
        if (checkInSchedule.getCheckInFrequency() == CheckInFrequency.DAILY) {
            builder.daily(startDate, checkInSchedule.getEndDate());
        } else if (checkInSchedule.getCheckInFrequency() == CheckInFrequency.WEEKDAY) {
            builder.weekdays(startDate, checkInSchedule.getEndDate());
        } else if (checkInSchedule.getCheckInFrequency() == CheckInFrequency.WEEKLY) {
            builder.weekly(startDate, checkInSchedule.getEndDate(), checkInSchedule.getWeeklyDays());
        } else if (checkInSchedule.getCheckInFrequency() == CheckInFrequency.CUSTOM) {
            builder.custom(startDate, checkInSchedule.getEndDate(), checkInSchedule.getCycleDays(),
                    enrollment.getDaysInCycle());
        }
        return builder.build();
    }
    
}
