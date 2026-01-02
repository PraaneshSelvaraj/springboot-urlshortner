package com.example.service;

import com.example.client.GrpcNotificationClient;
import com.example.dto.NotificationDto;
import com.example.dto.PagedNotificationsDto;
import com.example.grpc.notification.GetNotificationsRequest;
import com.example.grpc.notification.GetNotificationsResponse;
import com.example.grpc.notification.Notification;
import com.example.grpc.notification.NotificationRequest;
import com.example.grpc.notification.NotificationType;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {
  private final GrpcNotificationClient notificationClient;

  public NotificationService(GrpcNotificationClient notificationClient) {
    this.notificationClient = notificationClient;
  }

  public void sendUrlCreatedNotification(String shortCode, String longUrl) {
    try {
      NotificationRequest request =
          NotificationRequest.newBuilder()
              .setNotificationType(NotificationType.NEWURL)
              .setShortCode(shortCode)
              .setMessage("New URL Created: " + longUrl)
              .build();

      notificationClient.notify(request);

    } catch (Exception e) {
      System.err.println("Failed to send notification: " + e.getMessage());
    }
  }

  public void sendThresholdNotification(String shortCode) {
    try {
      NotificationRequest request =
          NotificationRequest.newBuilder()
              .setNotificationType(NotificationType.THRESHOLD)
              .setShortCode(shortCode)
              .setMessage("Threshold reached for shortcode - '" + shortCode + "'")
              .build();

      notificationClient.notify(request);

    } catch (Exception e) {
      System.err.println("Failed to send notification: " + e.getMessage());
    }
  }

  public PagedNotificationsDto getNotifications(
      int pageNo, int pageSize, String sortBy, String sortDirection) {
    if (pageNo < 0) {
      throw new IllegalArgumentException("Page number cannot be negative");
    }

    if (pageSize <= 0) {
      throw new IllegalArgumentException("Page size must be greater than zero.");
    }

    Set<String> allowedSortField = Set.of("id", "shortCode", "createdAt");
    if (sortBy != null && !allowedSortField.contains(sortBy)) {
      throw new IllegalArgumentException(
          "Invalid sortBy: '" + sortBy + "'. Allowed values: " + allowedSortField);
    }
    String validSortBy = sortBy != null ? sortBy : "id";

    if (sortDirection != null
        && !sortDirection.equalsIgnoreCase("asc")
        && !sortDirection.equalsIgnoreCase("desc")) {
      throw new IllegalArgumentException(
          "Invalid sortDirection: '" + sortDirection + "'. Allowed values: asc, desc");
    }
    String direction = sortDirection != null ? sortDirection : "desc";

    GetNotificationsRequest.Builder requestBuilder =
        GetNotificationsRequest.newBuilder().setPageNo(pageNo).setPageSize(pageSize);

    if (validSortBy != null) {
      requestBuilder.setSortBy(validSortBy);
    }
    if (direction != null) {
      requestBuilder.setSortDirection(direction);
    }

    GetNotificationsRequest request = requestBuilder.build();

    GetNotificationsResponse notificationsResponse;
    try {
      notificationsResponse = notificationClient.getNotifications(request);
    } catch (Exception e) {
      System.err.println("Failed to fetch notifications: " + e.getMessage());
      notificationsResponse = GetNotificationsResponse.getDefaultInstance();
    }

    List<NotificationDto> notifications =
        notificationsResponse.getNotificationsList().stream()
            .map(this::mapToNotificationDto)
            .collect(Collectors.toList());

    PagedNotificationsDto pagedNotificationsDto = new PagedNotificationsDto();
    pagedNotificationsDto.setNotifications(notifications);
    pagedNotificationsDto.setPageNo(notificationsResponse.getPageNo());
    pagedNotificationsDto.setPageSize(notificationsResponse.getPageSize());
    pagedNotificationsDto.setTotalPages(notificationsResponse.getTotalPages());
    pagedNotificationsDto.setTotalElements(notificationsResponse.getTotalElements());

    return pagedNotificationsDto;
  }

  private NotificationDto mapToNotificationDto(Notification notification) {
    NotificationDto notificationDto = new NotificationDto();

    notificationDto.setId(notification.getId());
    notificationDto.setMessage(notification.getMessage());
    notificationDto.setShortCode(notification.getShortCode());
    notificationDto.setNotificationType(notification.getNotificationType().toString());
    notificationDto.setNotificationStatus(notification.getNotificationStatus().toString());

    return notificationDto;
  }
}
