package com.navigatingcancer.healthtracker.api.data.model.scheduleConfig;

import java.time.LocalDate;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class CycleScheduleConfig extends ScheduleConfig {
  private Integer cycles;
  private Integer currentCycleNumber;
  private Integer daysInCycle;
  private List<Integer> cycleDays;
  private LocalDate currentCycleStartDate;
  private LocalDate nextCycleStartDate;
}
