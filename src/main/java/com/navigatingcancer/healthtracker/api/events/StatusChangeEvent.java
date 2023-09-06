package com.navigatingcancer.healthtracker.api.events;

import java.time.Instant;
import java.util.List;

import com.navigatingcancer.healthtracker.api.data.model.Enrollment;
import com.navigatingcancer.healthtracker.api.data.model.survey.SurveyPayload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class StatusChangeEvent {
    public enum Type {
        BY_CLINICIAN, BY_RULES
    };

    Enrollment enrollment;
    String enrollmentId;
    Long clinicId;
    Long patientId;
    String reason;
    String note;
    Instant date;
    String clinicianId;
    String clinicianName;
    Type changeType;
    // HT status category (or possibly enrollment status)
    String fromStatus;
    String toStatus;
    // If change is due to checkin
    SurveyPayload surveyPayload;
    List<String> checkinIds;
}