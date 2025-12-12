package com.example.controller;

import com.example.dto.PagedNotificationsDto;
import com.example.service.NotificationSerivce;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class NotificationController {

  private final NotificationSerivce notificationSerivce;

  public NotificationController(NotificationSerivce notificationSerivce) {
    this.notificationSerivce = notificationSerivce;
  }

  @GetMapping("/notifications")
  public ResponseEntity<PagedNotificationsDto> getNotifications(
      @RequestParam(defaultValue = "0") int pageNo,
      @RequestParam(defaultValue = "10") int pageSize) {

    if (pageNo < 0) {
      throw new IllegalArgumentException("Page number cannot be negative");
    }
    if (pageSize <= 0) {
      throw new IllegalArgumentException("Page size must be greater than zero.");
    }

    PagedNotificationsDto pagedNotificationsDto =
        notificationSerivce.getNotifications(pageNo, pageSize);
    return new ResponseEntity<>(pagedNotificationsDto, HttpStatus.OK);
  }
}
