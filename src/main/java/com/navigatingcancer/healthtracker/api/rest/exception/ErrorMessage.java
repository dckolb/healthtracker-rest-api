package com.navigatingcancer.healthtracker.api.rest.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
public class ErrorMessage {
    private Integer status;
    private String message;
}
