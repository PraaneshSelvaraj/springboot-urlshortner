package com.example.controller;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.dto.GoogleLoginRequestDto;
import com.example.dto.LoginRequestDto;
import com.example.dto.LoginResponseDto;
import com.example.dto.LogoutResponseDto;
import com.example.dto.RefreshTokensResponseDto;
import com.example.exception.AuthenticationException;
import com.example.security.UserPrincipal;
import com.example.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AuthController Tests")
class AuthControllerTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @MockBean private AuthService authService;

  @MockBean private com.example.util.JwtUtil jwtUtil;

  @MockBean private com.example.service.TokenBlacklistService tokenBlacklistService;

  @BeforeEach
  void setUp() {
    SecurityContextHolder.clearContext();
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  @DisplayName("Should login user successfully")
  void shouldLoginUserSuccessfully() throws Exception {
    LoginRequestDto request = new LoginRequestDto();
    request.setEmail("test@example.com");
    request.setPassword("password123");

    LoginResponseDto response = new LoginResponseDto();
    response.setMessage("Login successful");
    response.setAccessToken("access.token.here");
    response.setRefreshToken("refresh.token.here");

    when(authService.userLogin("test@example.com", "password123")).thenReturn(response);

    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("Login successful"))
        .andExpect(jsonPath("$.accessToken").value("access.token.here"))
        .andExpect(jsonPath("$.refreshToken").value("refresh.token.here"));

    verify(authService).userLogin("test@example.com", "password123");
  }

  @Test
  @DisplayName("Should return 400 when email is empty")
  void shouldReturn400WhenEmailIsEmpty() throws Exception {
    // Arrange
    LoginRequestDto request = new LoginRequestDto();
    request.setEmail("");
    request.setPassword("password");

    when(authService.userLogin("", "password"))
        .thenThrow(new IllegalArgumentException("Email should not be empty"));

    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Email should not be empty"));
  }

  @Test
  @DisplayName("Should return 400 when password is empty")
  void shouldReturn400WhenPasswordIsEmpty() throws Exception {
    LoginRequestDto request = new LoginRequestDto();
    request.setEmail("test@example.com");
    request.setPassword("");

    when(authService.userLogin("test@example.com", ""))
        .thenThrow(new IllegalArgumentException("Password should not be empty"));

    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Password should not be empty"));
  }

  @Test
  @DisplayName("Should return 401 when credentials are invalid")
  void shouldReturn401WhenCredentialsAreInvalid() throws Exception {
    LoginRequestDto request = new LoginRequestDto();
    request.setEmail("test@example.com");
    request.setPassword("wrongpassword");

    when(authService.userLogin(anyString(), anyString()))
        .thenThrow(new AuthenticationException("Invalid credentials"));

    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("Invalid credentials"));
  }

  @Test
  @DisplayName("Should handle service exception during login")
  void shouldHandleServiceExceptionDuringLogin() throws Exception {
    LoginRequestDto request = new LoginRequestDto();
    request.setEmail("test@example.com");
    request.setPassword("password");

    when(authService.userLogin(anyString(), anyString()))
        .thenThrow(new RuntimeException("Service error"));

    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isInternalServerError());
  }

  @Test
  @DisplayName("Should login with Google successfully")
  void shouldLoginWithGoogleSuccessfully() throws Exception {
    GoogleLoginRequestDto request = new GoogleLoginRequestDto();
    request.setIdToken("google.id.token");

    LoginResponseDto response = new LoginResponseDto();
    response.setMessage("Google login successful");
    response.setAccessToken("access.token.here");
    response.setRefreshToken("refresh.token.here");

    when(authService.googleLogin("google.id.token")).thenReturn(response);

    mockMvc
        .perform(
            post("/api/auth/google/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("Google login successful"))
        .andExpect(jsonPath("$.accessToken").exists())
        .andExpect(jsonPath("$.refreshToken").exists());

    verify(authService).googleLogin("google.id.token");
  }

  @Test
  @DisplayName("Should return 400 when Google idToken is empty")
  void shouldReturn400WhenGoogleIdTokenIsEmpty() throws Exception {
    GoogleLoginRequestDto request = new GoogleLoginRequestDto();
    request.setIdToken("");

    when(authService.googleLogin(""))
        .thenThrow(new IllegalArgumentException("idToken should not be empty"));

    mockMvc
        .perform(
            post("/api/auth/google/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("idToken should not be empty"));
  }

  @Test
  @DisplayName("Should return 401 when Google token is invalid")
  void shouldReturn401WhenGoogleTokenIsInvalid() throws Exception {
    GoogleLoginRequestDto request = new GoogleLoginRequestDto();
    request.setIdToken("invalid.token");

    when(authService.googleLogin("invalid.token"))
        .thenThrow(new AuthenticationException("Invalid Google token"));

    mockMvc
        .perform(
            post("/api/auth/google/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("Invalid Google token"));
  }

  @Test
  @DisplayName("Should handle Google login exception")
  void shouldHandleGoogleLoginException() throws Exception {
    GoogleLoginRequestDto request = new GoogleLoginRequestDto();
    request.setIdToken("token");

    when(authService.googleLogin(anyString())).thenThrow(new RuntimeException("Service error"));

    mockMvc
        .perform(
            post("/api/auth/google/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isInternalServerError());
  }

  @Test
  @DisplayName("Should refresh tokens successfully")
  void shouldRefreshTokensSuccessfully() throws Exception {
    String authHeader = "Bearer valid.refresh.token";

    RefreshTokensResponseDto response = new RefreshTokensResponseDto();
    response.setMessage("Tokens refreshed successfully.");
    response.setAccessToken("new.access.token");
    response.setRefreshToken("new.refresh.token");

    when(authService.refreshTokens("valid.refresh.token")).thenReturn(response);

    mockMvc
        .perform(post("/api/auth/refresh").header("Authorization", authHeader))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("Tokens refreshed successfully."))
        .andExpect(jsonPath("$.accessToken").value("new.access.token"))
        .andExpect(jsonPath("$.refreshToken").value("new.refresh.token"));

    verify(authService).refreshTokens("valid.refresh.token");
  }

  @Test
  @DisplayName("Should strip Bearer prefix from refresh token")
  void shouldStripBearerPrefixFromRefreshToken() throws Exception {
    RefreshTokensResponseDto response = new RefreshTokensResponseDto();
    response.setMessage("Success");
    response.setAccessToken("access");
    response.setRefreshToken("refresh");

    when(authService.refreshTokens("token123")).thenReturn(response);

    mockMvc
        .perform(post("/api/auth/refresh").header("Authorization", "Bearer token123"))
        .andExpect(status().isOk());

    verify(authService).refreshTokens("token123");
  }

  @Test
  @DisplayName("Should return 401 when refresh token is invalid")
  void shouldReturn401WhenRefreshTokenIsInvalid() throws Exception {
    when(authService.refreshTokens(anyString()))
        .thenThrow(new AuthenticationException("Invalid token"));

    mockMvc
        .perform(post("/api/auth/refresh").header("Authorization", "Bearer invalid"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("Invalid token"));
  }

  @Test
  @DisplayName("Should return 401 when refresh token is expired")
  void shouldReturn401WhenRefreshTokenIsExpired() throws Exception {
    when(authService.refreshTokens(anyString()))
        .thenThrow(new AuthenticationException("Token expired"));

    mockMvc
        .perform(post("/api/auth/refresh").header("Authorization", "Bearer expired"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("Token expired"));
  }

  @Test
  @DisplayName("Should handle missing Authorization header")
  void shouldHandleMissingAuthorizationHeader() throws Exception {
    mockMvc.perform(post("/api/auth/refresh")).andExpect(status().isInternalServerError());
  }

  @Test
  @DisplayName("Should logout user successfully")
  void shouldLogoutUserSuccessfully() throws Exception {
    // Setup authenticated user with token in credentials
    UserPrincipal principal = new UserPrincipal(123L, "test@example.com", "USER");
    String token = "test.access.token";
    UsernamePasswordAuthenticationToken auth =
        new UsernamePasswordAuthenticationToken(
            principal, token, List.of(new SimpleGrantedAuthority("ROLE_USER")));
    SecurityContextHolder.getContext().setAuthentication(auth);

    // Mock service response
    LogoutResponseDto response = new LogoutResponseDto();
    response.setSuccess(true);
    response.setMessage("Logout successful");
    when(authService.logoutUser(123L, token)).thenReturn(response);

    // Perform request with authenticated context
    mockMvc
        .perform(post("/api/auth/logout").principal(auth))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.message").value("Logout successful"));

    verify(authService).logoutUser(123L, token);
  }

  @Test
  @DisplayName("Should handle service exception during logout")
  void shouldHandleServiceExceptionDuringLogout() throws Exception {
    // Setup authenticated user with token
    UserPrincipal principal = new UserPrincipal(123L, "test@example.com", "USER");
    String token = "test.access.token";
    UsernamePasswordAuthenticationToken auth =
        new UsernamePasswordAuthenticationToken(
            principal, token, List.of(new SimpleGrantedAuthority("ROLE_USER")));
    SecurityContextHolder.getContext().setAuthentication(auth);

    // Mock service to throw exception
    when(authService.logoutUser(123L, token)).thenThrow(new RuntimeException("Service error"));

    // Perform request
    mockMvc
        .perform(post("/api/auth/logout").principal(auth))
        .andExpect(status().isInternalServerError());

    verify(authService).logoutUser(123L, token);
  }

  @Test
  @DisplayName("Should handle user not found during logout")
  void shouldHandleUserNotFoundDuringLogout() throws Exception {
    // Setup authenticated user with token
    UserPrincipal principal = new UserPrincipal(999L, "test@example.com", "USER");
    String token = "test.access.token";
    UsernamePasswordAuthenticationToken auth =
        new UsernamePasswordAuthenticationToken(
            principal, token, List.of(new SimpleGrantedAuthority("ROLE_USER")));
    SecurityContextHolder.getContext().setAuthentication(auth);

    // Mock service to throw NoSuchElementException
    when(authService.logoutUser(999L, token))
        .thenThrow(new NoSuchElementException("User not found"));

    // Perform request
    mockMvc
        .perform(post("/api/auth/logout").principal(auth))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("User not found"));

    verify(authService).logoutUser(999L, token);
  }
}
