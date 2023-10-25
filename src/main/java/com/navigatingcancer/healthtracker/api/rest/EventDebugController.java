package com.navigatingcancer.healthtracker.api.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.navigatingcancer.healthtracker.api.data.model.schedule.TriggerPayload;
import com.navigatingcancer.healthtracker.api.data.service.impl.SchedulingServiceImpl;
import com.navigatingcancer.scheduler.client.domain.TriggerEvent;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@ConditionalOnProperty(name = "debug-api-enabled")
@RestController("/debug/events")
public class EventDebugController {
  private final SchedulingServiceImpl schedulingService;
  private final ObjectMapper jsonMapper;

  @Autowired
  public EventDebugController(SchedulingServiceImpl schedulingService, ObjectMapper jsonMapper) {
    this.schedulingService = schedulingService;
    this.jsonMapper = jsonMapper;
  }

  @Data
  public static class ScheduledEventRequest {
    TriggerPayload triggerPayload;
  }

  @PostMapping("/scheduled")
  public void submitEvent(@RequestBody ScheduledEventRequest req) throws Exception {
    var triggerEvent = new TriggerEvent();
    triggerEvent.setData(jsonMapper.writeValueAsString(req.triggerPayload));
    triggerEvent.setScheduledFireTime(new Date(Instant.now().toEpochMilli()));
    triggerEvent.setNextFireTime(new Date(Instant.now().plus(2, ChronoUnit.DAYS).toEpochMilli()));
    schedulingService.accept(triggerEvent);
  }
}
