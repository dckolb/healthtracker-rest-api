package com.navigatingcancer.healthtracker.api.jobs;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.navigatingcancer.json.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class JobPublisher {
  private final AmazonSQS sqs;
  private final String queueUrl;

  @Autowired
  public JobPublisher(@Value("${ht-jobs-queue}") String queueName) {
    this.sqs = AmazonSQSClientBuilder.defaultClient();
    this.queueUrl = sqs.getQueueUrl(queueName).getQueueUrl();
  }

  public void publish(JobPayload jobPayload) {
    log.info("Publishing job payload {}", jobPayload);

    sqs.sendMessage(
        new SendMessageRequest()
            .withQueueUrl(queueUrl)
            .withMessageBody(JsonUtils.toJson(jobPayload))
            // ensure messages for same job are handled in order
            .withMessageGroupId(
                String.format("%s/%s", jobPayload.getJobType(), jobPayload.getId().toString()))
            // ensure message for same job and retry attempt are de-duplicated
            .withMessageDeduplicationId(
                String.format("%s-%d", jobPayload.getId().toString(), jobPayload.getRetryCount())));
  }
}
