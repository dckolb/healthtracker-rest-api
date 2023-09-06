package com.navigatingcancer.healthtracker.api.data.model.documents;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DocumentLocator {
  private Long clinicId;
  private Long patientId;
  private String documentId;
  private String title;
}
