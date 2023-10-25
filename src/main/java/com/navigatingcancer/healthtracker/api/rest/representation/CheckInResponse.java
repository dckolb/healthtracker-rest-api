package com.navigatingcancer.healthtracker.api.rest.representation;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.navigatingcancer.healthtracker.api.data.model.CheckIn;
import com.navigatingcancer.healthtracker.api.data.model.CheckInSchedule;
import com.navigatingcancer.healthtracker.api.data.model.CheckInStatus;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CheckInResponse {
  private String id;
  private Long patientId;
  private Long clinicId;
  private Long locationId;
  private String completedBy;
  private String surveyInstanceId;
  private Map<String, ?> checkInParameters;
  private LocalDate scheduleDate;
  private LocalTime scheduleTime;
  private CheckInStatus status;
  // private SurveyItemPayload surveyPayload;
  // private LocalDate patientReportedTxStartDate;
  private Boolean clinicCollect;
  // private LocalDate enrollmentPatientReportedStartDate;
  // private LocalDate enrollmentReminderStartDate;
  private Boolean declineACall;
  private String declineACallComment;
  private CheckInSchedule checkInSchedule;

  public CheckInResponse(CheckIn checkIn, CheckInSchedule schedule) {
    this.id = checkIn.getId();
    this.patientId = checkIn.getPatientId();
    this.clinicId = checkIn.getClinicId();
    this.locationId = checkIn.getLocationId();
    this.completedBy = checkIn.getCompletedBy();
    this.checkInParameters = checkIn.getCheckInParameters();
    this.scheduleDate = checkIn.getScheduleDate();
    this.scheduleTime = checkIn.getScheduleTime();
    this.status = checkIn.getStatus();
    this.declineACall = checkIn.getDeclineACall();
    this.declineACallComment = checkIn.getDeclineACallComment();
    this.surveyInstanceId = checkIn.getSurveyInstanceId();
    this.checkInSchedule = schedule;
  }

  public CheckInResponse(CheckIn checkIn) {
    this(checkIn, null);
  }
}
