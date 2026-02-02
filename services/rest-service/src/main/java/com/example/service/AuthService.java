package com.example.service;

import com.example.client.GrpcUserClient;
import com.example.dto.LoginResponseDto;
import com.example.dto.LogoutResponseDto;
import com.example.dto.RefreshTokensResponseDto;
import com.example.grpc.user.*;
import com.example.util.GrpcExceptionHandler;
import io.grpc.StatusRuntimeException;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
  private final GrpcUserClient userClient;
  private final TokenBlacklistService tokenBlacklistService;

  public AuthService(GrpcUserClient userClient, TokenBlacklistService tokenBlacklistService) {
    this.userClient = userClient;
    this.tokenBlacklistService = tokenBlacklistService;
  }

  public LoginResponseDto userLogin(String email, String password) {
    try {
      if (email.isEmpty()) {
        throw new IllegalArgumentException("Email should not be empty");
      }

      if (password.isEmpty()) {
        throw new IllegalArgumentException("Password should not be empty");
      }

      LoginRequest request =
          LoginRequest.newBuilder().setEmail(email).setPassword(password).build();
      LoginResponse response = userClient.userLogin(request);

      LoginResponseDto loginResponseDto = new LoginResponseDto();
      loginResponseDto.setMessage(response.getMessage());
      loginResponseDto.setAccessToken(response.getAccessToken());
      loginResponseDto.setRefreshToken(response.getRefreshToken());

      return loginResponseDto;
    } catch (StatusRuntimeException e) {
      throw GrpcExceptionHandler.handleGrpcException(e, "Failed to login");
    }
  }

  public LoginResponseDto googleLogin(String idToken) {
    try {
      if (idToken.isEmpty()) {
        throw new IllegalArgumentException("idToken should not be empty");
      }

      GoogleLoginRequest request = GoogleLoginRequest.newBuilder().setIdToken(idToken).build();
      LoginResponse response = userClient.googleLogin(request);

      LoginResponseDto loginResponseDto = new LoginResponseDto();
      loginResponseDto.setMessage(response.getMessage());
      loginResponseDto.setAccessToken(response.getAccessToken());
      loginResponseDto.setRefreshToken(response.getRefreshToken());

      return loginResponseDto;
    } catch (StatusRuntimeException e) {
      throw GrpcExceptionHandler.handleGrpcException(e, "Failed to login");
    }
  }

  public RefreshTokensResponseDto refreshTokens(String refreshToken) {
    try {
      if (refreshToken.isEmpty()) {
        throw new IllegalArgumentException("Refresh Token should not be empty");
      }

      RefreshTokenRequest request =
          RefreshTokenRequest.newBuilder().setRefreshToken(refreshToken).build();
      RefreshTokenResponse response = userClient.refreshTokens(request);

      RefreshTokensResponseDto responseDto = new RefreshTokensResponseDto();
      responseDto.setMessage("Tokens refreshed successfully.");
      responseDto.setAccessToken(response.getAccessToken());
      responseDto.setRefreshToken(response.getRefreshToken());

      return responseDto;
    } catch (StatusRuntimeException e) {
      throw GrpcExceptionHandler.handleGrpcException(e, "Failed to refresh tokens");
    }
  }

  public LogoutResponseDto logoutUser(Long userId, String token) {
    try {
      if (userId <= 0) {
        throw new IllegalArgumentException("Invalid user id");
      }

      tokenBlacklistService.blacklistToken(token, userId);

      LogoutUserRequest request = LogoutUserRequest.newBuilder().setId(userId).build();
      LogoutUserResponse response = userClient.logoutUser(request);

      LogoutResponseDto responseDto = new LogoutResponseDto();
      responseDto.setMessage(response.getMessage());
      responseDto.setSuccess(response.getSuccess());

      return responseDto;
    } catch (StatusRuntimeException e) {
      throw GrpcExceptionHandler.handleGrpcException(e, "Failed to refresh tokens");
    }
  }
}
