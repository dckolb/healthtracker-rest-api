package com.navigatingcancer.healthtracker.api.data.model.scheduleConfig;

import com.navigatingcancer.healthtracker.api.data.model.ScheduleType;
import lombok.Data;

@Data
public abstract class ScheduleConfig {
  private ScheduleType type;
}
