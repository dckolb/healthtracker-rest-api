package com.navigatingcancer.healthtracker.api.events;

import com.navigatingcancer.healthtracker.api.data.model.HealthTrackerEvent;
import com.navigatingcancer.healthtracker.api.data.service.impl.SchedulingServiceImpl;
import com.navigatingcancer.scheduler.client.domain.TriggerEvent;

public class EnrollmentStart extends QuartzEvent {

  public EnrollmentStart(TriggerEvent triggerEvent, SchedulingServiceImpl ss) {
    super(triggerEvent, ss);
  }

  @Override
  public HealthTrackerEvent prepareEventRecord() {
    HealthTrackerEvent event = new HealthTrackerEvent();
    event.setType(HealthTrackerEvent.Type.ENROLLMENT_ACTIVE);
    return event;
  }
}
