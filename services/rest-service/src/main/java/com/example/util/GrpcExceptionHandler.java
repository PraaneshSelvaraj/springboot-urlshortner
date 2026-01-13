package com.example.util;

import com.example.exception.AuthenticationException;
import io.grpc.StatusRuntimeException;
import java.util.NoSuchElementException;

public class GrpcExceptionHandler {

  public static RuntimeException handleGrpcException(
      StatusRuntimeException e, String defaultMessage) {
    String description = e.getStatus().getDescription();
    String message = (description != null && !description.isEmpty()) ? description : e.getMessage();

    return switch (e.getStatus().getCode()) {
      case ALREADY_EXISTS -> new IllegalStateException(message);
      case INVALID_ARGUMENT -> new IllegalArgumentException(message);
      case NOT_FOUND -> new NoSuchElementException(message);
      case UNAVAILABLE -> new IllegalStateException(message);
      case DEADLINE_EXCEEDED -> new IllegalStateException(message);
      case UNAUTHENTICATED -> new AuthenticationException(message);
      case PERMISSION_DENIED -> new SecurityException(message);
      default -> new RuntimeException(message != null ? message : defaultMessage);
    };
  }
}
