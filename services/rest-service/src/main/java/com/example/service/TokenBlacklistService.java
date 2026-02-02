package com.example.service;

import com.example.exception.TokenBlacklistException;
import com.example.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class TokenBlacklistService {

  private String BLACKLIST_KEY_PREFIX = "token:blacklist:";
  private long MIN_TTL_SECONDS = 60;

  private final StringRedisTemplate redisTemplate;
  private final JwtUtil jwtUtil;
  private final ObjectMapper objectMapper;

  public TokenBlacklistService(StringRedisTemplate redisTemplate, JwtUtil jwtUtil) {
    this.redisTemplate = redisTemplate;
    this.jwtUtil = jwtUtil;
    this.objectMapper = new ObjectMapper();
  }

  public void blacklistToken(String token, Long userId) {
    try {
      String hashedToken = hashToken(token);
      String key = BLACKLIST_KEY_PREFIX + hashedToken;

      long ttl = calculateTTL(token);

      Map<String, Object> tokenData = new HashMap<>();
      tokenData.put("userId", userId);
      tokenData.put("blacklistedAt", Instant.now().toString());
      tokenData.put("reason", "user_logout");

      String value = objectMapper.writeValueAsString(tokenData);

      redisTemplate.opsForValue().set(key, value, ttl, TimeUnit.SECONDS);

    } catch (Exception e) {
      throw new TokenBlacklistException("Failed to blacklist token", e);
    }
  }

  public boolean isTokenBlacklisted(String token) {
    try {
      String hashedToken = hashToken(token);
      String key = BLACKLIST_KEY_PREFIX + hashedToken;

      Boolean exists = redisTemplate.hasKey(key);
      return Boolean.TRUE.equals(exists);

    } catch (Exception e) {
      return false;
    }
  }

  private String hashToken(String token) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
      return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("SHA-256 algorithm not available", e);
    }
  }

  private long calculateTTL(String token) {
    try {
      Claims claims = jwtUtil.getTokenClaims(token);
      Date expiration = claims.getExpiration();

      if (expiration == null) {
        return MIN_TTL_SECONDS;
      }

      long ttl = expiration.getTime() - System.currentTimeMillis();
      ttl = ttl / 1000;

      if (ttl < MIN_TTL_SECONDS) {
        return MIN_TTL_SECONDS;
      }

      return ttl;

    } catch (Exception e) {
      return MIN_TTL_SECONDS;
    }
  }
}
