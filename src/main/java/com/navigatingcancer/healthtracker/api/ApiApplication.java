package com.navigatingcancer.healthtracker.api;

import com.navigatingcancer.notification.client.service.NotificationServiceClient;
import com.navigatingcancer.scheduler.client.service.SchedulerServiceClient;
import com.navigatingcancer.sqs.SqsHelper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import({SqsHelper.class, SchedulerServiceClient.class, NotificationServiceClient.class})
public class ApiApplication {

  public static void main(String[] args) {
    SpringApplication.run(ApiApplication.class, args);
  }
}
