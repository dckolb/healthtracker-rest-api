package com.navigatingcancer.healthtracker.api.events;

import java.time.LocalDate;
import java.time.LocalTime;

import com.navigatingcancer.healthtracker.api.data.model.CheckInType;
import com.navigatingcancer.healthtracker.api.data.model.Enrollment;
import com.navigatingcancer.healthtracker.api.data.model.HealthTrackerEvent;
import com.navigatingcancer.healthtracker.api.data.model.schedule.TriggerPayload;
import com.navigatingcancer.healthtracker.api.data.model.schedule.TriggerPayload.TriggerType;
import com.navigatingcancer.healthtracker.api.data.service.EnrollmentService;
import com.navigatingcancer.healthtracker.api.data.service.impl.SchedulingServiceImpl;
import com.navigatingcancer.scheduler.client.domain.SchedulePayloadBuilder;
import com.navigatingcancer.scheduler.client.domain.TriggerEvent;
import com.navigatingcancer.scheduler.client.domain.SchedulePayload.ScheduleItemPayload;

public class EnrollmentEnd extends QuartzEvent {
    private EnrollmentService enrollmentService;

    protected EnrollmentEnd(TriggerEvent triggerEvent, SchedulingServiceImpl ss) {
        super(triggerEvent, ss);
        this.enrollmentService = ss.getEnrollmentService();
    }

    public EnrollmentEnd(Enrollment en) {
        super(en);
    }

	@Override
	public HealthTrackerEvent prepareEventRecord() {
		HealthTrackerEvent event = new HealthTrackerEvent();
		event.setType(HealthTrackerEvent.Type.ENROLLMENT_COMPLETED);
		return event;
	}

	@Override
    public void onEvent() {
        if (Boolean.TRUE.equals(isEnrollmentActive())) {
            enrollment = enrollmentService.completeEnrollment(enrollment);
        }
        logEvent();
    }

    @Override
    public ScheduleItemPayload makeSchedule() {
        TriggerPayload.TriggerType messageType = TriggerType.ENROLLMENT_END;
        CheckInType checkInType = CheckInType.COMBO; // That type should not matter on the last day check. TODO: confirm.

        triggerPayload = new TriggerPayload(enrollment.getId(), checkInType, LocalTime.of(0, 0, 0), messageType);
        String triggerId = SchedulingServiceImpl.getTriggerId(checkInType, messageType);
        SchedulePayloadBuilder builder = SchedulePayloadBuilder.create(triggerId, triggerPayload);

        // Last status calculation at the start of the new date after the end of the
        // schedule
        builder.at(LocalTime.MIN, enrollment.getReminderTimeZone());
        LocalDate endDate = enrollment.getSchedulesLastDate();
        LocalDate checkDate = endDate.plusDays(1);
        builder.daily(checkDate, checkDate);
        return builder.build();
    }
}
