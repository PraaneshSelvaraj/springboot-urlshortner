package com.example.config;

import lombok.Builder;
import lombok.Value;
import org.springframework.http.HttpMethod;

@Value
@Builder
public class RateLimitRule {
  HttpMethod method;
  String pathPattern;
  int maxRequests;
  int windowSeconds;
  String keyPrefix;
  String description;
}
