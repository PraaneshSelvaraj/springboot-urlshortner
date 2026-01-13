package com.example.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("UserPrincipal Tests")
class UserPrincipalTest {

  @Test
  @DisplayName("Should create UserPrincipal with all fields")
  void shouldCreateUserPrincipalWithAllFields() {
    UserPrincipal principal = new UserPrincipal(1L, "test@example.com", "USER");

    assertThat(principal.getUserId()).isEqualTo(1L);
    assertThat(principal.getEmail()).isEqualTo("test@example.com");
    assertThat(principal.getRole()).isEqualTo("USER");
  }

  @Test
  @DisplayName("Should create UserPrincipal with ADMIN role")
  void shouldCreateUserPrincipalWithAdminRole() {
    UserPrincipal principal = new UserPrincipal(2L, "admin@example.com", "ADMIN");

    assertThat(principal.getUserId()).isEqualTo(2L);
    assertThat(principal.getEmail()).isEqualTo("admin@example.com");
    assertThat(principal.getRole()).isEqualTo("ADMIN");
  }

  @Test
  @DisplayName("Should create UserPrincipal with different user IDs")
  void shouldCreateUserPrincipalWithDifferentUserIds() {
    UserPrincipal principal1 = new UserPrincipal(100L, "user1@example.com", "USER");
    UserPrincipal principal2 = new UserPrincipal(200L, "user2@example.com", "USER");

    assertThat(principal1.getUserId()).isNotEqualTo(principal2.getUserId());
    assertThat(principal1.getUserId()).isEqualTo(100L);
    assertThat(principal2.getUserId()).isEqualTo(200L);
  }

  @Test
  @DisplayName("Should be equal when all fields match")
  void shouldBeEqualWhenAllFieldsMatch() {
    UserPrincipal principal1 = new UserPrincipal(1L, "test@example.com", "USER");
    UserPrincipal principal2 = new UserPrincipal(1L, "test@example.com", "USER");

    assertThat(principal1).isEqualTo(principal2);
  }

  @Test
  @DisplayName("Should not be equal when user IDs differ")
  void shouldNotBeEqualWhenUserIdsDiffer() {
    UserPrincipal principal1 = new UserPrincipal(1L, "test@example.com", "USER");
    UserPrincipal principal2 = new UserPrincipal(2L, "test@example.com", "USER");

    assertThat(principal1).isNotEqualTo(principal2);
  }

  @Test
  @DisplayName("Should have same hashCode when equal")
  void shouldHaveSameHashCodeWhenEqual() {
    UserPrincipal principal1 = new UserPrincipal(1L, "test@example.com", "USER");
    UserPrincipal principal2 = new UserPrincipal(1L, "test@example.com", "USER");

    assertThat(principal1.hashCode()).isEqualTo(principal2.hashCode());
  }
}
