package com.example.dto;

import java.util.List;
import lombok.Data;

@Data
public class PagedNotificationsDto {
  private List<NotificationDto> notifications;
  private int pageNo;
  private int pageSize;
  private int totalPages;
  private long totalElements;
}
