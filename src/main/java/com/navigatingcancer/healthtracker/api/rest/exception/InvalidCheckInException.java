package com.navigatingcancer.healthtracker.api.rest.exception;

public class InvalidCheckInException extends RuntimeException {
	
	public InvalidCheckInException() {
		super("Checkin does not exist or already completed");
	}

}
