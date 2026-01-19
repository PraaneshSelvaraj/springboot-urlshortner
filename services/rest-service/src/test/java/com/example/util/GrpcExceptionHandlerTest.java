package com.example.util;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.exception.AuthenticationException;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("GrpcExceptionHandler Tests")
class GrpcExceptionHandlerTest {

  @Test
  @DisplayName("Should handle ALREADY_EXISTS status")
  void shouldHandleAlreadyExistsStatus() {
    StatusRuntimeException exception =
        Status.ALREADY_EXISTS.withDescription("User already exists").asRuntimeException();

    RuntimeException result =
        GrpcExceptionHandler.handleGrpcException(exception, "Default message");

    assertThat(result).isInstanceOf(IllegalStateException.class);
    assertThat(result.getMessage()).isEqualTo("User already exists");
  }

  @Test
  @DisplayName("Should handle INVALID_ARGUMENT status")
  void shouldHandleInvalidArgumentStatus() {
    StatusRuntimeException exception =
        Status.INVALID_ARGUMENT.withDescription("Invalid email format").asRuntimeException();

    RuntimeException result = GrpcExceptionHandler.handleGrpcException(exception, "Default");

    assertThat(result).isInstanceOf(IllegalArgumentException.class);
    assertThat(result.getMessage()).isEqualTo("Invalid email format");
  }

  @Test
  @DisplayName("Should handle NOT_FOUND status")
  void shouldHandleNotFoundStatus() {
    StatusRuntimeException exception =
        Status.NOT_FOUND.withDescription("User not found").asRuntimeException();

    RuntimeException result = GrpcExceptionHandler.handleGrpcException(exception, "Default");

    assertThat(result).isInstanceOf(NoSuchElementException.class);
    assertThat(result.getMessage()).isEqualTo("User not found");
  }

  @Test
  @DisplayName("Should handle UNAVAILABLE status")
  void shouldHandleUnavailableStatus() {
    StatusRuntimeException exception =
        Status.UNAVAILABLE.withDescription("Service unavailable").asRuntimeException();

    RuntimeException result = GrpcExceptionHandler.handleGrpcException(exception, "Default");

    assertThat(result).isInstanceOf(IllegalStateException.class);
    assertThat(result.getMessage()).isEqualTo("Service unavailable");
  }

  @Test
  @DisplayName("Should handle DEADLINE_EXCEEDED status")
  void shouldHandleDeadlineExceededStatus() {
    StatusRuntimeException exception =
        Status.DEADLINE_EXCEEDED.withDescription("Request timeout").asRuntimeException();

    RuntimeException result = GrpcExceptionHandler.handleGrpcException(exception, "Default");

    assertThat(result).isInstanceOf(IllegalStateException.class);
    assertThat(result.getMessage()).isEqualTo("Request timeout");
  }

  @Test
  @DisplayName("Should handle UNAUTHENTICATED status")
  void shouldHandleUnauthenticatedStatus() {
    StatusRuntimeException exception =
        Status.UNAUTHENTICATED.withDescription("Invalid credentials").asRuntimeException();

    RuntimeException result = GrpcExceptionHandler.handleGrpcException(exception, "Default");

    assertThat(result).isInstanceOf(AuthenticationException.class);
    assertThat(result.getMessage()).isEqualTo("Invalid credentials");
  }

  @Test
  @DisplayName("Should handle PERMISSION_DENIED status")
  void shouldHandlePermissionDeniedStatus() {
    StatusRuntimeException exception =
        Status.PERMISSION_DENIED.withDescription("Access denied").asRuntimeException();

    RuntimeException result = GrpcExceptionHandler.handleGrpcException(exception, "Default");

    assertThat(result).isInstanceOf(SecurityException.class);
    assertThat(result.getMessage()).isEqualTo("Access denied");
  }

  @Test
  @DisplayName("Should use exception message when description is null")
  void shouldUseExceptionMessageWhenDescriptionIsNull() {
    StatusRuntimeException exception = Status.INTERNAL.asRuntimeException();

    RuntimeException result = GrpcExceptionHandler.handleGrpcException(exception, "Default error");

    assertThat(result).isInstanceOf(RuntimeException.class);
    assertThat(result.getMessage()).isNotNull();
  }

  @Test
  @DisplayName("Should use exception message when description is empty")
  void shouldUseExceptionMessageWhenDescriptionIsEmpty() {
    StatusRuntimeException exception = Status.INTERNAL.withDescription("").asRuntimeException();

    RuntimeException result = GrpcExceptionHandler.handleGrpcException(exception, "Default error");

    assertThat(result).isInstanceOf(RuntimeException.class);
    assertThat(result.getMessage()).isNotNull();
  }

  @Test
  @DisplayName("Should handle unknown status codes")
  void shouldHandleUnknownStatusCodes() {
    StatusRuntimeException exception =
        Status.INTERNAL.withDescription("Internal error").asRuntimeException();

    RuntimeException result = GrpcExceptionHandler.handleGrpcException(exception, "Default");

    assertThat(result).isInstanceOf(RuntimeException.class);
    assertThat(result.getMessage()).isEqualTo("Internal error");
  }

  @Test
  @DisplayName("Should preserve exception message over default")
  void shouldPreserveExceptionMessageOverDefault() {
    StatusRuntimeException exception =
        Status.UNAUTHENTICATED.withDescription("Token expired").asRuntimeException();

    RuntimeException result =
        GrpcExceptionHandler.handleGrpcException(exception, "Should not see this");

    assertThat(result.getMessage()).isEqualTo("Token expired");
    assertThat(result.getMessage()).doesNotContain("Should not see this");
  }

  @Test
  @DisplayName("Should handle CANCELLED status")
  void shouldHandleCancelledStatus() {
    StatusRuntimeException exception =
        Status.CANCELLED.withDescription("Request cancelled").asRuntimeException();

    RuntimeException result = GrpcExceptionHandler.handleGrpcException(exception, "Default");

    assertThat(result).isInstanceOf(RuntimeException.class);
    assertThat(result.getMessage()).isEqualTo("Request cancelled");
  }
}
