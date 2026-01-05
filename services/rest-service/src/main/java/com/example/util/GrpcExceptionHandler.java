package com.example.util;

import io.grpc.StatusRuntimeException;
import java.util.NoSuchElementException;

public class GrpcExceptionHandler {

  public static RuntimeException handleGrpcException(
      StatusRuntimeException e, String defaultMessage) {
    return switch (e.getStatus().getCode()) {
      case ALREADY_EXISTS -> new IllegalStateException("Resource already exists");
      case INVALID_ARGUMENT -> new IllegalArgumentException("Invalid data: " + e.getMessage());
      case NOT_FOUND -> new NoSuchElementException("Resource not found");
      case UNAVAILABLE ->
          new IllegalStateException("Service is currently unavailable. Please try again later.");
      case DEADLINE_EXCEEDED -> new IllegalStateException("Request timed out. Please try again.");
      case UNAUTHENTICATED -> new SecurityException("Authentication required");
      case PERMISSION_DENIED -> new SecurityException("Permission denied");
      default -> new RuntimeException(defaultMessage + ": " + e.getMessage());
    };
  }
}
