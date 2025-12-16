package com.example.service;

import com.example.dto.NotificationDto;
import com.example.dto.PagedNotificationsDto;
import com.example.grpc.notification.GetNotificationsResponse;
import com.example.grpc.notification.Notification;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {
  private final GrpcNotificationClient notificationClient;

  public NotificationService(GrpcNotificationClient notificationClient) {
    this.notificationClient = notificationClient;
  }

  public void sendUrlCreatedNotification(String shortCode, String longUrl) {
    notificationClient.sendUrlCreatedNotification(shortCode, longUrl);
  }

  public void sendThresholdNotification(String shortCode) {
    notificationClient.sendThresholdNotification(shortCode);
  }

  public PagedNotificationsDto getNotifications(int pageNo, int pageSize) {
    PagedNotificationsDto pagedNotificationsDto = new PagedNotificationsDto();

    GetNotificationsResponse notificationsResponse =
        notificationClient.getNotifications(pageNo, pageSize);

    List<NotificationDto> notifications =
        notificationsResponse.getNotificationsList().stream()
            .map(this::mapToNotificationDto)
            .collect(Collectors.toList());

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
