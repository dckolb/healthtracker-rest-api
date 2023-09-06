package com.navigatingcancer.healthtracker.api.data.model.patientrecord;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.navigatingcancer.healthtracker.api.data.util.DateUtils;
import lombok.Data;

import java.util.UUID;

@Data
public class ProSentToEhrPayload {
    @JsonProperty("message_id")
    UUID messageId = UUID.randomUUID();

    @JsonProperty("enrollment_id")
    String enrollmentId;

    @JsonProperty("clinic_id")
    Long clinicId;

    @JsonProperty("patient_id")
    Long patientId;

    @JsonProperty("pro_review_id")
    String proReviewId;

    @JsonProperty("document_title")
    String documentTitle;

    @JsonProperty("created_at")
    String createdAt;

    @JsonProperty("created_by")
    String createdBy;

    @JsonProperty("record_timestamp")
    String recordTimeStamp = DateUtils.timeNowInIsoDateFormat();

    public ProSentToEhrPayload(String enrollmentId, String by, Long clinicId, Long patientId, String proReviewId, String documentTitle) {
        this.setEnrollmentId(enrollmentId);
        this.setClinicId(clinicId);
        this.setPatientId(patientId);
        this.setCreatedBy(by);
        this.setCreatedAt(DateUtils.timeNowInIsoDateFormat());
        this.setProReviewId(proReviewId);
        this.setDocumentTitle(documentTitle);
    }
}
