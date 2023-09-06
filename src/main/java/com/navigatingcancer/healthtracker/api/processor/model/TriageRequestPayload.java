package com.navigatingcancer.healthtracker.api.processor.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.navigatingcancer.healthtracker.api.rest.representation.PatientRequest;
import lombok.Data;
import org.codehaus.plexus.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Data
public class TriageRequestPayload {
    private static final String REFILL_REQUEST_TYPE = "refill_request";

    // Needs to match one of TriagePathways::IncidentIssueSubtype.active.pluck(:name)
    private static final String REFILL_REQUEST_SUBTYPE = "Refill Request";
    private static final String REFILL_REQUEST_DESCRIPTION = "Refill request";

    private static final String REQUEST_CALL_TYPE = "requested_call";
    // Needs to match one of TriagePathways::IncidentIssueSubtype.active.pluck(:name)
    private static final String REQUEST_CALL_SUBTYPE = "Call Request";
    private static final String REQUEST_CALL_DESCRIPTION = "Call request";

    private static final String ALERT_LEVEL = "immediate";

    @Data
    public static class Reason {

        @JsonProperty("reason_type")
        String reasonType;

        @JsonProperty("description")
        String description;

        @JsonProperty("details")
        List<String> details;

        @JsonProperty("issue_subtype")
        String issueSubtype;
    }

    @JsonProperty("patient_id")
    Long patientId;

    @JsonProperty("clinic_id")
    Long clinicId;

    @JsonProperty("provider_id")
    Long providerId;

    @JsonProperty("location_id")
    Long locationId;

    @JsonProperty("alert_level")
    String alertLevel = "immediate";

    @JsonProperty("reasons")
    List<TriageRequestPayload.Reason> reasons = new ArrayList<>();

    public static TriageRequestPayload createRefillPayload(PatientRequest refillRequest){
        TriageRequestPayload triagePayload = createBasePayload(refillRequest);
        TriageRequestPayload.Reason reason = new TriageRequestPayload.Reason();
        reason.setReasonType(REFILL_REQUEST_TYPE);
        reason.setDescription(REFILL_REQUEST_DESCRIPTION);
        reason.setIssueSubtype(REFILL_REQUEST_SUBTYPE);

        String details = refillRequest.getPayload();
        if(!StringUtils.isBlank(details)) {
            reason.setDetails(Collections.singletonList(details));
        }

        triagePayload.getReasons().add(reason);
        return triagePayload;
    }

    public static TriageRequestPayload createCallPayload(PatientRequest callRequest){
        TriageRequestPayload triagePayload = createBasePayload(callRequest);
        TriageRequestPayload.Reason reason = new TriageRequestPayload.Reason();
        reason.setReasonType(REQUEST_CALL_TYPE);
        reason.setDescription(REQUEST_CALL_DESCRIPTION);
        reason.setIssueSubtype(REQUEST_CALL_SUBTYPE);

        String details = callRequest.getPayload();
        if(!StringUtils.isBlank(details)) {
           reason.setDetails(Collections.singletonList(String.format("Reason: %s", details)));
        }

        triagePayload.getReasons().add(reason);
        return triagePayload;
    }

    private static TriageRequestPayload createBasePayload(PatientRequest patientRequest){
        TriageRequestPayload triagePayload = new TriageRequestPayload();
        triagePayload.setClinicId(patientRequest.getClinicId());
        triagePayload.setLocationId(patientRequest.getLocationId());
        triagePayload.setPatientId(patientRequest.getPatientId());
        triagePayload.setProviderId(patientRequest.getProviderId());
        triagePayload.setAlertLevel(ALERT_LEVEL);
        return triagePayload;
    }
}
