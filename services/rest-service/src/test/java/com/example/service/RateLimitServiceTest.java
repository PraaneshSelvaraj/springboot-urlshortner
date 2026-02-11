package com.example.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
@DisplayName("RateLimitService Tests")
class RateLimitServiceTest {

  @Mock private StringRedisTemplate redisTemplate;

  @Mock private ValueOperations<String, String> valueOperations;

  @InjectMocks private RateLimitService rateLimitService;

  @BeforeEach
  void setUp() {
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
  }

  @Test
  @DisplayName("Should not rate limit on first request")
  void shouldNotRateLimitOnFirstRequest() {
    String key = "ratelimit:test:user1";
    int maxRequests = 10;
    int windowSeconds = 60;

    when(valueOperations.increment(key)).thenReturn(1L);

    boolean result = rateLimitService.isRateLimited(key, maxRequests, windowSeconds);

    assertThat(result).isFalse();
    verify(valueOperations).increment(key);
    verify(redisTemplate).expire(eq(key), eq(Duration.ofSeconds(windowSeconds)));
  }

  @Test
  @DisplayName("Should not rate limit when count is below max")
  void shouldNotRateLimitWhenCountIsBelowMax() {
    String key = "ratelimit:test:user1";
    int maxRequests = 10;
    int windowSeconds = 60;

    when(valueOperations.increment(key)).thenReturn(5L);

    boolean result = rateLimitService.isRateLimited(key, maxRequests, windowSeconds);

    assertThat(result).isFalse();
    verify(valueOperations).increment(key);
    verify(redisTemplate, never()).expire(eq(key), eq(Duration.ofSeconds(windowSeconds)));
  }

  @Test
  @DisplayName("Should not rate limit when count equals max")
  void shouldNotRateLimitWhenCountEqualsMax() {
    String key = "ratelimit:test:user1";
    int maxRequests = 10;
    int windowSeconds = 60;

    when(valueOperations.increment(key)).thenReturn(10L);

    boolean result = rateLimitService.isRateLimited(key, maxRequests, windowSeconds);

    assertThat(result).isFalse();
    verify(valueOperations).increment(key);
    verify(redisTemplate, never()).expire(eq(key), eq(Duration.ofSeconds(windowSeconds)));
  }

  @Test
  @DisplayName("Should rate limit when count exceeds max")
  void shouldRateLimitWhenCountExceedsMax() {
    String key = "ratelimit:test:user1";
    int maxRequests = 10;
    int windowSeconds = 60;

    when(valueOperations.increment(key)).thenReturn(11L);

    boolean result = rateLimitService.isRateLimited(key, maxRequests, windowSeconds);

    assertThat(result).isTrue();
    verify(valueOperations).increment(key);
    verify(redisTemplate, never()).expire(eq(key), eq(Duration.ofSeconds(windowSeconds)));
  }

  @Test
  @DisplayName("Should rate limit when count is far above max")
  void shouldRateLimitWhenCountIsFarAboveMax() {
    String key = "ratelimit:test:user1";
    int maxRequests = 10;
    int windowSeconds = 60;

    when(valueOperations.increment(key)).thenReturn(100L);

    boolean result = rateLimitService.isRateLimited(key, maxRequests, windowSeconds);

    assertThat(result).isTrue();
    verify(valueOperations).increment(key);
  }

  @Test
  @DisplayName("Should handle different users independently")
  void shouldHandleDifferentUsersIndependently() {
    String keyUser1 = "ratelimit:test:user1";
    String keyUser2 = "ratelimit:test:user2";
    int maxRequests = 10;
    int windowSeconds = 60;

    when(valueOperations.increment(keyUser1)).thenReturn(11L);
    when(valueOperations.increment(keyUser2)).thenReturn(1L);

    boolean resultUser1 = rateLimitService.isRateLimited(keyUser1, maxRequests, windowSeconds);
    boolean resultUser2 = rateLimitService.isRateLimited(keyUser2, maxRequests, windowSeconds);

    assertThat(resultUser1).isTrue();
    assertThat(resultUser2).isFalse();
  }

  @Test
  @DisplayName("Should handle different endpoints independently")
  void shouldHandleDifferentEndpointsIndependently() {
    String keyCreate = "ratelimit:url:create:user1";
    String keyDelete = "ratelimit:url:delete:user1";
    int maxRequests = 10;
    int windowSeconds = 60;

    when(valueOperations.increment(keyCreate)).thenReturn(11L);
    when(valueOperations.increment(keyDelete)).thenReturn(1L);

    boolean resultCreate = rateLimitService.isRateLimited(keyCreate, maxRequests, windowSeconds);
    boolean resultDelete = rateLimitService.isRateLimited(keyDelete, maxRequests, windowSeconds);

    assertThat(resultCreate).isTrue();
    assertThat(resultDelete).isFalse();
  }

