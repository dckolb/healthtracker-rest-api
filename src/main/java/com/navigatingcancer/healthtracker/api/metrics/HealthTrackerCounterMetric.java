package com.navigatingcancer.healthtracker.api.metrics;

public enum HealthTrackerCounterMetric {
  PRO_DOCUMENT_ERROR(
      "health_tracker.pro.document_error",
      "Count of errors encountered while generating or delivering PRO documents"),
  PRO_SENT_TO_EHR(
      "health_tracker.pro.sent_to_ehr", "Count of PROs submitted to external EHR system"),
  PRO_NOTE_ADDED("health_tracker.pro.note_added", "Count of notes attached to for PROs"),
  TRIAGE_TICKET_SUBMITTED(
      "triage.ticket.submitted", "count the number of open triage ticket requests sent to GC"),
  TRIAGE_TICKET_CLOSED(
      "triage.ticket.closed", "count the number of triage ticket closed notifications from GC"),
  CHECKIN_CREATED("checkin.created", "created checkins count"),
  CHECKIN_COMPLETED("checkin.completed", "completed checkins count"),
  CHECKIN_MISSED("checkin.missed", "missed checkins count"),
  ENROLLMENT_CREATED("enrollment.created", "created enrollments count"),
  ENROLLMENT_COMPLETED("enrollment.completed", "completed enrollments count"),
  ENROLLMENT_STOPPED("enrollment.stopped", "stopped enrollments count");

  private final String name;
  private final String description;

  HealthTrackerCounterMetric(String name, String description) {
    this.name = name;
    this.description = description;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }
}
