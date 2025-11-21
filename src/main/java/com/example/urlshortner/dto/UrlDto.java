package com.example.urlshortner.dto;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class UrlDto {
  private Long id;
  private String longUrl;
  private String shortCode;
  private String shortUrl;
  private int clicks;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private LocalDateTime expiresAt;
}
