package com.navigatingcancer.healthtracker.api.data.model;

import javax.validation.constraints.NotNull;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Data
@Document(collection = "call_attempts")
public class CallAttempt extends AbstractDocument {
    @Indexed
    private @NotNull String checkInId;
    private @NotNull Long clinicId;
    private @NotNull Long locationId;
    private @NotNull String enrollmentId;
    private @NotNull String notes;
}