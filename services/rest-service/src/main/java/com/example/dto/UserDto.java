package com.example.dto;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class UserDto {
  private long id;
  private String username;
  private String email;
  private String role;
  private String authProvider;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
