package com.example.util;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.security.UserPrincipal;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@DisplayName("UserContext Tests")
class UserContextTest {

  @BeforeEach
  void setUp() {
    SecurityContextHolder.clearContext();
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  @DisplayName("Should get current user when authenticated")
  void shouldGetCurrentUserWhenAuthenticated() {
    UserPrincipal principal = new UserPrincipal(1L, "test@example.com", "USER");
    UsernamePasswordAuthenticationToken auth =
        new UsernamePasswordAuthenticationToken(
            principal, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
    SecurityContextHolder.getContext().setAuthentication(auth);

    UserPrincipal result = UserContext.getCurrentUser();

    assertThat(result).isNotNull();
    assertThat(result.getUserId()).isEqualTo(1L);
    assertThat(result.getEmail()).isEqualTo("test@example.com");
    assertThat(result.getRole()).isEqualTo("USER");
  }

  @Test
  @DisplayName("Should return null when not authenticated")
  void shouldReturnNullWhenNotAuthenticated() {
    UserPrincipal result = UserContext.getCurrentUser();
    assertThat(result).isNull();
  }

  @Test
  @DisplayName("Should return null when authentication principal is not UserPrincipal")
  void shouldReturnNullWhenPrincipalIsNotUserPrincipal() {
    UsernamePasswordAuthenticationToken auth =
        new UsernamePasswordAuthenticationToken("username", null, List.of());
    SecurityContextHolder.getContext().setAuthentication(auth);
    UserPrincipal result = UserContext.getCurrentUser();

    assertThat(result).isNull();
  }

  @Test
  @DisplayName("Should check if user is authenticated")
  void shouldCheckIfUserIsAuthenticated() {
    UserPrincipal principal = new UserPrincipal(1L, "test@example.com", "USER");
    UsernamePasswordAuthenticationToken auth =
        new UsernamePasswordAuthenticationToken(
            principal, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
    SecurityContextHolder.getContext().setAuthentication(auth);

    assertThat(UserContext.isAuthenticated()).isTrue();
  }

  @Test
  @DisplayName("Should return false when not authenticated")
  void shouldReturnFalseWhenNotAuthenticated() {
    assertThat(UserContext.isAuthenticated()).isFalse();
  }

  @Test
  @DisplayName("Should get current user ID")
  void shouldGetCurrentUserId() {
    UserPrincipal principal = new UserPrincipal(123L, "test@example.com", "USER");
    setAuthentication(principal);

    Long userId = UserContext.getCurrentUserId();

    assertThat(userId).isEqualTo(123L);
  }

  @Test
  @DisplayName("Should return null user ID when not authenticated")
  void shouldReturnNullUserIdWhenNotAuthenticated() {
    Long userId = UserContext.getCurrentUserId();

    assertThat(userId).isNull();
  }

  @Test
  @DisplayName("Should get current user email")
  void shouldGetCurrentUserEmail() {
    UserPrincipal principal = new UserPrincipal(1L, "user@example.com", "USER");
    setAuthentication(principal);

    String email = UserContext.getCurrentUserEmail();

    assertThat(email).isEqualTo("user@example.com");
  }

  @Test
  @DisplayName("Should return null email when not authenticated")
  void shouldReturnNullEmailWhenNotAuthenticated() {
    String email = UserContext.getCurrentUserEmail();

    assertThat(email).isNull();
  }

  @Test
  @DisplayName("Should get current user role")
  void shouldGetCurrentUserRole() {
    UserPrincipal principal = new UserPrincipal(1L, "user@example.com", "ADMIN");
    setAuthentication(principal);

    String role = UserContext.getCurrentUserRole();

    assertThat(role).isEqualTo("ADMIN");
  }

  @Test
  @DisplayName("Should return null role when not authenticated")
  void shouldReturnNullRoleWhenNotAuthenticated() {
    String role = UserContext.getCurrentUserRole();

    assertThat(role).isNull();
  }

  @Test
  @DisplayName("Should get ADMIN role correctly")
  void shouldGetAdminRoleCorrectly() {
    UserPrincipal principal = new UserPrincipal(1L, "admin@example.com", "ADMIN");
    setAuthentication(principal);

    assertThat(UserContext.getCurrentUserRole()).isEqualTo("ADMIN");
  }

  @Test
  @DisplayName("Should get USER role correctly")
  void shouldGetUserRoleCorrectly() {
    UserPrincipal principal = new UserPrincipal(1L, "user@example.com", "USER");
    setAuthentication(principal);

    assertThat(UserContext.getCurrentUserRole()).isEqualTo("USER");
  }

  private void setAuthentication(UserPrincipal principal) {
    UsernamePasswordAuthenticationToken auth =
        new UsernamePasswordAuthenticationToken(
            principal, null, List.of(new SimpleGrantedAuthority("ROLE_" + principal.getRole())));
    SecurityContextHolder.getContext().setAuthentication(auth);
  }
}
