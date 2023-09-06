package com.navigatingcancer.healthtracker.api.data.model.documents;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DocumentTemporaryUrl {
  private String url;

  @Data
  public static class Response {
    DocumentTemporaryUrl data;
  }
}
