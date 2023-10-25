package com.navigatingcancer.healthtracker.api.data.model.scheduleConfig;

import java.time.LocalDate;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class CalendarScheduleConfig extends ScheduleConfig {
  private List<LocalDate> calendarDays;
}
