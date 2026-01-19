package com.example.controller;

import com.example.dto.GoogleLoginRequestDto;
import com.example.dto.LoginRequestDto;
import com.example.dto.LoginResponseDto;
import com.example.dto.LogoutResponseDto;
import com.example.dto.RefreshTokensResponseDto;
import com.example.security.UserPrincipal;
import com.example.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
public class AuthController {
  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  @PostMapping("/api/auth/login")
  public ResponseEntity<LoginResponseDto> userLogin(@RequestBody LoginRequestDto request) {
    LoginResponseDto loginResponseDto =
        authService.userLogin(request.getEmail(), request.getPassword());
    return new ResponseEntity<>(loginResponseDto, HttpStatus.OK);
  }

  @PostMapping("/api/auth/google/login")
  public ResponseEntity<LoginResponseDto> googleLogin(@RequestBody GoogleLoginRequestDto request) {
    LoginResponseDto loginResponseDto = authService.googleLogin(request.getIdToken());
    return new ResponseEntity<>(loginResponseDto, HttpStatus.OK);
  }

  @PostMapping("/api/auth/refresh")
  public ResponseEntity<RefreshTokensResponseDto> refreshTokens(
      @RequestHeader("Authorization") String authHeader) {
    String refreshToken = authHeader.replace("Bearer ", "");
    RefreshTokensResponseDto responseDto = authService.refreshTokens(refreshToken);
    return new ResponseEntity<>(responseDto, HttpStatus.OK);
  }

  @PostMapping("/api/auth/logout")
  public ResponseEntity<LogoutResponseDto> logoutUser(
      @AuthenticationPrincipal UserPrincipal userPrincipal) {
    LogoutResponseDto responseDto = authService.logoutUser(userPrincipal.getUserId());
    return new ResponseEntity<>(responseDto, HttpStatus.OK);
  }
}
