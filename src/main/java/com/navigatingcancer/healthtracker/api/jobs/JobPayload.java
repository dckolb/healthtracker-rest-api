package com.navigatingcancer.healthtracker.api.jobs;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
@JsonSubTypes({@JsonSubTypes.Type(value = SyncPatientActivityJobTemplate.Payload.class)})
@Data
@AllArgsConstructor
@NoArgsConstructor
public abstract class JobPayload {
  /** A unique id for the job, useful for deduplication, logging and tracing */
  private UUID id = UUID.randomUUID();

  private int retryCount = 0;

  private String message;

  void recordAttempt(String message) {
    this.message = message;
    retryCount++;
  }

  abstract String getJobType();
}
