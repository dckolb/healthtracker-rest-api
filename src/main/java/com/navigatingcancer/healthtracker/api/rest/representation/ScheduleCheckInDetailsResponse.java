package com.navigatingcancer.healthtracker.api.rest.representation;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ScheduleCheckInDetailsResponse {
  private String enrollmentId;
  private String checkInScheduleId;
  private LocalDateTime nextCheckInDate;
  private LocalDateTime lastCheckInDate;
  private List<CheckInResponse> checkIns;
}
