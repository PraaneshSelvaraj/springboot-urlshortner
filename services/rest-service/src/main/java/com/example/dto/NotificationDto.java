package com.example.dto;

import lombok.Data;

@Data
public class NotificationDto {
  private Long id;
  private String message;
  private String shortCode;
  private String notificationType;
  private String notificationStatus;
}
