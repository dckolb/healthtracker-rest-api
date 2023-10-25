package com.navigatingcancer.healthtracker.api.rest.representation;

import com.navigatingcancer.healthtracker.api.data.model.*;
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
  private List<ProReviewActivity> activities;
  private List<ProReviewSideEffect> sideEffects;
  private List<ProReviewAdherence> oralAdherence;
  private HealthTrackerStatus healthTrackerStatus;

  /** Deprecated in favor of `activities`, to be removed with next major revision of API */
  @Deprecated(forRemoval = true)
  private List<Integer> patientActivityIds = List.of();

  public ProReviewResponse(
      ProReview proReview, List<ProReviewNote> proReviewNotes, List<ProReviewActivity> activities) {
    this.id = proReview.getId();
    this.clinicId = proReview.getClinicId();
    this.enrollmentId = proReview.getEnrollmentId();
    this.checkInIds = proReview.getCheckInIds();
    this.ehrDelivery = proReview.getEhrDelivery();
    this.healthTrackerStatus = proReview.getHealthTrackerStatus();
    this.notes = proReviewNotes;
    this.activities = activities;

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
