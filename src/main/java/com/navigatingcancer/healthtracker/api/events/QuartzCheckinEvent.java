package com.navigatingcancer.healthtracker.api.events;

import com.navigatingcancer.healthtracker.api.data.auth.AuthInterceptor;
import com.navigatingcancer.healthtracker.api.data.model.CheckInSchedule;
import com.navigatingcancer.healthtracker.api.data.model.Enrollment;
import com.navigatingcancer.healthtracker.api.data.model.HealthTrackerEvent;
import com.navigatingcancer.healthtracker.api.data.service.impl.SchedulingServiceImpl;
import com.navigatingcancer.scheduler.client.domain.TriggerEvent;

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
}
