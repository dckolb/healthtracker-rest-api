package com.navigatingcancer.healthtracker.api.data.model;

import java.security.InvalidParameterException;

public enum CheckInScheduleStatus {
  ACTIVE,
  PAUSED,
  STOPPED,
  COMPLETED;

  public static CheckInScheduleStatus fromEnrollmentStatus(EnrollmentStatus enStatus) {
    switch (enStatus) {
      case ACTIVE:
        return CheckInScheduleStatus.ACTIVE;
      case COMPLETED:
        return CheckInScheduleStatus.COMPLETED;
      case PAUSED:
        return CheckInScheduleStatus.PAUSED;
      case STOPPED:
        return CheckInScheduleStatus.STOPPED;
      default:
        throw new InvalidParameterException(
            "No corresponding checkInScheduleStatus found for enrollmentStatus "
                + enStatus.toString());
    }
  }
}
