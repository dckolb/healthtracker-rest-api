package com.navigatingcancer.healthtracker.api.jobs;

import static org.junit.Assert.*;

import com.navigatingcancer.json.utils.JsonUtils;
import java.util.UUID;
import org.junit.Test;

public class SyncPatientActivityJobTemplateTest {

  @Test
  public void testSerializePayload() {
    var payload =
        new SyncPatientActivityJobTemplate.Payload(
            "6400577f650e985d3ec72192",
            UUID.fromString("3c71ce8e-006e-48d1-8dfe-88ad83978a3b"),
            2,
            "something went wrong!");
    var asJson = JsonUtils.toJson(payload);
    assertEquals(
        "{\"syncPatientActivity\":{\"proReviewActivityId\":\"6400577f650e985d3ec72192\",\"id\":\"3c71ce8e-006e-48d1-8dfe-88ad83978a3b\",\"retryCount\":2,\"message\":\"something went wrong!\"}}",
        asJson);
  }

  @Test
  public void testDeserializePayload() {
    var payload =
        JsonUtils.fromJson(
            "{\"syncPatientActivity\":{\"id\":\"3c71ce8e-006e-48d1-8dfe-88ad83978a3b\",\"retryCount\":2,\"proReviewActivityId\":\"6400577f650e985d3ec72192\",\"message\":\"something went wrong!\"}}",
            JobPayload.class);
    assertNotNull(payload);
    assertTrue(payload instanceof SyncPatientActivityJobTemplate.Payload);
    assertEquals(UUID.fromString("3c71ce8e-006e-48d1-8dfe-88ad83978a3b"), payload.getId());
    assertEquals(2, payload.getRetryCount());
    assertEquals("something went wrong!", payload.getMessage());
    assertEquals(
        "6400577f650e985d3ec72192",
        ((SyncPatientActivityJobTemplate.Payload) payload).getProReviewActivityId());
  }
}
