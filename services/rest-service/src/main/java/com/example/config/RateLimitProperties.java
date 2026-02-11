package com.example.config;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "rate-limit")
public class RateLimitProperties {

  private List<RuleConfig> rules = new ArrayList<>();

  @Data
  public static class RuleConfig {
    private String method;
    private String pathPattern;
    private int maxRequests;
    private int windowSeconds;
    private String keyPrefix;
    private String description;
  }
}
