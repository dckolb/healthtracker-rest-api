package com.navigatingcancer.healthtracker.api.rest.exception;

public class RabbitMQException extends RuntimeException {
    public RabbitMQException(String message) {
        super(message);
    }
}
