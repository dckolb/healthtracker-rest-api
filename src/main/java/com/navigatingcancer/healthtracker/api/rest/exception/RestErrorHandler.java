package com.navigatingcancer.healthtracker.api.rest.exception;

import javax.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
@Slf4j
public class RestErrorHandler extends ResponseEntityExceptionHandler {

  @ExceptionHandler(BadDataException.class)
  public ResponseEntity<Object> handleBadDataException(BadDataException ex, WebRequest request) {
    log.error("handleBadDataException", ex);
    return handleExceptionInternal(
        ex,
        new ErrorMessage(HttpStatus.BAD_REQUEST.value(), ex.getMessage()),
        new HttpHeaders(),
        HttpStatus.BAD_REQUEST,
        request);
  }

  @ExceptionHandler(MissingParametersException.class)
  public ResponseEntity<Object> handleMissingParametersException(
      MissingParametersException ex, WebRequest request) {
    log.error("handleMissingParametersException", ex);
    return handleExceptionInternal(
        ex,
        new ErrorMessage(HttpStatus.BAD_REQUEST.value(), ex.getMessage()),
        new HttpHeaders(),
        HttpStatus.BAD_REQUEST,
        request);
  }

  @ExceptionHandler(DuplicateEnrollmentException.class)
  public ResponseEntity<Object> handleDuplicateEnrollmentException(
      DuplicateEnrollmentException ex, WebRequest request) {
    log.error("handleDuplicateEnrollmentException", ex);
    return handleExceptionInternal(
        ex,
        new ErrorMessage(HttpStatus.CONFLICT.value(), ex.getMessage()),
        new HttpHeaders(),
        HttpStatus.CONFLICT,
        request);
  }

  @ExceptionHandler(InvalidCheckInException.class)
  public ResponseEntity<Object> handleCheckInAlreadyCompletedException(
      InvalidCheckInException ex, WebRequest request) {
    log.error("handleCheckInAlreadyCompletedException", ex);
    return handleExceptionInternal(
        ex,
        new ErrorMessage(HttpStatus.CONFLICT.value(), ex.getMessage()),
        new HttpHeaders(),
        HttpStatus.CONFLICT,
        request);
  }

  @ExceptionHandler(RabbitMQException.class)
  public ResponseEntity<Object> handleRabbitException(RabbitMQException ex, WebRequest request) {
    log.error("handleRabbitException", ex);
    return handleExceptionInternal(
        ex,
        new ErrorMessage(HttpStatus.FAILED_DEPENDENCY.value(), ex.getMessage()),
        new HttpHeaders(),
        HttpStatus.FAILED_DEPENDENCY,
        request);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<Object> handleGenericException(Exception ex, WebRequest request) {
    log.error("handleGenericException", ex);
    return handleExceptionInternal(
        ex,
        new ErrorMessage(HttpStatus.INTERNAL_SERVER_ERROR.value(), ex.getMessage()),
        new HttpHeaders(),
        HttpStatus.INTERNAL_SERVER_ERROR,
        request);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<Object> handleConstraintViolationException(
      Exception ex, WebRequest request) {
    log.error("handleConstraintViolationException", ex);
    return handleExceptionInternal(
        ex,
        new ErrorMessage(HttpStatus.BAD_REQUEST.value(), ex.getMessage()),
        new HttpHeaders(),
        HttpStatus.BAD_REQUEST,
        request);
  }

  @ExceptionHandler(UnknownEnrollmentException.class)
  public ResponseEntity<Object> handleUnknownEnrollmentException(Exception ex, WebRequest request) {
    log.error("handleUnknownEnrollmentException", ex);
    return handleExceptionInternal(
        ex,
        new ErrorMessage(HttpStatus.NOT_FOUND.value(), ex.getMessage()),
        new HttpHeaders(),
        HttpStatus.NOT_FOUND,
        request);
  }

  @ExceptionHandler(RecordNotFoundException.class)
  public ResponseEntity<Object> handleRecordNotFoundException(Exception ex, WebRequest request) {
    log.error("handleUnknownEnrollmentException", ex);
    return handleExceptionInternal(
        ex,
        new ErrorMessage(HttpStatus.NOT_FOUND.value(), ex.getMessage()),
        new HttpHeaders(),
        HttpStatus.NOT_FOUND,
        request);
  }

  @Override
  protected ResponseEntity<Object> handleExceptionInternal(
      Exception ex, Object body, HttpHeaders headers, HttpStatus status, WebRequest request) {
    return new ResponseEntity<>(new ErrorMessage(status.value(), ex.getMessage()), headers, status);
  }
}
