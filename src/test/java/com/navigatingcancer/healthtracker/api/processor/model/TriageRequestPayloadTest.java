package com.navigatingcancer.healthtracker.api.processor.model;

import com.navigatingcancer.healthtracker.api.rest.representation.PatientRequest;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

public class TriageRequestPayloadTest {
    private PatientRequest patientRequest;
    private Long clinicId = 2L;
    private Long patientId = 33L;
    private Long providerId = 6L;
    private Long locationId = 12L;

    @Before
    public void setup() throws Exception {
        this.patientRequest = new PatientRequest();
        patientRequest.setClinicId(clinicId);
        patientRequest.setLocationId(locationId);
        patientRequest.setPatientId(patientId);
        patientRequest.setProviderId(providerId);
    }

    @Test
    public void createRefillPayload() {
        TriageRequestPayload payload = TriageRequestPayload.createRefillPayload(patientRequest);
        Assert.assertEquals(clinicId, payload.getClinicId());
        Assert.assertEquals(locationId, payload.getLocationId());
        Assert.assertEquals(patientId, payload.getPatientId());
        Assert.assertEquals(providerId, payload.getProviderId());
        Assert.assertEquals("immediate", payload.getAlertLevel());

        List<TriageRequestPayload.Reason> reasons = payload.getReasons();
        Assert.assertEquals("created 1 triage reason", 1, reasons.size());

        Assert.assertEquals("reason type is valid", "refill_request", reasons.get(0).getReasonType());
        Assert.assertEquals("reason description is valid", "Refill request", reasons.get(0).getDescription());
        Assert.assertEquals("issue sub-type is valid", "Refill Request", reasons.get(0).getIssueSubtype());
    }

    @Test
    public void createCallPayload() {
        TriageRequestPayload payload = TriageRequestPayload.createCallPayload(patientRequest);
        Assert.assertEquals(clinicId, payload.getClinicId());
        Assert.assertEquals(locationId, payload.getLocationId());
        Assert.assertEquals(patientId, payload.getPatientId());
        Assert.assertEquals(providerId, payload.getProviderId());
        Assert.assertEquals("immediate", payload.getAlertLevel());

        List<TriageRequestPayload.Reason> reasons = payload.getReasons();
        Assert.assertEquals("created 1 triage reason", 1, reasons.size());

        Assert.assertEquals("reason type is valid", "requested_call", reasons.get(0).getReasonType());
        Assert.assertEquals("reason description is valid", "Call request", reasons.get(0).getDescription());
        Assert.assertEquals("issue sub-type is valid", "Call Request", reasons.get(0).getIssueSubtype());
    }
}