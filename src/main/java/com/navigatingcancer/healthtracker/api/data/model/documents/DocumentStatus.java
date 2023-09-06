package com.navigatingcancer.healthtracker.api.data.model.documents;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DocumentStatus {
  private String status;
  private String errorDetails;

  @Data
  public static class Response {
    DocumentStatus data;
  }
}
