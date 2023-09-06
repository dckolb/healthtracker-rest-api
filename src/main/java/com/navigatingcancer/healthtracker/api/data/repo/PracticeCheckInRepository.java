package com.navigatingcancer.healthtracker.api.data.repo;

import com.navigatingcancer.healthtracker.api.data.model.PracticeCheckIn;

import java.util.Optional;

public interface PracticeCheckInRepository  {
    PracticeCheckIn save(PracticeCheckIn e);
    Optional<PracticeCheckIn> findFirstByClinicIdAndPatientId(Long clinicId, Long patientId);
}
