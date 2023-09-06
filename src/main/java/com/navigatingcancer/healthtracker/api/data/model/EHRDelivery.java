package com.navigatingcancer.healthtracker.api.data.model;

import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;

@Data
@AllArgsConstructor
public class EHRDelivery {
  @NonNull private PdfDeliveryStatus status;
  private String documentId;
  @NonNull private String submittedBy;
  @NonNull private Date submittedDate;
  private String details;
}
