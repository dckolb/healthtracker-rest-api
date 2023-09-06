package com.navigatingcancer.healthtracker.api.events;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ProReviewNoteEvent {
  String enrollmentId;
  String by;
  Long clinicId;
  Long patientId;
  List<String> checkinIds;
  String proReviewId;
  String noteContent;
}
