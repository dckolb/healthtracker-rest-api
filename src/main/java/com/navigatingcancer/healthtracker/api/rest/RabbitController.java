package com.navigatingcancer.healthtracker.api.rest;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RabbitController {
	
	@Autowired
	RabbitTemplate rabbitTemplate;
	
	@PostMapping("/rabbit/push")
	public void publish(@RequestBody String body) {
		rabbitTemplate.convertAndSend("app/health_tracker/patient/status_record", body);
	}

}
