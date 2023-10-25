package com.navigatingcancer.healthtracker.api.data.model;

public enum ProgramType {
  HEALTH_TRACKER("HealthTracker"),
  PRO_CTCAE("PRO-CTCAE");

  private final String programName;

  ProgramType(String programName) {
    this.programName = programName;
  }

  public String getProgramName() {
    return programName;
  }
}
