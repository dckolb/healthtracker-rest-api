package com.navigatingcancer.healthtracker.api.data.model.schedule;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.navigatingcancer.healthtracker.api.data.model.CheckInType;
import java.time.LocalTime;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TriggerPayload {

  @Deprecated
  public TriggerPayload(
      String enrollmentId, CheckInType checkIntype, LocalTime checkInTime, TriggerType type) {
    this.enrollmentId = enrollmentId;
    this.checkInType = checkIntype;
    this.checkInTime = checkInTime;
    this.type = type;
  }

  public TriggerPayload(String enrollmentId, LocalTime checkInTime, TriggerType type) {
    this.enrollmentId = enrollmentId;
    this.checkInTime = checkInTime;
    this.type = type;
  }

  public TriggerPayload(
      String enrollmentId, String checkInScheduleId, LocalTime checkInTime, TriggerType type) {
    this.enrollmentId = enrollmentId;
    this.checkInScheduleId = checkInScheduleId;
    this.checkInTime = checkInTime;
    this.type = type;
  }

  public TriggerPayload() {}

  public static enum TriggerType {
    SYSTEM,
    REMINDER,
    STATUS,
    ENROLLMENT_START,
    ENROLLMENT_END,
    CYCLE_START,
    CYCLE_END
  }

  String enrollmentId;
  String checkInScheduleId;
  LocalTime checkInTime;
  TriggerType type;

  /**
   * @deprecated use checkInScheduleId instead, this remains for backward compatibility with
   *     pre-scheduled payloads
   */
  @Deprecated CheckInType checkInType;
}
