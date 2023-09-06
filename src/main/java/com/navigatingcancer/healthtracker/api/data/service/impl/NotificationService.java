package com.navigatingcancer.healthtracker.api.data.service.impl;

import java.util.HashMap;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Service;

import com.navigatingcancer.healthtracker.api.data.model.Enrollment;
import com.navigatingcancer.healthtracker.api.data.model.LanguageType;
import com.navigatingcancer.notification.client.domain.DeliveryType;
import com.navigatingcancer.notification.client.domain.NotificationPayload;
import com.navigatingcancer.notification.client.domain.TemplateId;
import com.navigatingcancer.notification.client.service.NotificationServiceClient;

@Service
@Slf4j
@ComponentScan(basePackages = {"com.navigatingcancer"})
public class NotificationService {

	public enum Event {

		ENROLLED("ht_enrolled"), PAUSED("ht_paused"), STOPPED("ht_stopped"), RESUMED("ht_resumed"), REMINDER("ht_reminder");

		String templateId;

		private Event(String templateId) {
			this.templateId = templateId;
		}
	}

	public enum Category {

		STATUS_CHANGED("status_changed"), 
		FIRST_ENROLLMENT("first_enrollment"), 
		CHECK_IN_REMINDER("check_in_reminder");
	
		String category;
		private Category(String category) {
			this.category = category;
		}
	}

	@Autowired
	private NotificationServiceClient notificationServiceClient;

	public void sendNotification(String id, Enrollment enrollment, Event event) {
		String category = "unknown";
		sendNotification(id, enrollment, event, category);
	}

	// NOTE : id + currentTIme gives us a random but unique identifier. This allows the
	// notification service to dedupe double messaging when patient signs up for
	// adherence and symptoms reporting.
	public void sendNotification(String id, Enrollment enrollment, Event event, String category) {
		log.debug("NotificationService::sendNotification {} {} {}", id, enrollment, event);
		NotificationPayload payload = new NotificationPayload();
		payload.setId(id);
		payload.setTemplateId(TemplateId.fromValue(event.templateId));
		payload.setData(getData(enrollment));
		payload.setClinicId(enrollment.getClinicId());
		payload.setLocationId(enrollment.getLocationId());
		payload.setPatientId(enrollment.getPatientId());
		payload.setSurveyType(enrollment.getSurveyType());
		payload.setProductFeature("health_tracker");
		payload.setCategory(category);

		if (enrollment.getDefaultLanguage() != null && enrollment.getDefaultLanguage() == LanguageType.ES) {
			payload.setDefaultLanguage(com.navigatingcancer.notification.client.domain.Language.ES);
		}

		if (enrollment.getPhoneNumber() != null) {
			payload.setType(DeliveryType.SMS);
			payload.setTarget(enrollment.getPhoneNumber());
		} else if (enrollment.getEmailAddress() != null) {
			payload.setType(DeliveryType.EMAIL);
			payload.setTarget(enrollment.getEmailAddress());
		}

		if (!enrollment.isManualCollect()) {
			notificationServiceClient.send(payload);
		} else {
			log.info("Not sending notification for clinic collect enrollment {} {}", id, enrollment);
		}
	}

	static Map<String, Object> getData(Enrollment enrollment) {
		Map<String, Object> data = new HashMap<>();
		data.put("patient_id", enrollment.getPatientId());
		data.put("clinic_id", enrollment.getClinicId());
		data.put("location_id", enrollment.getLocationId());
		data.put("url", enrollment.getUrl());
		return data;
	}

}
