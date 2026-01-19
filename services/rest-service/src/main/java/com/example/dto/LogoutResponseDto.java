package com.example.dto;

import lombok.Data;

@Data
public class LogoutResponseDto {
  private boolean success = false;
  private String message;
}
