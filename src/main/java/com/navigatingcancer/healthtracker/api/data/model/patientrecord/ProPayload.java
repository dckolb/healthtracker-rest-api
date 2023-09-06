package com.navigatingcancer.healthtracker.api.data.model.patientrecord;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.navigatingcancer.healthtracker.api.data.model.Adherence;
import com.navigatingcancer.healthtracker.api.data.model.Enrollment;
import com.navigatingcancer.healthtracker.api.data.model.SideEffect;
import com.navigatingcancer.healthtracker.api.data.util.DateUtils;
import com.navigatingcancer.healthtracker.api.processor.model.SymptomDetails;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
public class ProPayload {
    @JsonProperty("message_id")
    UUID messageId = UUID.randomUUID();

    @JsonProperty("enrollment_id")
    String enrollmentId;

    @JsonProperty("patient_id")
    Long patientId;

    @JsonProperty("clinic_id")
    Long clinicId;

    @JsonProperty("created_by")
    String createdBy;

    @JsonProperty("created_at")
    String createdAt;

    @JsonProperty("end_current_cycle")
    Boolean endCurrentCycle;

    @JsonProperty("all_adherences")
    List<Adherence> allAdherences = new ArrayList<>();

    @JsonProperty("all_side_effects")
    List<SideEffect> allSideEffects = new ArrayList<>();

    @JsonProperty("all_symptom_details")
    List<SymptomDetails> allSymptomDetails = new ArrayList<>();

    @JsonProperty("record_timestamp")
    String recordTimeStamp = DateUtils.timeNowInIsoDateFormat();

    public ProPayload(Enrollment enrollment, String createdBy) {
        this.setEnrollmentId(enrollment.getId());
        this.setPatientId(enrollment.getPatientId());
        this.setClinicId(enrollment.getClinicId());
        this.setCreatedBy(PayloadUtils.defaultCreatedBy(createdBy));
        this.setCreatedAt(DateUtils.timeNowInIsoDateFormat());
    }
}
