package com.navigatingcancer.healthtracker.api.jobs;

import com.navigatingcancer.sqs.SqsListener;
import datadog.trace.api.Trace;
import io.opentracing.util.GlobalTracer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@SqsListener(queueName = "${ht-jobs-queue}")
@Component
public class JobListener implements Consumer<JobPayload> {
  private final int maxRetries;
  private final JobPublisher publisher;
  private final Map<Class<JobPayload>, JobTemplate> jobTemplatesByType = new HashMap<>();

  @Autowired
  public JobListener(
      @Value("${jobs.maxRetries:5}") int maxRetries,
      List<JobTemplate> jobTemplates,
      JobPublisher publisher) {

    this.maxRetries = maxRetries;
    this.publisher = publisher;
    for (JobTemplate template : jobTemplates) {
      jobTemplatesByType.put(template.getPayloadType(), template);
    }
    log.info("jobTemplatesByType {}", jobTemplatesByType);
  }

  @Trace(operationName = "job.dispatch")
  @Override
  public void accept(JobPayload jobPayload) {
    GlobalTracer.get()
        .activeSpan()
        .setTag("jobId", jobPayload.getId().toString())
        .setTag("jobType", jobPayload.getJobType());

    if (!jobTemplatesByType.containsKey(jobPayload.getClass())) {
      throw new UnsupportedOperationException(
          String.format(
              "unknown job payload. class=%s, type=%s, id=%s",
              jobPayload.getClass(), jobPayload.getJobType(), jobPayload.getId()));
    }

    try {
      log.info("Executing job {})", jobPayload);

      var template = jobTemplatesByType.get(jobPayload.getClass());
      template.execute(jobPayload);
      log.info(
          "Successfully executed job. type={}, id={}", jobPayload.getJobType(), jobPayload.getId());
    } catch (RetryableJobException e) {
      if (jobPayload.getRetryCount() >= maxRetries) {
        log.error(
            "Job template threw exception during execution. The job retries are exhausted!", e);
        // throw the error and let the queue's re-drive policy handle this job
        throw new RuntimeException("exhausted retries", e);
      }

      log.error("Job template threw exception during execution. The job will be retried.", e);
      jobPayload.recordAttempt(e.getMessage());
      publisher.publish(jobPayload);
    } catch (Exception e) {
      log.error("Job template threw un-retryable exception. It will not be retried");
      throw e;
    }
  }
}
