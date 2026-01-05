package com.example.util;

import static org.assertj.core.api.Assertions.assertThat;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("GrpcExceptionHandler Tests - Exception Mapping Utility")
class GrpcExceptionHandlerTest {

  @Test
  @DisplayName("Should map ALREADY_EXISTS to IllegalStateException")
  void shouldMapAlreadyExistsToIllegalStateException() {
    StatusRuntimeException grpcException =
        new StatusRuntimeException(Status.ALREADY_EXISTS.withDescription("User already exists"));

    RuntimeException result =
        GrpcExceptionHandler.handleGrpcException(grpcException, "Failed to create user");

    assertThat(result)
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Resource already exists");
  }

  @Test
  @DisplayName("Should map INVALID_ARGUMENT to IllegalArgumentException")
  void shouldMapInvalidArgumentToIllegalArgumentException() {
    StatusRuntimeException grpcException =
        new StatusRuntimeException(Status.INVALID_ARGUMENT.withDescription("Invalid email format"));

    RuntimeException result =
        GrpcExceptionHandler.handleGrpcException(grpcException, "Failed to validate input");

    assertThat(result)
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Invalid data:");
  }

  @Test
  @DisplayName("Should map NOT_FOUND to NoSuchElementException")
  void shouldMapNotFoundToNoSuchElementException() {
    StatusRuntimeException grpcException =
        new StatusRuntimeException(Status.NOT_FOUND.withDescription("User not found"));

    RuntimeException result =
        GrpcExceptionHandler.handleGrpcException(grpcException, "Failed to find user");

    assertThat(result).isInstanceOf(NoSuchElementException.class).hasMessage("Resource not found");
  }

  @Test
  @DisplayName("Should map UNAVAILABLE to IllegalStateException")
  void shouldMapUnavailableToIllegalStateException() {
    StatusRuntimeException grpcException =
        new StatusRuntimeException(Status.UNAVAILABLE.withDescription("Service unavailable"));

    RuntimeException result =
        GrpcExceptionHandler.handleGrpcException(grpcException, "Failed to connect");

    assertThat(result)
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Service is currently unavailable. Please try again later.");
  }

  @Test
  @DisplayName("Should map DEADLINE_EXCEEDED to IllegalStateException")
  void shouldMapDeadlineExceededToIllegalStateException() {
    StatusRuntimeException grpcException =
        new StatusRuntimeException(Status.DEADLINE_EXCEEDED.withDescription("Request timeout"));

    RuntimeException result =
        GrpcExceptionHandler.handleGrpcException(grpcException, "Failed to complete request");

    assertThat(result)
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Request timed out. Please try again.");
  }

  @Test
  @DisplayName("Should map UNAUTHENTICATED to SecurityException")
  void shouldMapUnauthenticatedToSecurityException() {
    StatusRuntimeException grpcException =
        new StatusRuntimeException(Status.UNAUTHENTICATED.withDescription("Invalid token"));

    RuntimeException result =
        GrpcExceptionHandler.handleGrpcException(grpcException, "Failed to authenticate");

    assertThat(result)
        .isInstanceOf(SecurityException.class)
        .hasMessage("Authentication required");
  }

  @Test
  @DisplayName("Should map PERMISSION_DENIED to SecurityException")
  void shouldMapPermissionDeniedToSecurityException() {
    StatusRuntimeException grpcException =
        new StatusRuntimeException(
            Status.PERMISSION_DENIED.withDescription("Insufficient permissions"));

    RuntimeException result =
        GrpcExceptionHandler.handleGrpcException(grpcException, "Failed to authorize");

    assertThat(result).isInstanceOf(SecurityException.class).hasMessage("Permission denied");
  }

  @Test
  @DisplayName("Should map unknown status to RuntimeException")
  void shouldMapUnknownStatusToRuntimeException() {
    StatusRuntimeException grpcException =
        new StatusRuntimeException(Status.INTERNAL.withDescription("Internal server error"));

    RuntimeException result =
        GrpcExceptionHandler.handleGrpcException(grpcException, "Failed to process request");

    assertThat(result)
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Failed to process request");
  }

  @Test
  @DisplayName("Should preserve gRPC error message in INVALID_ARGUMENT exception")
  void shouldPreserveGrpcErrorMessageInException() {
    String customErrorMessage = "Email must be valid";
    StatusRuntimeException grpcException =
        new StatusRuntimeException(Status.INVALID_ARGUMENT.withDescription(customErrorMessage));

    RuntimeException result =
        GrpcExceptionHandler.handleGrpcException(grpcException, "Validation failed");

    assertThat(result.getMessage()).contains("Invalid data:").contains(customErrorMessage);
  }

  @Test
  @DisplayName("Should include default message in RuntimeException for unknown status")
  void shouldIncludeDefaultMessageInRuntimeException() {
    String defaultMessage = "Operation failed";
    String grpcMessage = "Something went wrong";
    StatusRuntimeException grpcException =
        new StatusRuntimeException(Status.UNKNOWN.withDescription(grpcMessage));

    RuntimeException result =
        GrpcExceptionHandler.handleGrpcException(grpcException, defaultMessage);

    assertThat(result.getMessage()).contains(defaultMessage).contains(grpcMessage);
  }
}
