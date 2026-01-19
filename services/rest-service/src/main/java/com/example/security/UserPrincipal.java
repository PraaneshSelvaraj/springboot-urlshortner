package com.example.security;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserPrincipal {
  private final Long userId;
  private final String email;
  private final String role;
}
