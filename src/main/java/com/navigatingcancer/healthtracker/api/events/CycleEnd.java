package com.navigatingcancer.healthtracker.api.events;

import com.navigatingcancer.healthtracker.api.data.model.HealthTrackerEvent;
import com.navigatingcancer.healthtracker.api.data.service.impl.SchedulingServiceImpl;
import com.navigatingcancer.scheduler.client.domain.TriggerEvent;

public class CycleEnd extends QuartzEvent {

  public CycleEnd(TriggerEvent triggerEvent, SchedulingServiceImpl ss) {
    super(triggerEvent, ss);
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
}
