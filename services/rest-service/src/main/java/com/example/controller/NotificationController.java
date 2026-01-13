package com.example.controller;

import com.example.dto.PagedNotificationsDto;
import com.example.service.NotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class NotificationController {

  private final NotificationService notificationService;

  public NotificationController(NotificationService notificationService) {
    this.notificationService = notificationService;
  }

  @GetMapping("/api/notifications")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<PagedNotificationsDto> getNotifications(
      @RequestParam(defaultValue = "0") int pageNo,
      @RequestParam(defaultValue = "10") int pageSize,
      @RequestParam(required = false) String sortBy,
      @RequestParam(required = false) String sortDirection) {

    PagedNotificationsDto pagedNotificationsDto =
        notificationService.getNotifications(pageNo, pageSize, sortBy, sortDirection);
    return new ResponseEntity<>(pagedNotificationsDto, HttpStatus.OK);
  }
}
