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

import lombok.extern.slf4j.Slf4j;

import com.navigatingcancer.scheduler.client.domain.SchedulePayloadBuilder;
import com.navigatingcancer.scheduler.client.domain.TriggerEvent;

@Slf4j
public class CycleStart extends QuartzEvent	 {

	public CycleStart(TriggerEvent triggerEvent, SchedulingServiceImpl ss) {
		super(triggerEvent, ss);
	}

    public CycleStart(Enrollment en) {
        super(en);
    }

	@Override
	public HealthTrackerEvent prepareEventRecord() {
		HealthTrackerEvent event = new HealthTrackerEvent();
		event.setType(HealthTrackerEvent.Type.CYCLE_STARTED);
		return event;
	}

	@Override
	public ScheduleItemPayload makeSchedule() {
		TriggerPayload.TriggerType messageType = TriggerType.CYCLE_START;
		CheckInType checkInType = CheckInType.COMBO;

		// Start of the first day of the new cycle
		triggerPayload = new TriggerPayload(enrollment.getId(), checkInType, LocalTime.of(0, 0, 0), messageType);
		String triggerId = SchedulingServiceImpl.getTriggerId(checkInType, messageType);
		SchedulePayloadBuilder builder = SchedulePayloadBuilder.create(triggerId, triggerPayload);

		builder.at(LocalTime.MIN, enrollment.getReminderTimeZone());
		LocalDate startDate = enrollment.getStartDate();
		LocalDate endDate = enrollment.getSchedulesLastDate();
		// TODO Only one day cycle can start on the last day of the enrollment. Do we support one day cycles?
		builder.cycle(startDate, endDate, enrollment.getDaysInCycle());
		return builder.build();
	}

	public void onEvent() {
		if (!Boolean.TRUE.equals(isEnrollmentActive())) {
			log.info("Ignore cycle start event since enrollment status is not active {}", enrollment);
			return;
		}
		// On cycle start always reset the auto ended flag
		if( isAutoClosed() ) {
			healthTrackerStatusRepository.setEndCurrentCycle(getEnrollment().getId(), false);
		}
		// Make record of the event
		logEvent();
	}

}
