package com.navigatingcancer.healthtracker.api.data.service.impl;

import com.google.common.base.Preconditions;
import com.navigatingcancer.healthtracker.api.data.model.ContactPreferences;
import com.navigatingcancer.healthtracker.api.data.model.Enrollment;
import com.navigatingcancer.healthtracker.api.data.model.LanguageType;
import com.navigatingcancer.notification.client.domain.DeliveryType;
import com.navigatingcancer.notification.client.domain.NotificationPayload;
import com.navigatingcancer.notification.client.domain.TemplateId;
import com.navigatingcancer.notification.client.service.NotificationServiceClient;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@ComponentScan(basePackages = {"com.navigatingcancer"})
public class NotificationService {

  public enum Event {
    ENROLLED(TemplateId.ENROLLED),
    PAUSED(TemplateId.PAUSED),
    STOPPED(TemplateId.STOPPED),
    RESUMED(TemplateId.RESUMED),
    REMINDER(TemplateId.REMINDER),
    ON_DEMAND_SURVEY(TemplateId.ON_DEMAND_SURVEY);

    private final TemplateId templateId;

    Event(TemplateId templateId) {
      this.templateId = templateId;
    }
  }

  public enum Category {
    STATUS_CHANGED("status_changed"),
    FIRST_ENROLLMENT("first_enrollment"),
    CHECK_IN_REMINDER("check_in_reminder"),
    ON_DEMAND_SURVEY_REQUEST("on_demand_survey_request");

    private final String category;

    Category(String category) {
      this.category = category;
    }
  }

  @Autowired private NotificationServiceClient notificationServiceClient;

  public void sendOneTimeNotification(
      String id,
      Enrollment enrollment,
      Event event,
      Category category,
      ContactPreferences contactPreferences) {
    var payload = buildPayload(id, enrollment, event, category, contactPreferences);
    payload.setOneTimeConsentGranted(true);
    notificationServiceClient.send(payload);
  }

  // NOTE : id + currentTIme gives us a random but unique identifier. This allows the
  // notification service to dedupe double messaging when patient signs up for
  // adherence and symptoms reporting.
  public void sendNotification(String id, Enrollment enrollment, Event event, Category category) {
    log.debug("NotificationService::sendNotification {} {} {}", id, enrollment, event);
    NotificationPayload payload =
        buildPayload(id, enrollment, event, category, enrollment.getContactPreferences());
    notificationServiceClient.send(payload);
  }

  private static NotificationPayload buildPayload(
      String id,
      Enrollment enrollment,
      Event event,
      Category category,
      ContactPreferences contactPreferences) {
    Preconditions.checkArgument(
        contactPreferences != null
            && (contactPreferences.getPhoneNumber() != null
                || contactPreferences.getEmailAddress() != null),
        "enrollment provided must have contact method");

    NotificationPayload payload = new NotificationPayload();
    payload.setId(id);
    payload.setTemplateId(event.templateId);
    payload.setData(getData(enrollment));
    payload.setClinicId(enrollment.getClinicId());
    payload.setLocationId(enrollment.getLocationId());
    payload.setPatientId(enrollment.getPatientId());
    payload.setSurveyType(enrollment.getSurveyType());
    payload.setProductFeature("health_tracker");
    payload.setCategory(category.toString());

    if (enrollment.getDefaultLanguage() != null
        && enrollment.getDefaultLanguage() == LanguageType.ES) {
      payload.setDefaultLanguage(com.navigatingcancer.notification.client.domain.Language.ES);
    }

    if (StringUtils.isNotBlank(contactPreferences.getPhoneNumber())) {
      payload.setType(DeliveryType.SMS);
      payload.setTarget(contactPreferences.getPhoneNumber());
    } else if (StringUtils.isNotBlank(contactPreferences.getEmailAddress())) {
      payload.setType(DeliveryType.EMAIL);
      payload.setTarget(contactPreferences.getEmailAddress());
    }

    return payload;
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
