package com.navigatingcancer.healthtracker.api.events;

import java.time.LocalDate;
import java.time.LocalTime;

import com.navigatingcancer.healthtracker.api.data.model.CheckInType;
import com.navigatingcancer.healthtracker.api.data.model.Enrollment;
import com.navigatingcancer.healthtracker.api.data.model.HealthTrackerEvent;
import com.navigatingcancer.healthtracker.api.data.model.schedule.TriggerPayload;
import com.navigatingcancer.healthtracker.api.data.model.schedule.TriggerPayload.TriggerType;
import com.navigatingcancer.healthtracker.api.data.service.impl.SchedulingServiceImpl;
import com.navigatingcancer.scheduler.client.domain.SchedulePayload.ScheduleItemPayload;
import com.navigatingcancer.scheduler.client.domain.SchedulePayloadBuilder;
import com.navigatingcancer.scheduler.client.domain.TriggerEvent;

public class EnrollmentStart extends QuartzEvent {

	public EnrollmentStart(TriggerEvent triggerEvent, SchedulingServiceImpl ss) {
		super(triggerEvent, ss);
	}

	public EnrollmentStart(Enrollment en) {
        super(en);
    }

	@Override
	public HealthTrackerEvent prepareEventRecord() {
		HealthTrackerEvent event = new HealthTrackerEvent();
		event.setType(HealthTrackerEvent.Type.ENROLLMENT_ACTIVE);
		return event;
	}

	@Override
	public ScheduleItemPayload makeSchedule() {
		TriggerPayload.TriggerType messageType = TriggerType.ENROLLMENT_START;
		CheckInType checkInType = CheckInType.COMBO;

		triggerPayload = new TriggerPayload(enrollment.getId(), checkInType, LocalTime.of(0, 0, 0), messageType);
		String triggerId = SchedulingServiceImpl.getTriggerId(checkInType, messageType);
		SchedulePayloadBuilder builder = SchedulePayloadBuilder.create(triggerId, triggerPayload);

		builder.at(LocalTime.MIN, enrollment.getReminderTimeZone());
		LocalDate starttDate = enrollment.getStartDate();
		builder.daily(starttDate, starttDate);
		return builder.build();
	}

}
