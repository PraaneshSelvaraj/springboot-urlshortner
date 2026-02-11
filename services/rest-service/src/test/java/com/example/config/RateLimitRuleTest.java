package com.example.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

@DisplayName("RateLimitRule Tests")
class RateLimitRuleTest {

  @Test
  @DisplayName("Should create rule with builder")
  void shouldCreateRuleWithBuilder() {
    RateLimitRule rule = RateLimitRule.builder()
        .method(HttpMethod.POST)
        .pathPattern("/api/test")
        .maxRequests(10)
        .windowSeconds(60)
        .keyPrefix("test:prefix")
        .description("Test rule")
        .build();

    assertThat(rule.getMethod()).isEqualTo(HttpMethod.POST);
    assertThat(rule.getPathPattern()).isEqualTo("/api/test");
    assertThat(rule.getMaxRequests()).isEqualTo(10);
    assertThat(rule.getWindowSeconds()).isEqualTo(60);
    assertThat(rule.getKeyPrefix()).isEqualTo("test:prefix");
    assertThat(rule.getDescription()).isEqualTo("Test rule");
  }

  @Test
  @DisplayName("Should create rule with all HTTP methods")
  void shouldCreateRuleWithAllHttpMethods() {
    HttpMethod[] methods = {HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT,
                            HttpMethod.DELETE, HttpMethod.PATCH};

    for (HttpMethod method : methods) {
      RateLimitRule rule = RateLimitRule.builder()
          .method(method)
          .pathPattern("/api/test")
          .maxRequests(10)
          .windowSeconds(60)
          .keyPrefix("test")
          .description("Test")
          .build();

      assertThat(rule.getMethod()).isEqualTo(method);
    }
  }

  @Test
  @DisplayName("Should handle wildcard path patterns")
  void shouldHandleWildcardPathPatterns() {
    RateLimitRule rule1 = RateLimitRule.builder()
        .method(HttpMethod.GET)
        .pathPattern("/api/**")
        .maxRequests(10)
        .windowSeconds(60)
        .keyPrefix("test")
        .description("Wildcard")
        .build();

    RateLimitRule rule2 = RateLimitRule.builder()
        .method(HttpMethod.POST)
        .pathPattern("/api/*/items")
        .maxRequests(5)
        .windowSeconds(30)
        .keyPrefix("test2")
        .description("Single wildcard")
        .build();

    assertThat(rule1.getPathPattern()).isEqualTo("/api/**");
    assertThat(rule2.getPathPattern()).isEqualTo("/api/*/items");
  }

  @Test
  @DisplayName("Should handle different window sizes")
  void shouldHandleDifferentWindowSizes() {
    RateLimitRule rule1 = RateLimitRule.builder()
        .method(HttpMethod.POST)
        .pathPattern("/api/test1")
        .maxRequests(10)
        .windowSeconds(30)
        .keyPrefix("test1")
        .description("30 second window")
        .build();

    RateLimitRule rule2 = RateLimitRule.builder()
        .method(HttpMethod.POST)
        .pathPattern("/api/test2")
        .maxRequests(100)
        .windowSeconds(3600)
        .keyPrefix("test2")
        .description("1 hour window")
        .build();

    assertThat(rule1.getWindowSeconds()).isEqualTo(30);
    assertThat(rule2.getWindowSeconds()).isEqualTo(3600);
  }

  @Test
  @DisplayName("Should handle different max request limits")
  void shouldHandleDifferentMaxRequestLimits() {
    RateLimitRule rule1 = RateLimitRule.builder()
        .method(HttpMethod.POST)
        .pathPattern("/api/test1")
        .maxRequests(1)
        .windowSeconds(60)
        .keyPrefix("test1")
        .description("Very restrictive")
        .build();

    RateLimitRule rule2 = RateLimitRule.builder()
        .method(HttpMethod.POST)
        .pathPattern("/api/test2")
        .maxRequests(1000)
        .windowSeconds(60)
        .keyPrefix("test2")
        .description("Very permissive")
        .build();

    assertThat(rule1.getMaxRequests()).isEqualTo(1);
    assertThat(rule2.getMaxRequests()).isEqualTo(1000);
  }

  @Test
  @DisplayName("Should have unique key prefix for different rules")
  void shouldHaveUniqueKeyPrefixForDifferentRules() {
    RateLimitRule rule1 = RateLimitRule.builder()
        .method(HttpMethod.POST)
        .pathPattern("/api/urls")
        .maxRequests(10)
        .windowSeconds(60)
        .keyPrefix("ratelimit:url:create")
        .description("Create URL")
        .build();

    RateLimitRule rule2 = RateLimitRule.builder()
        .method(HttpMethod.DELETE)
        .pathPattern("/api/urls/**")
        .maxRequests(10)
        .windowSeconds(60)
        .keyPrefix("ratelimit:url:delete")
        .description("Delete URL")
        .build();

    assertThat(rule1.getKeyPrefix()).isNotEqualTo(rule2.getKeyPrefix());
    assertThat(rule1.getKeyPrefix()).startsWith("ratelimit:");
    assertThat(rule2.getKeyPrefix()).startsWith("ratelimit:");
  }

  @Test
  @DisplayName("Should create rule with null description")
  void shouldCreateRuleWithNullDescription() {
    RateLimitRule rule = RateLimitRule.builder()
        .method(HttpMethod.GET)
        .pathPattern("/api/test")
        .maxRequests(10)
        .windowSeconds(60)
        .keyPrefix("test")
        .description(null)
        .build();

    assertThat(rule.getDescription()).isNull();
  }

  @Test
  @DisplayName("Should support equality check")
  void shouldSupportEqualityCheck() {
    RateLimitRule rule1 = RateLimitRule.builder()
        .method(HttpMethod.POST)
        .pathPattern("/api/test")
        .maxRequests(10)
        .windowSeconds(60)
        .keyPrefix("test")
        .description("Test")
        .build();

    RateLimitRule rule2 = RateLimitRule.builder()
        .method(HttpMethod.POST)
        .pathPattern("/api/test")
        .maxRequests(10)
        .windowSeconds(60)
        .keyPrefix("test")
        .description("Test")
        .build();

    assertThat(rule1).isEqualTo(rule2);
    assertThat(rule1.hashCode()).isEqualTo(rule2.hashCode());
  }

  @Test
  @DisplayName("Should detect inequality")
  void shouldDetectInequality() {
    RateLimitRule rule1 = RateLimitRule.builder()
        .method(HttpMethod.POST)
        .pathPattern("/api/test")
        .maxRequests(10)
        .windowSeconds(60)
        .keyPrefix("test1")
        .description("Test 1")
        .build();

    RateLimitRule rule2 = RateLimitRule.builder()
        .method(HttpMethod.DELETE)
        .pathPattern("/api/test")
        .maxRequests(10)
        .windowSeconds(60)
        .keyPrefix("test2")
        .description("Test 2")
        .build();

    assertThat(rule1).isNotEqualTo(rule2);
  }
}
