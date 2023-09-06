package com.navigatingcancer.healthtracker.api.rest.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value= HttpStatus.NOT_FOUND)
public class UnknownEnrollmentException extends RuntimeException {
    public UnknownEnrollmentException(String message) {
        super(message);
    }
}
