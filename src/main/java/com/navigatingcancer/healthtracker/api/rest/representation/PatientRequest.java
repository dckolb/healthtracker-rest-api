package com.navigatingcancer.healthtracker.api.rest.representation;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
@RequiredArgsConstructor
@NoArgsConstructor
public class PatientRequest {
    @NotNull
    @NonNull
    Long patientId;

    @NotNull
    @NonNull
    Long clinicId;

    @NotBlank
    @NonNull
    String payload;

    Long locationId;
    Long providerId;
}
