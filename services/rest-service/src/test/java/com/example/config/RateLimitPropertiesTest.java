package com.example.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("RateLimitProperties Tests")
class RateLimitPropertiesTest {

  @Test
  @DisplayName("Should initialize with empty rules list")
  void shouldInitializeWithEmptyRulesList() {
    RateLimitProperties properties = new RateLimitProperties();

    assertThat(properties.getRules()).isNotNull();
    assertThat(properties.getRules()).isEmpty();
  }

  @Test
  @DisplayName("Should set and get rules")
  void shouldSetAndGetRules() {
    RateLimitProperties properties = new RateLimitProperties();

    RateLimitProperties.RuleConfig rule = new RateLimitProperties.RuleConfig();
    rule.setMethod("POST");
    rule.setPathPattern("/api/test");
    rule.setMaxRequests(5);
    rule.setWindowSeconds(30);
    rule.setKeyPrefix("test:prefix");
    rule.setDescription("Test rule");

    properties.setRules(List.of(rule));

    assertThat(properties.getRules()).hasSize(1);
    assertThat(properties.getRules().get(0).getMethod()).isEqualTo("POST");
    assertThat(properties.getRules().get(0).getPathPattern()).isEqualTo("/api/test");
    assertThat(properties.getRules().get(0).getMaxRequests()).isEqualTo(5);
    assertThat(properties.getRules().get(0).getWindowSeconds()).isEqualTo(30);
    assertThat(properties.getRules().get(0).getKeyPrefix()).isEqualTo("test:prefix");
    assertThat(properties.getRules().get(0).getDescription()).isEqualTo("Test rule");
  }

  @Test
  @DisplayName("Should support multiple rules")
  void shouldSupportMultipleRules() {
    RateLimitProperties properties = new RateLimitProperties();

    RateLimitProperties.RuleConfig rule1 = new RateLimitProperties.RuleConfig();
    rule1.setMethod("POST");
    rule1.setPathPattern("/api/test1");
    rule1.setMaxRequests(10);
    rule1.setWindowSeconds(60);

    RateLimitProperties.RuleConfig rule2 = new RateLimitProperties.RuleConfig();
    rule2.setMethod("DELETE");
    rule2.setPathPattern("/api/test2");
    rule2.setMaxRequests(5);
    rule2.setWindowSeconds(30);

    properties.setRules(List.of(rule1, rule2));

    assertThat(properties.getRules()).hasSize(2);
    assertThat(properties.getRules().get(0).getMethod()).isEqualTo("POST");
    assertThat(properties.getRules().get(1).getMethod()).isEqualTo("DELETE");
  }

  @Test
  @DisplayName("RuleConfig should have all fields settable")
  void ruleConfigShouldHaveAllFieldsSettable() {
    RateLimitProperties.RuleConfig rule = new RateLimitProperties.RuleConfig();

    rule.setMethod("PUT");
    rule.setPathPattern("/api/**");
    rule.setMaxRequests(100);
    rule.setWindowSeconds(120);
    rule.setKeyPrefix("custom:key");
    rule.setDescription("Custom description");

    assertThat(rule.getMethod()).isEqualTo("PUT");
    assertThat(rule.getPathPattern()).isEqualTo("/api/**");
    assertThat(rule.getMaxRequests()).isEqualTo(100);
    assertThat(rule.getWindowSeconds()).isEqualTo(120);
    assertThat(rule.getKeyPrefix()).isEqualTo("custom:key");
    assertThat(rule.getDescription()).isEqualTo("Custom description");
  }

  @Test
  @DisplayName("Should handle null description")
  void shouldHandleNullDescription() {
    RateLimitProperties.RuleConfig rule = new RateLimitProperties.RuleConfig();
    rule.setMethod("GET");
    rule.setPathPattern("/api/test");
    rule.setMaxRequests(10);
    rule.setWindowSeconds(60);
    rule.setKeyPrefix("test");
    rule.setDescription(null);

    assertThat(rule.getDescription()).isNull();
  }

  @Test
  @DisplayName("Should handle empty string values")
  void shouldHandleEmptyStringValues() {
    RateLimitProperties.RuleConfig rule = new RateLimitProperties.RuleConfig();
    rule.setMethod("");
    rule.setPathPattern("");
    rule.setKeyPrefix("");
    rule.setDescription("");

    assertThat(rule.getMethod()).isEmpty();
    assertThat(rule.getPathPattern()).isEmpty();
    assertThat(rule.getKeyPrefix()).isEmpty();
    assertThat(rule.getDescription()).isEmpty();
  }

  @Test
  @DisplayName("Should handle zero and negative values for max requests")
  void shouldHandleZeroAndNegativeValuesForMaxRequests() {
    RateLimitProperties.RuleConfig rule1 = new RateLimitProperties.RuleConfig();
    rule1.setMaxRequests(0);
    assertThat(rule1.getMaxRequests()).isEqualTo(0);

    RateLimitProperties.RuleConfig rule2 = new RateLimitProperties.RuleConfig();
    rule2.setMaxRequests(-1);
    assertThat(rule2.getMaxRequests()).isEqualTo(-1);
  }

  @Test
  @DisplayName("Should handle very large window seconds")
  void shouldHandleVeryLargeWindowSeconds() {
    RateLimitProperties.RuleConfig rule = new RateLimitProperties.RuleConfig();
    rule.setWindowSeconds(Integer.MAX_VALUE);

    assertThat(rule.getWindowSeconds()).isEqualTo(Integer.MAX_VALUE);
  }
}
