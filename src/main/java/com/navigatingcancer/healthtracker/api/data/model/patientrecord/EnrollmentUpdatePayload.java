package com.navigatingcancer.healthtracker.api.data.model.patientrecord;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.navigatingcancer.healthtracker.api.data.auth.Identity;
import com.navigatingcancer.healthtracker.api.data.model.Enrollment;
import com.navigatingcancer.healthtracker.api.data.model.HealthTrackerStatus;
import com.navigatingcancer.healthtracker.api.data.model.HealthTrackerStatusCategory;
import com.navigatingcancer.healthtracker.api.data.util.DateUtils;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@NoArgsConstructor
@Data
public class EnrollmentUpdatePayload {
    @JsonProperty("message_id")
    UUID messageId = UUID.randomUUID();

    @JsonProperty("enrollment_id")
    String enrollmentId;

    @JsonProperty("clinic_id")
    Long clinicId;

    @JsonProperty("patient_id")
    Long patientId;

    @JsonProperty("enrollment_status")
    String enrollmentStatus;

    @JsonProperty("reason")
    String reason;

    @JsonProperty("reason_details")
    String reasonDetails;

    @JsonProperty("system_reported_missed_check_in")
    Boolean systemReportedMissedCheckIn;

    // NOTE : not used by GC at the moment
    @JsonProperty("system_reported_missed_check_in_description")
    String systemReportedMissedCheckInDescription;

    @JsonProperty("created_at")
    String createdAt;

    @JsonProperty("created_by")
    String createdBy;

    @JsonProperty("record_timestamp")
    String recordTimeStamp = DateUtils.timeNowInIsoDateFormat();

    public EnrollmentUpdatePayload(Enrollment enrollment, String createdBy) {
        this.setEnrollmentId(enrollment.getId());
        this.setClinicId(enrollment.getClinicId());
        this.setPatientId(enrollment.getPatientId());
        this.setEnrollmentStatus(enrollment.getStatus().toString().toUpperCase());
        this.setCreatedBy(PayloadUtils.defaultCreatedBy(createdBy));
        this.setCreatedAt(DateUtils.timeNowInIsoDateFormat());
    }

    public EnrollmentUpdatePayload(HealthTrackerStatus status, HealthTrackerStatusCategory from, HealthTrackerStatusCategory to, Identity identity) {
        this.setEnrollmentId(status.getId());
        this.setClinicId(status.getClinicId());
        this.setPatientId(status.getPatientInfo().getId());
        this.setEnrollmentStatus(
                status.getCategory() == HealthTrackerStatusCategory.COMPLETED ? "COMPLETED" : "ACTIVE");
        this.setCreatedBy(PayloadUtils.defaultCreatedBy(identity.getClinicianName()));
        this.setCreatedAt(DateUtils.timeNowInIsoDateFormat());
        this.setEnrollmentStatus("status changed");
        this.setReason("clinician action");
        this.setReasonDetails(
                "status changed from " + PayloadUtils.htCategoryToNiceName(from) + " to " + PayloadUtils.htCategoryToNiceName(to));
    }
}