  @Test
  @DisplayName("Should set expiration on first request")
  void shouldSetExpirationOnFirstRequest() {
    String key = "ratelimit:test:user1";
    int maxRequests = 10;
    int windowSeconds = 120;

    when(valueOperations.increment(key)).thenReturn(1L);

    rateLimitService.isRateLimited(key, maxRequests, windowSeconds);

    verify(redisTemplate).expire(eq(key), eq(Duration.ofSeconds(120)));
  }

  @Test
  @DisplayName("Should handle null count from Redis")
  void shouldHandleNullCountFromRedis() {
    String key = "ratelimit:test:user1";
    int maxRequests = 10;
    int windowSeconds = 60;

    when(valueOperations.increment(key)).thenReturn(null);

    boolean result = rateLimitService.isRateLimited(key, maxRequests, windowSeconds);

    assertThat(result).isFalse();
    verify(valueOperations).increment(key);
  }

  @Test
  @DisplayName("Should use different window sizes for different endpoints")
  void shouldUseDifferentWindowSizesForDifferentEndpoints() {
    String key = "ratelimit:test:user1";
    int maxRequests = 5;
    int windowSeconds30 = 30;
    int windowSeconds120 = 120;

    when(valueOperations.increment(key)).thenReturn(1L);

    rateLimitService.isRateLimited(key, maxRequests, windowSeconds30);
    verify(redisTemplate).expire(eq(key), eq(Duration.ofSeconds(30)));

    reset(redisTemplate);
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.increment(key)).thenReturn(1L);

    rateLimitService.isRateLimited(key, maxRequests, windowSeconds120);
    verify(redisTemplate).expire(eq(key), eq(Duration.ofSeconds(120)));
  }

  @Test
  @DisplayName("Should handle different max request limits")
  void shouldHandleDifferentMaxRequestLimits() {
    String key = "ratelimit:test:user1";
    int windowSeconds = 60;

    when(valueOperations.increment(key)).thenReturn(15L);

    boolean resultMax10 = rateLimitService.isRateLimited(key, 10, windowSeconds);
    assertThat(resultMax10).isTrue();

    reset(redisTemplate);
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.increment(key)).thenReturn(15L);

    boolean resultMax20 = rateLimitService.isRateLimited(key, 20, windowSeconds);
    assertThat(resultMax20).isFalse();
  }

  @Test
  @DisplayName("Should return false when Redis throws exception")
  void shouldReturnFalseWhenRedisThrowsException() {
    String key = "ratelimit:test:user1";
    int maxRequests = 10;
    int windowSeconds = 60;

    when(valueOperations.increment(key)).thenThrow(new RuntimeException("Redis connection failed"));

    boolean result = rateLimitService.isRateLimited(key, maxRequests, windowSeconds);

    assertThat(result).isFalse();
    verify(valueOperations).increment(key);
  }

  @Test
  @DisplayName("Should handle Redis exception gracefully and not affect application")
  void shouldHandleRedisExceptionGracefully() {
    String key = "ratelimit:test:user1";
    int maxRequests = 10;
    int windowSeconds = 60;

    when(redisTemplate.opsForValue()).thenThrow(new RuntimeException("Redis unavailable"));

    boolean result = rateLimitService.isRateLimited(key, maxRequests, windowSeconds);

    assertThat(result).isFalse();
  }

  @Test
  @DisplayName("Should not call expire when Redis returns zero")
  void shouldNotCallExpireWhenRedisReturnsZero() {
    String key = "ratelimit:test:user1";
    int maxRequests = 10;
    int windowSeconds = 60;

    when(valueOperations.increment(key)).thenReturn(0L);

    boolean result = rateLimitService.isRateLimited(key, maxRequests, windowSeconds);

    assertThat(result).isFalse();
    verify(redisTemplate, never()).expire(any(), any());
  }

  @Test
  @DisplayName("Should handle very large count values")
  void shouldHandleVeryLargeCountValues() {
    String key = "ratelimit:test:user1";
    int maxRequests = 10;
    int windowSeconds = 60;

    when(valueOperations.increment(key)).thenReturn(Long.MAX_VALUE);

    boolean result = rateLimitService.isRateLimited(key, maxRequests, windowSeconds);

    assertThat(result).isTrue();
    verify(valueOperations).increment(key);
  }

  @Test
  @DisplayName("Should handle edge case where count is exactly max + 1")
  void shouldHandleEdgeCaseWhereCountIsExactlyMaxPlusOne() {
    String key = "ratelimit:test:user1";
    int maxRequests = 100;
    int windowSeconds = 60;

    when(valueOperations.increment(key)).thenReturn(101L);

    boolean result = rateLimitService.isRateLimited(key, maxRequests, windowSeconds);

    assertThat(result).isTrue();
    verify(valueOperations).increment(key);
    verify(redisTemplate, never()).expire(any(), any());
  }
}
