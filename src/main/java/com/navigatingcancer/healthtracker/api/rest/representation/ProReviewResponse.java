package com.navigatingcancer.healthtracker.api.rest.representation;

import com.navigatingcancer.healthtracker.api.data.model.Adherence;
import com.navigatingcancer.healthtracker.api.data.model.EHRDelivery;
import com.navigatingcancer.healthtracker.api.data.model.HealthTrackerStatus;
import com.navigatingcancer.healthtracker.api.data.model.ProReview;
import com.navigatingcancer.healthtracker.api.data.model.ProReviewNote;
import com.navigatingcancer.healthtracker.api.data.model.SideEffect;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.Data;

/** Copy of adherence configured to emit fields in camelCase */
@Data
class ProReviewAdherence {
  String status;
  String medication;
  String reason = "";
  String reasonDetails = "";

  ProReviewAdherence(Adherence adherence) {
    this.setStatus(adherence.getStatus());
    this.setMedication(adherence.getMedication());
    this.setReason(adherence.getReason());
    this.setReasonDetails(adherence.getReasonDetails());
  }
}

/** Copy of SideEffect with overridden JSON names */
@Data
class ProReviewSideEffect {
  String symptomType;
  String frequency;
  String severity;
  String rawSeverity;
  String interference;
  String location;
  String occurrence;

  ProReviewSideEffect(SideEffect sideEffect) {
    this.setSymptomType(sideEffect.getSymptomType());
    this.setFrequency(sideEffect.getFrequency());
    this.setSeverity(sideEffect.getSeverity());
    this.setRawSeverity(sideEffect.getRawSeverity());
    this.setInterference(sideEffect.getInterference());
    this.setLocation(sideEffect.getLocation());
    this.setOccurrence(sideEffect.getOccurrence());
  }
}

@Data
public class ProReviewResponse {
  private String id;
  private Long clinicId;
  private String enrollmentId;
  private List<String> checkInIds;
  private EHRDelivery ehrDelivery;
  private List<ProReviewNote> notes;
  private List<ProReviewSideEffect> sideEffects;
  private List<ProReviewAdherence> oralAdherence;
  private HealthTrackerStatus healthTrackerStatus;

  public ProReviewResponse(ProReview proReview, List<ProReviewNote> proReviewNotes) {
    this.id = proReview.getId();
    this.clinicId = proReview.getClinicId();
    this.enrollmentId = proReview.getEnrollmentId();
    this.checkInIds = proReview.getCheckInIds();
    this.ehrDelivery = proReview.getEhrDelivery();
    this.healthTrackerStatus = proReview.getHealthTrackerStatus();
    this.notes = proReviewNotes;

    this.sideEffects =
        Optional.ofNullable(proReview.getSideEffects()).orElse(Collections.emptyList()).stream()
            .map(ProReviewSideEffect::new)
            .collect(Collectors.toList());

    this.oralAdherence =
        Optional.ofNullable(proReview.getOralAdherence()).orElse(Collections.emptyList()).stream()
            .map(ProReviewAdherence::new)
            .collect(Collectors.toList());
  }
}
