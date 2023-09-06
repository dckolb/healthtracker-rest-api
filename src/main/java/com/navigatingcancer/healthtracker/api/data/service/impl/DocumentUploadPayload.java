package com.navigatingcancer.healthtracker.api.data.service.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DocumentUploadPayload {
  @JsonProperty("clinic_id")
  private Long clinicId;

  @JsonProperty("patient_id")
  private Long patientId;

  @JsonProperty("document_title")
  private String documentTitle;

  @JsonProperty("document_url")
  private String documentUrl;
}
