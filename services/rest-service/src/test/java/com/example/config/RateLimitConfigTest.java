package com.example.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.util.AntPathMatcher;

@DisplayName("RateLimitConfig Tests")
class RateLimitConfigTest {

  private RateLimitConfig rateLimitConfig;
  private RateLimitProperties properties;

  @BeforeEach
  void setUp() {
    properties = new RateLimitProperties();

    // Create POST /api/urls rule
    RateLimitProperties.RuleConfig postRule = new RateLimitProperties.RuleConfig();
    postRule.setMethod("POST");
    postRule.setPathPattern("/api/urls");
    postRule.setMaxRequests(10);
    postRule.setWindowSeconds(60);
    postRule.setKeyPrefix("ratelimit:url:create");
    postRule.setDescription("URL creation rate limit");

    // Create DELETE /api/urls/** rule
    RateLimitProperties.RuleConfig deleteRule = new RateLimitProperties.RuleConfig();
    deleteRule.setMethod("DELETE");
    deleteRule.setPathPattern("/api/urls/**");
    deleteRule.setMaxRequests(10);
    deleteRule.setWindowSeconds(60);
    deleteRule.setKeyPrefix("ratelimit:url:delete");
    deleteRule.setDescription("URL deletion rate limit");

    properties.setRules(List.of(postRule, deleteRule));

    rateLimitConfig = new RateLimitConfig(properties);
  }

  @Test
  @DisplayName("Should create rate limit rules bean")
  void shouldCreateRateLimitRulesBean() {
    List<RateLimitRule> rules = rateLimitConfig.rateLimitRules();

    assertThat(rules).isNotNull();
    assertThat(rules).isNotEmpty();
  }

  @Test
  @DisplayName("Should contain rule for POST /api/urls")
  void shouldContainRuleForPostUrls() {
    List<RateLimitRule> rules = rateLimitConfig.rateLimitRules();

    RateLimitRule postRule =
        rules.stream()
            .filter(r -> r.getMethod().equals(HttpMethod.POST))
            .filter(r -> r.getPathPattern().equals("/api/urls"))
            .findFirst()
            .orElse(null);

    assertThat(postRule).isNotNull();
    assertThat(postRule.getMaxRequests()).isEqualTo(10);
    assertThat(postRule.getWindowSeconds()).isEqualTo(60);
    assertThat(postRule.getKeyPrefix()).isEqualTo("ratelimit:url:create");
    assertThat(postRule.getDescription()).isEqualTo("URL creation rate limit");
  }

  @Test
  @DisplayName("Should contain rule for DELETE /api/urls/**")
  void shouldContainRuleForDeleteUrls() {
    List<RateLimitRule> rules = rateLimitConfig.rateLimitRules();

    RateLimitRule deleteRule =
        rules.stream()
            .filter(r -> r.getMethod().equals(HttpMethod.DELETE))
            .filter(r -> r.getPathPattern().equals("/api/urls/**"))
            .findFirst()
            .orElse(null);

    assertThat(deleteRule).isNotNull();
    assertThat(deleteRule.getMaxRequests()).isEqualTo(10);
    assertThat(deleteRule.getWindowSeconds()).isEqualTo(60);
    assertThat(deleteRule.getKeyPrefix()).isEqualTo("ratelimit:url:delete");
    assertThat(deleteRule.getDescription()).isEqualTo("URL deletion rate limit");
  }

  @Test
  @DisplayName("Should have exactly two rules configured")
  void shouldHaveExactlyTwoRulesConfigured() {
    List<RateLimitRule> rules = rateLimitConfig.rateLimitRules();

    assertThat(rules).hasSize(2);
  }

  @Test
  @DisplayName("Should create AntPathMatcher bean")
  void shouldCreateAntPathMatcherBean() {
    AntPathMatcher pathMatcher = rateLimitConfig.antPathMatcher();

    assertThat(pathMatcher).isNotNull();
  }

  @Test
  @DisplayName("Should use consistent window seconds for all rules")
  void shouldUseConsistentWindowSecondsForAllRules() {
    List<RateLimitRule> rules = rateLimitConfig.rateLimitRules();

    assertThat(rules).allMatch(rule -> rule.getWindowSeconds() == 60);
  }

  @Test
  @DisplayName("Should use consistent max requests for all rules")
  void shouldUseConsistentMaxRequestsForAllRules() {
    List<RateLimitRule> rules = rateLimitConfig.rateLimitRules();

    assertThat(rules).allMatch(rule -> rule.getMaxRequests() == 10);
  }

  @Test
  @DisplayName("Should have unique key prefixes for each rule")
  void shouldHaveUniqueKeyPrefixesForEachRule() {
    List<RateLimitRule> rules = rateLimitConfig.rateLimitRules();

    long uniquePrefixes = rules.stream().map(RateLimitRule::getKeyPrefix).distinct().count();

    assertThat(uniquePrefixes).isEqualTo(rules.size());
  }

  @Test
  @DisplayName("Should have descriptions for all rules")
  void shouldHaveDescriptionsForAllRules() {
    List<RateLimitRule> rules = rateLimitConfig.rateLimitRules();

    assertThat(rules).allMatch(rule -> rule.getDescription() != null && !rule.getDescription().isEmpty());
  }

  @Test
  @DisplayName("AntPathMatcher should match patterns correctly")
  void antPathMatcherShouldMatchPatternsCorrectly() {
    AntPathMatcher pathMatcher = rateLimitConfig.antPathMatcher();

    assertThat(pathMatcher.match("/api/urls", "/api/urls")).isTrue();
    assertThat(pathMatcher.match("/api/urls/**", "/api/urls/abc123")).isTrue();
    assertThat(pathMatcher.match("/api/urls/**", "/api/urls/abc123/test")).isTrue();
    assertThat(pathMatcher.match("/api/urls", "/api/urls/abc123")).isFalse();
  }
}
