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

public class CycleEnd extends QuartzEvent {

	public CycleEnd(TriggerEvent triggerEvent, SchedulingServiceImpl ss) {
		super(triggerEvent, ss);
	}

	public CycleEnd(Enrollment en) {
		super(en);
	}

	@Override
	public HealthTrackerEvent prepareEventRecord() {
		HealthTrackerEvent event = null;
		if (!isAutoClosed()) {
			event = new HealthTrackerEvent();
			event.setType(HealthTrackerEvent.Type.CYCLE_ENDED);
		}
		return event;
	}

	@Override
	public ScheduleItemPayload makeSchedule() {
		TriggerPayload.TriggerType messageType = TriggerType.CYCLE_END;
		CheckInType checkInType = CheckInType.COMBO;

		// use the start of the first day of the new cycle
		triggerPayload = new TriggerPayload(enrollment.getId(), checkInType, LocalTime.of(0, 0, 0), messageType);
		String triggerId = SchedulingServiceImpl.getTriggerId(checkInType, messageType);
		SchedulePayloadBuilder builder = SchedulePayloadBuilder.create(triggerId, triggerPayload);

		builder.at(LocalTime.MIN, enrollment.getReminderTimeZone());
		LocalDate startDate = enrollment.getStartDate().plusDays(enrollment.getDaysInCycle());
		LocalDate endDate = enrollment.getSchedulesLastDate();
		if (endDate != null && endDate.isBefore(startDate)) {
			endDate = startDate; // if there is only one cycle, the end date is last date + 1 day
		}
		builder.cycle(startDate, endDate, enrollment.getDaysInCycle());
		return builder.build();
	}
}
