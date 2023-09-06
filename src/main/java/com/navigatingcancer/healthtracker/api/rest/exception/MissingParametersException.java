package com.navigatingcancer.healthtracker.api.rest.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value= HttpStatus.BAD_REQUEST)
public class MissingParametersException extends RuntimeException{
    public MissingParametersException(String message) {
        super(message);
    }
}
