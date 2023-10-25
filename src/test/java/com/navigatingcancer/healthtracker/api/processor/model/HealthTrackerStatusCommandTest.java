package com.navigatingcancer.healthtracker.api.processor.model;

import com.navigatingcancer.json.utils.JsonUtils;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.springframework.util.Assert;

@RunWith(JUnit4.class)
public class HealthTrackerStatusCommandTest {
  @Test
  public void canDeserializeFromJson() {
    var command =
        new HealthTrackerStatusCommand("enrollmentId", null, List.of("checkin1", "checkin2"));

    Assert.notNull(
        JsonUtils.fromJson(JsonUtils.toJson(command), HealthTrackerStatusCommand.class),
        "should be deserializable");
  }
}
