package com.example.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.example.security.UserPrincipal;
import com.example.service.TokenBlacklistService;
import com.example.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@DisplayName("JwtAuthenticationFilter Tests")
@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

  @Mock private JwtUtil jwtUtil;

  @Mock private TokenBlacklistService tokenBlacklistService;

  @Mock private FilterChain filterChain;

  @Mock private HttpServletRequest request;

  @Mock private HttpServletResponse response;

  @InjectMocks private JwtAuthenticationFilter jwtAuthenticationFilter;

  @BeforeEach
  void setUp() {
    SecurityContextHolder.clearContext();
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  @DisplayName("Should authenticate user with valid auth token")
  void shouldAuthenticateUserWithValidAuthToken() throws ServletException, IOException {
    String token = "valid.jwt.token";
    String authHeader = "Bearer " + token;

    when(request.getHeader("Authorization")).thenReturn(authHeader);
    when(tokenBlacklistService.isTokenBlacklisted(token)).thenReturn(false);
    when(jwtUtil.validateToken(token)).thenReturn(true);
    when(jwtUtil.extractUserId(token)).thenReturn(1L);
    when(jwtUtil.extractEmail(token)).thenReturn("test@example.com");
    when(jwtUtil.extractRole(token)).thenReturn("USER");
    when(jwtUtil.extractTokenType(token)).thenReturn("auth");

    jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    assertThat(authentication).isNotNull();
    assertThat(authentication.isAuthenticated()).isTrue();
    assertThat(authentication.getPrincipal()).isInstanceOf(UserPrincipal.class);

    UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
    assertThat(principal.getUserId()).isEqualTo(1L);
    assertThat(principal.getEmail()).isEqualTo("test@example.com");
    assertThat(principal.getRole()).isEqualTo("USER");

    assertThat(authentication.getAuthorities()).anyMatch(a -> a.getAuthority().equals("ROLE_USER"));

    verify(filterChain).doFilter(request, response);
  }

  @Test
  @DisplayName("Should authenticate ADMIN user correctly")
  void shouldAuthenticateAdminUserCorrectly() throws ServletException, IOException {
    String token = "admin.jwt.token";
    when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
    when(tokenBlacklistService.isTokenBlacklisted(token)).thenReturn(false);
    when(jwtUtil.validateToken(token)).thenReturn(true);
    when(jwtUtil.extractUserId(token)).thenReturn(2L);
    when(jwtUtil.extractEmail(token)).thenReturn("admin@example.com");
    when(jwtUtil.extractRole(token)).thenReturn("ADMIN");
    when(jwtUtil.extractTokenType(token)).thenReturn("auth");

    jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    assertThat(authentication).isNotNull();

    UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
    assertThat(principal.getRole()).isEqualTo("ADMIN");

    assertThat(authentication.getAuthorities())
        .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

    verify(filterChain).doFilter(request, response);
  }

  @Test
  @DisplayName("Should add ROLE_ prefix to role without prefix")
  void shouldAddRolePrefixToRoleWithoutPrefix() throws ServletException, IOException {
    String token = "token";
    when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
    when(tokenBlacklistService.isTokenBlacklisted(token)).thenReturn(false);
    when(jwtUtil.validateToken(token)).thenReturn(true);
    when(jwtUtil.extractUserId(token)).thenReturn(1L);
    when(jwtUtil.extractEmail(token)).thenReturn("test@example.com");
    when(jwtUtil.extractRole(token)).thenReturn("USER"); // Without ROLE_ prefix
    when(jwtUtil.extractTokenType(token)).thenReturn("auth");

    jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    assertThat(authentication.getAuthorities()).anyMatch(a -> a.getAuthority().equals("ROLE_USER"));

    verify(filterChain).doFilter(request, response);
  }

  @Test
  @DisplayName("Should not add ROLE_ prefix if already present")
  void shouldNotAddRolePrefixIfAlreadyPresent() throws ServletException, IOException {
    String token = "token";
    when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
    when(tokenBlacklistService.isTokenBlacklisted(token)).thenReturn(false);
    when(jwtUtil.validateToken(token)).thenReturn(true);
    when(jwtUtil.extractUserId(token)).thenReturn(1L);
    when(jwtUtil.extractEmail(token)).thenReturn("test@example.com");
    when(jwtUtil.extractRole(token)).thenReturn("ROLE_USER");
    when(jwtUtil.extractTokenType(token)).thenReturn("auth");

    jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    assertThat(authentication.getAuthorities()).anyMatch(a -> a.getAuthority().equals("ROLE_USER"));
    assertThat(authentication.getAuthorities())
        .noneMatch(a -> a.getAuthority().equals("ROLE_ROLE_USER"));

    verify(filterChain).doFilter(request, response);
  }

  @Test
  @DisplayName("Should skip authentication when no Authorization header")
  void shouldSkipAuthenticationWhenNoAuthorizationHeader() throws ServletException, IOException {
    when(request.getHeader("Authorization")).thenReturn(null);

    jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    verify(filterChain).doFilter(request, response);
    verify(jwtUtil, never()).validateToken(any());
  }

  @Test
  @DisplayName("Should skip authentication when Authorization header does not start with Bearer")
  void shouldSkipAuthenticationWhenAuthorizationHeaderDoesNotStartWithBearer()
      throws ServletException, IOException {
    when(request.getHeader("Authorization")).thenReturn("Basic dXNlcjpwYXNz");

    jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    verify(filterChain).doFilter(request, response);
    verify(jwtUtil, never()).validateToken(any());
  }

  @Test
  @DisplayName("Should not authenticate when token is invalid")
  void shouldNotAuthenticateWhenTokenIsInvalid() throws ServletException, IOException {
    String token = "invalid.jwt.token";
    when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
    when(jwtUtil.validateToken(token)).thenReturn(false);

    jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    verify(filterChain).doFilter(request, response);
  }

  @Test
  @DisplayName("Should not authenticate when token type is not auth")
  void shouldNotAuthenticateWhenTokenTypeIsNotAuth() throws ServletException, IOException {
    String token = "refresh.jwt.token";
    when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
    when(tokenBlacklistService.isTokenBlacklisted(token)).thenReturn(false);
    when(jwtUtil.validateToken(token)).thenReturn(true);
    when(jwtUtil.extractUserId(token)).thenReturn(1L);
    when(jwtUtil.extractEmail(token)).thenReturn("test@example.com");
    when(jwtUtil.extractRole(token)).thenReturn("USER");
    when(jwtUtil.extractTokenType(token)).thenReturn("refresh");

    jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    verify(filterChain).doFilter(request, response);
  }

  @Test
  @DisplayName("Should handle exception gracefully and continue filter chain")
  void shouldHandleExceptionGracefullyAndContinueFilterChain()
      throws ServletException, IOException {
    when(request.getHeader("Authorization")).thenReturn("Bearer token");
    when(jwtUtil.validateToken(any())).thenThrow(new RuntimeException("JWT parsing error"));

    jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    verify(filterChain).doFilter(request, response);
  }

  @Test
  @DisplayName("Should handle empty Bearer token")
  void shouldHandleEmptyBearerToken() throws ServletException, IOException {
    when(request.getHeader("Authorization")).thenReturn("Bearer ");

    jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    verify(filterChain).doFilter(request, response);
  }

  @Test
  @DisplayName("Should extract token correctly after Bearer prefix")
  void shouldExtractTokenCorrectlyAfterBearerPrefix() throws ServletException, IOException {
    String token = "mytoken123";
    when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
    when(tokenBlacklistService.isTokenBlacklisted(token)).thenReturn(false);
    when(jwtUtil.validateToken(token)).thenReturn(true);
    when(jwtUtil.extractUserId(token)).thenReturn(1L);
    when(jwtUtil.extractEmail(token)).thenReturn("test@example.com");
    when(jwtUtil.extractRole(token)).thenReturn("USER");
    when(jwtUtil.extractTokenType(token)).thenReturn("auth");

    jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

    verify(jwtUtil).validateToken(token);
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
    verify(filterChain).doFilter(request, response);
  }

  @Test
  @DisplayName("Should not authenticate when token is blacklisted")
  void shouldNotAuthenticateWhenTokenIsBlacklisted() throws ServletException, IOException {
    String token = "blacklisted.jwt.token";
    when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
    when(tokenBlacklistService.isTokenBlacklisted(token)).thenReturn(true);

    jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    verify(filterChain).doFilter(request, response);
    verify(jwtUtil, never()).validateToken(any());
  }

  @Test
  @DisplayName("Should always call doFilter even when authentication fails")
  void shouldAlwaysCallDoFilterEvenWhenAuthenticationFails() throws ServletException, IOException {
    when(request.getHeader("Authorization")).thenReturn("Bearer invalidtoken");
    when(tokenBlacklistService.isTokenBlacklisted(any())).thenReturn(false);
    when(jwtUtil.validateToken(any())).thenReturn(false);

    jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
  }
}
