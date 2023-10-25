package com.navigatingcancer.healthtracker.api.jobs;

public class RetryableJobException extends Exception {
  public RetryableJobException(String message, Throwable cause) {
    super(message, cause);
  }

  public RetryableJobException(String message) {
    super(message);
  }
}
