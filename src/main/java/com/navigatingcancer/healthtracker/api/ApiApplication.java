package com.navigatingcancer.healthtracker.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

import com.navigatingcancer.notification.client.service.NotificationServiceClient;
import com.navigatingcancer.patientinfo.PatientInfoClient;
import com.navigatingcancer.scheduler.client.service.SchedulerServiceClient;
import com.navigatingcancer.sqs.SqsHelper;

@SpringBootApplication
@Import({ SqsHelper.class, SchedulerServiceClient.class, NotificationServiceClient.class, PatientInfoClient.class})
public class ApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApiApplication.class, args);
	}

}
