package com.navigatingcancer.healthtracker.api.data.repo;

import java.util.List;

import com.navigatingcancer.healthtracker.api.data.model.HealthTrackerEvent;

public interface HealthTrackerEventsRepository {
    public HealthTrackerEvent save(HealthTrackerEvent e);
	public HealthTrackerEvent upsertCheckinEvent(HealthTrackerEvent e);
    public List<HealthTrackerEvent> getPatientEvents(Long clinicId, Long patientId);
    public List<HealthTrackerEvent> getEnrollmentEvents(String enrollmentId);
}