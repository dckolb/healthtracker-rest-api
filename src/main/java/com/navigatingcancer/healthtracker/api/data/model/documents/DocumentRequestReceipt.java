package com.navigatingcancer.healthtracker.api.data.model.documents;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DocumentRequestReceipt {
  private DocumentLocator locator;
  private String path;

  @Data
  public static class Response {
    DocumentRequestReceipt data;
  }
}
