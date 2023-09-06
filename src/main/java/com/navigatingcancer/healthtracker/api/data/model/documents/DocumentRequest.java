package com.navigatingcancer.healthtracker.api.data.model.documents;

import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DocumentRequest {
  private static final String PRO_TEMPLATE_NAME = "pro";

  private String templateName;
  private Map<String, ?> params;

  public static DocumentRequest proDocumentRequest(
      Long clinicId, Long patientId, String proReviewId) {
    return DocumentRequest.builder()
        .templateName(PRO_TEMPLATE_NAME)
        .params(
            Map.of(
                "clinicId", clinicId.toString(),
                "patientId", patientId.toString(),
                "proReviewId", proReviewId))
        .build();
  }
}
