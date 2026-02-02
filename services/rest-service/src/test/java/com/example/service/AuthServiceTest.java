package com.example.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.example.client.GrpcUserClient;
import com.example.dto.LoginResponseDto;
import com.example.dto.LogoutResponseDto;
import com.example.dto.RefreshTokensResponseDto;
import com.example.exception.AuthenticationException;
import com.example.grpc.user.*;
import io.grpc.Status;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("AuthService Tests")
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

  @Mock private GrpcUserClient userClient;

  @Mock private TokenBlacklistService tokenBlacklistService;

  @InjectMocks private AuthService authService;

  @Test
  @DisplayName("Should login user successfully")
  void shouldLoginUserSuccessfully() {
    String email = "test@example.com";
    String password = "password123";

    LoginResponse grpcResponse =
        LoginResponse.newBuilder()
            .setMessage("Login successful")
            .setAccessToken("access.token.here")
            .setRefreshToken("refresh.token.here")
            .build();

    when(userClient.userLogin(any(LoginRequest.class))).thenReturn(grpcResponse);

    LoginResponseDto result = authService.userLogin(email, password);
    assertThat(result).isNotNull();
    assertThat(result.getMessage()).isEqualTo("Login successful");
    assertThat(result.getAccessToken()).isEqualTo("access.token.here");
    assertThat(result.getRefreshToken()).isEqualTo("refresh.token.here");

    verify(userClient).userLogin(any(LoginRequest.class));
  }

  @Test
  @DisplayName("Should throw exception when email is empty")
  void shouldThrowExceptionWhenEmailIsEmpty() {
    assertThatThrownBy(() -> authService.userLogin("", "password"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Email should not be empty");

    verify(userClient, never()).userLogin(any());
  }

  @Test
  @DisplayName("Should throw exception when password is empty")
  void shouldThrowExceptionWhenPasswordIsEmpty() {
    assertThatThrownBy(() -> authService.userLogin("test@example.com", ""))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Password should not be empty");

    verify(userClient, never()).userLogin(any());
  }

  @Test
  @DisplayName("Should handle UNAUTHENTICATED gRPC exception")
  void shouldHandleUnauthenticatedGrpcException() {
    when(userClient.userLogin(any(LoginRequest.class)))
        .thenThrow(
            Status.UNAUTHENTICATED.withDescription("Invalid credentials").asRuntimeException());

    assertThatThrownBy(() -> authService.userLogin("test@example.com", "wrongpass"))
        .isInstanceOf(AuthenticationException.class)
        .hasMessage("Invalid credentials");
  }

  @Test
  @DisplayName("Should handle NOT_FOUND gRPC exception")
  void shouldHandleNotFoundGrpcException() {
    when(userClient.userLogin(any(LoginRequest.class)))
        .thenThrow(Status.NOT_FOUND.withDescription("User not found").asRuntimeException());

    assertThatThrownBy(() -> authService.userLogin("test@example.com", "password"))
        .isInstanceOf(NoSuchElementException.class)
        .hasMessage("User not found");
  }

  @Test
  @DisplayName("Should handle UNAVAILABLE gRPC exception")
  void shouldHandleUnavailableGrpcException() {
    when(userClient.userLogin(any(LoginRequest.class)))
        .thenThrow(Status.UNAVAILABLE.withDescription("Service unavailable").asRuntimeException());

    assertThatThrownBy(() -> authService.userLogin("test@example.com", "password"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Service unavailable");
  }

  @Test
  @DisplayName("Should handle generic gRPC exception")
  void shouldHandleGenericGrpcException() {
    when(userClient.userLogin(any(LoginRequest.class)))
        .thenThrow(Status.INTERNAL.withDescription("Internal error").asRuntimeException());

    assertThatThrownBy(() -> authService.userLogin("test@example.com", "password"))
        .isInstanceOf(RuntimeException.class);
  }

  @Test
  @DisplayName("Should login with Google successfully")
  void shouldLoginWithGoogleSuccessfully() {
    String idToken = "google.id.token.here";

    LoginResponse grpcResponse =
        LoginResponse.newBuilder()
            .setMessage("Google login successful")
            .setAccessToken("access.token.here")
            .setRefreshToken("refresh.token.here")
            .build();

    when(userClient.googleLogin(any(GoogleLoginRequest.class))).thenReturn(grpcResponse);

    LoginResponseDto result = authService.googleLogin(idToken);

    assertThat(result).isNotNull();
    assertThat(result.getMessage()).isEqualTo("Google login successful");
    assertThat(result.getAccessToken()).isEqualTo("access.token.here");
    assertThat(result.getRefreshToken()).isEqualTo("refresh.token.here");

    verify(userClient).googleLogin(any(GoogleLoginRequest.class));
  }

  @Test
  @DisplayName("Should throw exception when idToken is empty")
  void shouldThrowExceptionWhenIdTokenIsEmpty() {
    assertThatThrownBy(() -> authService.googleLogin(""))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("idToken should not be empty");

    verify(userClient, never()).googleLogin(any());
  }

  @Test
  @DisplayName("Should handle invalid Google token")
  void shouldHandleInvalidGoogleToken() {
    when(userClient.googleLogin(any(GoogleLoginRequest.class)))
        .thenThrow(
            Status.UNAUTHENTICATED.withDescription("Invalid Google token").asRuntimeException());

    assertThatThrownBy(() -> authService.googleLogin("invalid.token"))
        .isInstanceOf(AuthenticationException.class)
        .hasMessage("Invalid Google token");
  }

  @Test
  @DisplayName("Should handle Google login gRPC exception")
  void shouldHandleGoogleLoginGrpcException() {
    when(userClient.googleLogin(any(GoogleLoginRequest.class)))
        .thenThrow(Status.UNAVAILABLE.withDescription("Service unavailable").asRuntimeException());

    assertThatThrownBy(() -> authService.googleLogin("google.token"))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  @DisplayName("Should return tokens from Google login response")
  void shouldReturnTokensFromGoogleLoginResponse() {
    LoginResponse grpcResponse =
        LoginResponse.newBuilder()
            .setMessage("Success")
            .setAccessToken("access")
            .setRefreshToken("refresh")
            .build();

    when(userClient.googleLogin(any(GoogleLoginRequest.class))).thenReturn(grpcResponse);

    LoginResponseDto result = authService.googleLogin("token");

    assertThat(result.getAccessToken()).isNotEmpty();
    assertThat(result.getRefreshToken()).isNotEmpty();
  }

  @Test
  @DisplayName("Should refresh tokens successfully")
  void shouldRefreshTokensSuccessfully() {
    String refreshToken = "valid.refresh.token";

    RefreshTokenResponse grpcResponse =
        RefreshTokenResponse.newBuilder()
            .setAccessToken("new.access.token")
            .setRefreshToken("new.refresh.token")
            .build();

    when(userClient.refreshTokens(any(RefreshTokenRequest.class))).thenReturn(grpcResponse);

    RefreshTokensResponseDto result = authService.refreshTokens(refreshToken);

    assertThat(result).isNotNull();
    assertThat(result.getMessage()).isEqualTo("Tokens refreshed successfully.");
    assertThat(result.getAccessToken()).isEqualTo("new.access.token");
    assertThat(result.getRefreshToken()).isEqualTo("new.refresh.token");

    verify(userClient).refreshTokens(any(RefreshTokenRequest.class));
  }

  @Test
  @DisplayName("Should throw exception when refresh token is empty")
  void shouldThrowExceptionWhenRefreshTokenIsEmpty() {
    assertThatThrownBy(() -> authService.refreshTokens(""))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Refresh Token should not be empty");

    verify(userClient, never()).refreshTokens(any());
  }

  @Test
  @DisplayName("Should handle expired refresh token")
  void shouldHandleExpiredRefreshToken() {
    when(userClient.refreshTokens(any(RefreshTokenRequest.class)))
        .thenThrow(Status.UNAUTHENTICATED.withDescription("Token expired").asRuntimeException());

    assertThatThrownBy(() -> authService.refreshTokens("expired.token"))
        .isInstanceOf(AuthenticationException.class)
        .hasMessage("Token expired");
  }

  @Test
  @DisplayName("Should handle invalid refresh token")
  void shouldHandleInvalidRefreshToken() {
    when(userClient.refreshTokens(any(RefreshTokenRequest.class)))
        .thenThrow(Status.UNAUTHENTICATED.withDescription("Invalid token").asRuntimeException());

    assertThatThrownBy(() -> authService.refreshTokens("invalid.token"))
        .isInstanceOf(AuthenticationException.class);
  }

  @Test
  @DisplayName("Should handle refresh token gRPC exception")
  void shouldHandleRefreshTokenGrpcException() {
    when(userClient.refreshTokens(any(RefreshTokenRequest.class)))
        .thenThrow(Status.INTERNAL.withDescription("Internal error").asRuntimeException());

    assertThatThrownBy(() -> authService.refreshTokens("token"))
        .isInstanceOf(RuntimeException.class);
  }

  @Test
  @DisplayName("Should logout user successfully")
  void shouldLogoutUserSuccessfully() {
    Long userId = 123L;
    String token = "test.access.token";

    LogoutUserResponse grpcResponse =
        LogoutUserResponse.newBuilder().setMessage("Logout successful").setSuccess(true).build();

    doNothing().when(tokenBlacklistService).blacklistToken(token, userId);
    when(userClient.logoutUser(any(LogoutUserRequest.class))).thenReturn(grpcResponse);

    LogoutResponseDto result = authService.logoutUser(userId, token);

    assertThat(result).isNotNull();
    assertThat(result.getMessage()).isEqualTo("Logout successful");
    assertThat(result.isSuccess()).isTrue();

    verify(tokenBlacklistService).blacklistToken(token, userId);
    verify(userClient).logoutUser(any(LogoutUserRequest.class));
  }

  @Test
  @DisplayName("Should throw exception when user ID is zero")
  void shouldThrowExceptionWhenUserIdIsZero() {
    assertThatThrownBy(() -> authService.logoutUser(0L, "test.token"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Invalid user id");

    verify(tokenBlacklistService, never()).blacklistToken(anyString(), any());
    verify(userClient, never()).logoutUser(any());
  }

  @Test
  @DisplayName("Should throw exception when user ID is negative")
  void shouldThrowExceptionWhenUserIdIsNegative() {
    assertThatThrownBy(() -> authService.logoutUser(-1L, "test.token"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Invalid user id");

    verify(tokenBlacklistService, never()).blacklistToken(anyString(), any());
    verify(userClient, never()).logoutUser(any());
  }

  @Test
  @DisplayName("Should handle user not found during logout")
  void shouldHandleUserNotFoundDuringLogout() {
    String token = "test.token";
    doNothing().when(tokenBlacklistService).blacklistToken(token, 999L);
    when(userClient.logoutUser(any(LogoutUserRequest.class)))
        .thenThrow(Status.NOT_FOUND.withDescription("User not found").asRuntimeException());

    assertThatThrownBy(() -> authService.logoutUser(999L, token))
        .isInstanceOf(NoSuchElementException.class)
        .hasMessage("User not found");

    verify(tokenBlacklistService).blacklistToken(token, 999L);
  }

  @Test
  @DisplayName("Should handle logout gRPC exception")
  void shouldHandleLogoutGrpcException() {
    String token = "test.token";
    doNothing().when(tokenBlacklistService).blacklistToken(token, 1L);
    when(userClient.logoutUser(any(LogoutUserRequest.class)))
        .thenThrow(Status.UNAVAILABLE.withDescription("Service unavailable").asRuntimeException());

    assertThatThrownBy(() -> authService.logoutUser(1L, token))
        .isInstanceOf(IllegalStateException.class);

    verify(tokenBlacklistService).blacklistToken(token, 1L);
  }

  @Test
  @DisplayName("Should return success false on logout failure")
  void shouldReturnSuccessFalseOnLogoutFailure() {
    String token = "test.token";
    LogoutUserResponse grpcResponse =
        LogoutUserResponse.newBuilder().setMessage("Logout failed").setSuccess(false).build();

    doNothing().when(tokenBlacklistService).blacklistToken(token, 1L);
    when(userClient.logoutUser(any(LogoutUserRequest.class))).thenReturn(grpcResponse);

    LogoutResponseDto result = authService.logoutUser(1L, token);

    assertThat(result.isSuccess()).isFalse();
    assertThat(result.getMessage()).isEqualTo("Logout failed");

    verify(tokenBlacklistService).blacklistToken(token, 1L);
  }
}
