package com.navigatingcancer.healthtracker.api.events;

import com.navigatingcancer.healthtracker.api.data.model.HealthTrackerEvent;
import com.navigatingcancer.healthtracker.api.data.service.impl.SchedulingServiceImpl;
import com.navigatingcancer.scheduler.client.domain.TriggerEvent;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CycleStart extends QuartzEvent {

  public CycleStart(TriggerEvent triggerEvent, SchedulingServiceImpl ss) {
    super(triggerEvent, ss);
  }

  @Override
  public HealthTrackerEvent prepareEventRecord() {
    HealthTrackerEvent event = new HealthTrackerEvent();
    event.setType(HealthTrackerEvent.Type.CYCLE_STARTED);
    return event;
  }

  @Override
  public void onEvent() {
    if (!Boolean.TRUE.equals(isEnrollmentActive())) {
      log.info("Ignore cycle start event since enrollment status is not active {}", enrollment);
      return;
    }
    // On cycle start always reset the auto ended flag
    if (isAutoClosed()) {
      healthTrackerStatusRepository.setEndCurrentCycle(getEnrollment().getId(), false);
    }
    // Make record of the event
    logEvent();
  }
}
