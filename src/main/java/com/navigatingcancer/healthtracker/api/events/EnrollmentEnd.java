package com.navigatingcancer.healthtracker.api.events;

import com.navigatingcancer.healthtracker.api.data.model.HealthTrackerEvent;
import com.navigatingcancer.healthtracker.api.data.service.EnrollmentService;
import com.navigatingcancer.healthtracker.api.data.service.impl.SchedulingServiceImpl;
import com.navigatingcancer.scheduler.client.domain.TriggerEvent;

public class EnrollmentEnd extends QuartzEvent {
  private EnrollmentService enrollmentService;

  protected EnrollmentEnd(TriggerEvent triggerEvent, SchedulingServiceImpl ss) {
    super(triggerEvent, ss);
    this.enrollmentService = ss.getEnrollmentService();
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
}
