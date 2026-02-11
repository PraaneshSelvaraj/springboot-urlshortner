package com.example.service;

import java.time.Duration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RateLimitService {

  private final StringRedisTemplate redisTemplate;

  public RateLimitService(StringRedisTemplate redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  public boolean isRateLimited(String key, int maxRequests, int windowSeconds) {
    try {
      Long count = redisTemplate.opsForValue().increment(key);

      if (count == null) {
        return false;
      }

      if (count == 1) {
        redisTemplate.expire(key, Duration.ofSeconds(windowSeconds));
      }

      return count > maxRequests;

    } catch (Exception e) {
      return false;
    }
  }
}
