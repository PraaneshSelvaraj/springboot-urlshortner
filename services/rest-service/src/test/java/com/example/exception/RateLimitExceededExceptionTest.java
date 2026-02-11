package com.example.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("RateLimitExceededException Tests")
class RateLimitExceededExceptionTest {

  @Test
  @DisplayName("Should create exception with message")
  void shouldCreateExceptionWithMessage() {
    String message = "Rate limit exceeded";

    RateLimitExceededException exception = new RateLimitExceededException(message);

    assertThat(exception.getMessage()).isEqualTo(message);
  }

  @Test
  @DisplayName("Should be throwable")
  void shouldBeThrowable() {
    String message = "Too many requests";

    assertThatThrownBy(() -> {
      throw new RateLimitExceededException(message);
    })
        .isInstanceOf(RateLimitExceededException.class)
        .isInstanceOf(RuntimeException.class)
        .hasMessage(message);
  }

  @Test
  @DisplayName("Should handle null message")
  void shouldHandleNullMessage() {
    RateLimitExceededException exception = new RateLimitExceededException(null);

    assertThat(exception.getMessage()).isNull();
  }

  @Test
  @DisplayName("Should handle empty message")
  void shouldHandleEmptyMessage() {
    RateLimitExceededException exception = new RateLimitExceededException("");

    assertThat(exception.getMessage()).isEmpty();
  }

  @Test
  @DisplayName("Should preserve detailed error message")
  void shouldPreserveDetailedErrorMessage() {
    String detailedMessage = "Rate limit exceeded. Maximum 10 requests per 60 seconds for endpoint POST:/api/urls";

    RateLimitExceededException exception = new RateLimitExceededException(detailedMessage);

    assertThat(exception.getMessage()).isEqualTo(detailedMessage);
  }

  @Test
  @DisplayName("Should be instance of RuntimeException")
  void shouldBeInstanceOfRuntimeException() {
    RateLimitExceededException exception = new RateLimitExceededException("test");

    assertThat(exception).isInstanceOf(RuntimeException.class);
  }

  @Test
  @DisplayName("Should have accessible cause when rethrown")
  void shouldHaveAccessibleCauseWhenRethrown() {
    String message = "Rate limit hit";

    try {
      throw new RateLimitExceededException(message);
    } catch (RateLimitExceededException e) {
      assertThat(e.getMessage()).isEqualTo(message);
      assertThat(e.getCause()).isNull();
    }
  }
}
