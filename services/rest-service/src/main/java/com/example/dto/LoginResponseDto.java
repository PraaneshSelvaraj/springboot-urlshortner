package com.example.dto;

import lombok.Data;

@Data
public class LoginResponseDto {
  private String message;
  private String accessToken;
  private String refreshToken;
}
