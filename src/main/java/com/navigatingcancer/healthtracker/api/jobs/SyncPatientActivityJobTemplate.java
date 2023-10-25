package com.navigatingcancer.healthtracker.api.jobs;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.navigatingcancer.healthtracker.api.data.client.gc.GcApiClient;
import com.navigatingcancer.healthtracker.api.data.client.gc.PatientActivityResponse;
import com.navigatingcancer.healthtracker.api.data.repo.proReviewActivity.ProReviewActivityRepository;
import feign.FeignException;
import java.util.UUID;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This JobTemplate is responsible for copying ProReviewActivity data to GC in order to create a
 * correlated patient activity.
 */
@Component
@Slf4j
public class SyncPatientActivityJobTemplate
    implements JobTemplate<SyncPatientActivityJobTemplate.Payload> {
  public static final String JOB_TYPE = "syncPatientActivity";

  private final GcApiClient gcApiClient;
  private final ProReviewActivityRepository proReviewActivityRepo;

  @Autowired
  SyncPatientActivityJobTemplate(
      GcApiClient gcApiClient, ProReviewActivityRepository proReviewActivityRepo) {
    this.gcApiClient = gcApiClient;
    this.proReviewActivityRepo = proReviewActivityRepo;
  }

  @Override
  public void execute(SyncPatientActivityJobTemplate.Payload payload) throws RetryableJobException {
    var activity =
        proReviewActivityRepo
            .findById(payload.proReviewActivityId)
            .orElseThrow(
                () ->
                    new RuntimeException(
                        "unable to find ProReviewActivity with id " + payload.proReviewActivityId));

    if (activity.isSynced()) {
      log.info("activity is already marked as synced, ignoring this payload");
      return;
    }

    PatientActivityResponse response;
    try {
      response = gcApiClient.savePatientActivity(activity);
    } catch (FeignException.TooManyRequests
        | FeignException.BadGateway
        | FeignException.GatewayTimeout
        | FeignException.ServiceUnavailable e) {
      throw new RetryableJobException("retryable HTTP status", e);
    }

    proReviewActivityRepo.markActivitySynced(payload.proReviewActivityId, response.getId());
    log.info("successfully synced ProReviewActivity with GC");
  }

  @Override
  public Class<SyncPatientActivityJobTemplate.Payload> getPayloadType() {
    return SyncPatientActivityJobTemplate.Payload.class;
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @JsonIgnoreProperties(ignoreUnknown = true)
  @JsonTypeName(SyncPatientActivityJobTemplate.JOB_TYPE)
  public static class Payload extends JobPayload {
    private String proReviewActivityId;

    public Payload(String proReviewActivityId) {
      super();
      this.proReviewActivityId = proReviewActivityId;
    }

    @JsonCreator
    public Payload(
        @JsonProperty("proReviewActivityId") String proReviewActivityId,
        @JsonProperty("id") UUID id,
        @JsonProperty("retryCount") int retryCount,
        @JsonProperty("message") String message) {
      super(id, retryCount, message);
      this.proReviewActivityId = proReviewActivityId;
    }

    @Override
    String getJobType() {
      return JOB_TYPE;
    }
  }
}
