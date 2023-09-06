package com.navigatingcancer.healthtracker.api.rest.representation;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class EnrollmentIdentifiers {
    private String id;
    private Long patientId;
}
