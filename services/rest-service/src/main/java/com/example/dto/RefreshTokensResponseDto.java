package com.example.dto;

import lombok.Data;

@Data
public class RefreshTokensResponseDto {
  private String message;
  private String accessToken;
  private String refreshToken;
}
