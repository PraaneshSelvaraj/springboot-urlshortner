package com.example.exception;

import java.util.NoSuchElementException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(InvalidUrlException.class)
  public ResponseEntity<ErrorResponse> handleInvalidUrlException(
      InvalidUrlException ex, WebRequest request) {
    ErrorResponse error = new ErrorResponse(HttpStatus.BAD_REQUEST.value(), ex.getMessage());
    return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(NoSuchElementException.class)
  public ResponseEntity<ErrorResponse> handleNoSuchElementException(
      NoSuchElementException ex, WebRequest request) {
    ErrorResponse error = new ErrorResponse(HttpStatus.NOT_FOUND.value(), ex.getMessage());
    return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler(UrlExpiredException.class)
  public ResponseEntity<ErrorResponse> handleUrlExpiredException(
      UrlExpiredException ex, WebRequest request) {
    ErrorResponse error = new ErrorResponse(HttpStatus.GONE.value(), ex.getMessage());
    return new ResponseEntity<>(error, HttpStatus.GONE);
  }

  @ExceptionHandler(ThresholdReachedException.class)
  public ResponseEntity<ErrorResponse> handleThresholdReachedException(
      ThresholdReachedException ex, WebRequest request) {
    ErrorResponse error = new ErrorResponse(HttpStatus.TOO_MANY_REQUESTS.value(), ex.getMessage());
    return new ResponseEntity<>(error, HttpStatus.TOO_MANY_REQUESTS);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
      IllegalArgumentException ex, WebRequest request) {
    ErrorResponse error = new ErrorResponse(HttpStatus.BAD_REQUEST.value(), ex.getMessage());
    return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(IllegalStateException.class)
  public ResponseEntity<ErrorResponse> handleIllegalStateException(
      IllegalStateException ex, WebRequest request) {
    ErrorResponse error = new ErrorResponse(HttpStatus.CONFLICT.value(), ex.getMessage());
    return new ResponseEntity<>(error, HttpStatus.CONFLICT);
  }

  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<ErrorResponse> handleAuthenticationException(
      AuthenticationException ex, WebRequest request) {
    ErrorResponse error = new ErrorResponse(HttpStatus.UNAUTHORIZED.value(), ex.getMessage());
    return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
  }

  @ExceptionHandler(SecurityException.class)
  public ResponseEntity<ErrorResponse> handleSecurityException(
      SecurityException ex, WebRequest request) {
    ErrorResponse error = new ErrorResponse(HttpStatus.FORBIDDEN.value(), ex.getMessage());
    return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
  }

  @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
  public ResponseEntity<ErrorResponse> handleAccessDeniedException(
      org.springframework.security.access.AccessDeniedException ex, WebRequest request) {
    ErrorResponse error =
        new ErrorResponse(HttpStatus.FORBIDDEN.value(), "Access denied. Insufficient permissions.");
    return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
  }

  @ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
  public ResponseEntity<ErrorResponse> handleAuthenticationException(
      org.springframework.security.core.AuthenticationException ex, WebRequest request) {
    ErrorResponse error =
        new ErrorResponse(
            HttpStatus.UNAUTHORIZED.value(), "Authentication failed: " + ex.getMessage());
    return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
  }

  @ExceptionHandler(TokenBlacklistException.class)
  public ResponseEntity<ErrorResponse> handleTokenBlacklistException(
      TokenBlacklistException ex, WebRequest request) {
    ErrorResponse error =
        new ErrorResponse(
            HttpStatus.SERVICE_UNAVAILABLE.value(), "Logout service temporarily unavailable");
    return new ResponseEntity<>(error, HttpStatus.SERVICE_UNAVAILABLE);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGlobalException(Exception ex, WebRequest request) {
    ErrorResponse error =
        new ErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "An unexpected error occurred: " + ex.getMessage());
    return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
  }
}
