package com.navigatingcancer.healthtracker.api.data.model;

import java.util.List;
import lombok.*;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@EqualsAndHashCode
@Document(collection = "pro_review_activities")
public class ProReviewActivity extends AbstractDocument {
  private String proReviewId;
  private Long patientId;
  private List<String> selectedActions;
  private String notes;
  private boolean inPerson;
  private int minutes;
  private Long enteredById;
  private Long clinicId;

  // Set after syncing with GC
  private Long patientActivityId;

  public boolean isSynced() {
    return patientActivityId != null;
  }
}
