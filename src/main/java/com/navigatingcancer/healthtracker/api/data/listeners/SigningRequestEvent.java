package com.navigatingcancer.healthtracker.api.data.listeners;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public final class SigningRequestEvent {
    private String signingRequestId;
    private String status;
    private Date timestamp;

}