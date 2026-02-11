package com.example.config;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.util.AntPathMatcher;

@Configuration
@RequiredArgsConstructor
public class RateLimitConfig {

  private final RateLimitProperties properties;

  @Bean
  public List<RateLimitRule> rateLimitRules() {
    return properties.getRules().stream()
        .map(
            config ->
                RateLimitRule.builder()
                    .method(HttpMethod.valueOf(config.getMethod()))
                    .pathPattern(config.getPathPattern())
                    .maxRequests(config.getMaxRequests())
                    .windowSeconds(config.getWindowSeconds())
                    .keyPrefix(config.getKeyPrefix())
                    .description(config.getDescription())
                    .build())
        .toList();
  }

  @Bean
  public AntPathMatcher antPathMatcher() {
    return new AntPathMatcher();
  }
}
