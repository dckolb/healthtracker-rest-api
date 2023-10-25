package com.navigatingcancer.healthtracker.api.jobs;

public interface JobTemplate<T extends JobPayload> {
  Class<T> getPayloadType();

  void execute(T payload) throws RetryableJobException;
}
