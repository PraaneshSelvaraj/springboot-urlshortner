package com.example.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.example.dto.NotificationDto;
import com.example.dto.PagedNotificationsDto;
import com.example.grpc.notification.GetNotificationsResponse;
import com.example.grpc.notification.Notification;
import com.example.grpc.notification.NotificationStatus;
import com.example.grpc.notification.NotificationType;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationService Tests")
class NotificationServiceTest {

  @Mock private GrpcNotificationClient grpcNotificationClient;

  @InjectMocks private NotificationService notificationService;

  @Test
  @DisplayName("Should send URL created notification successfully")
  void shouldSendUrlCreatedNotification() {
    String shortCode = "abc123";
    String longUrl = "https://www.example.com";

    notificationService.sendUrlCreatedNotification(shortCode, longUrl);

    verify(grpcNotificationClient).sendUrlCreatedNotification(eq(shortCode), eq(longUrl));
  }

  @Test
  @DisplayName("Should send threshold notification successfully")
  void shouldSendThresholdNotification() {
    String shortCode = "abc123";

    notificationService.sendThresholdNotification(shortCode);

    verify(grpcNotificationClient).sendThresholdNotification(eq(shortCode));
  }

  @Test
  @DisplayName("Should get notifications with pagination successfully")
  void shouldGetNotificationsWithPagination() {
    int pageNo = 0;
    int pageSize = 10;

    Notification notification1 =
        Notification.newBuilder()
            .setId(1L)
            .setMessage("URL created")
            .setShortCode("abc123")
            .setNotificationType(NotificationType.NEWURL)
            .setNotificationStatus(NotificationStatus.PENDING)
            .build();

    Notification notification2 =
        Notification.newBuilder()
            .setId(2L)
            .setMessage("Threshold reached")
            .setShortCode("xyz789")
            .setNotificationType(NotificationType.THRESHOLD)
            .setNotificationStatus(NotificationStatus.SUCCESS)
            .build();

    GetNotificationsResponse grpcResponse =
        GetNotificationsResponse.newBuilder()
            .addAllNotifications(Arrays.asList(notification1, notification2))
            .setPageNo(pageNo)
            .setPageSize(pageSize)
            .setTotalPages(1)
            .setTotalElements(2L)
            .build();

    when(grpcNotificationClient.getNotifications(pageNo, pageSize)).thenReturn(grpcResponse);

    PagedNotificationsDto result = notificationService.getNotifications(pageNo, pageSize);

    assertThat(result).isNotNull();
    assertThat(result.getNotifications()).hasSize(2);
    assertThat(result.getPageNo()).isEqualTo(pageNo);
    assertThat(result.getPageSize()).isEqualTo(pageSize);
    assertThat(result.getTotalPages()).isEqualTo(1);
    assertThat(result.getTotalElements()).isEqualTo(2L);

    NotificationDto firstNotification = result.getNotifications().get(0);
    assertThat(firstNotification.getId()).isEqualTo(1L);
    assertThat(firstNotification.getMessage()).isEqualTo("URL created");
    assertThat(firstNotification.getShortCode()).isEqualTo("abc123");
    assertThat(firstNotification.getNotificationType()).isEqualTo("NEWURL");
    assertThat(firstNotification.getNotificationStatus()).isEqualTo("PENDING");

    NotificationDto secondNotification = result.getNotifications().get(1);
    assertThat(secondNotification.getId()).isEqualTo(2L);
    assertThat(secondNotification.getMessage()).isEqualTo("Threshold reached");
    assertThat(secondNotification.getShortCode()).isEqualTo("xyz789");
    assertThat(secondNotification.getNotificationType()).isEqualTo("THRESHOLD");
    assertThat(secondNotification.getNotificationStatus()).isEqualTo("SUCCESS");

    verify(grpcNotificationClient).getNotifications(pageNo, pageSize);
  }

  @Test
  @DisplayName("Should get empty notifications list when no notifications exist")
  void shouldGetEmptyNotificationsListWhenNoNotificationsExist() {
    int pageNo = 0;
    int pageSize = 10;

    GetNotificationsResponse grpcResponse =
        GetNotificationsResponse.newBuilder()
            .setPageNo(pageNo)
            .setPageSize(pageSize)
            .setTotalPages(0)
            .setTotalElements(0L)
            .build();

    when(grpcNotificationClient.getNotifications(pageNo, pageSize)).thenReturn(grpcResponse);

    PagedNotificationsDto result = notificationService.getNotifications(pageNo, pageSize);

    assertThat(result).isNotNull();
    assertThat(result.getNotifications()).isEmpty();
    assertThat(result.getPageNo()).isEqualTo(pageNo);
    assertThat(result.getPageSize()).isEqualTo(pageSize);
    assertThat(result.getTotalPages()).isEqualTo(0);
    assertThat(result.getTotalElements()).isEqualTo(0L);

    verify(grpcNotificationClient).getNotifications(pageNo, pageSize);
  }

  @Test
  @DisplayName("Should handle multiple pages of notifications")
  void shouldHandleMultiplePagesOfNotifications() {
    int pageNo = 2;
    int pageSize = 5;

    Notification notification =
        Notification.newBuilder()
            .setId(10L)
            .setMessage("Test notification")
            .setShortCode("test123")
            .setNotificationType(NotificationType.NEWURL)
            .setNotificationStatus(NotificationStatus.SUCCESS)
            .build();

    GetNotificationsResponse grpcResponse =
        GetNotificationsResponse.newBuilder()
            .addNotifications(notification)
            .setPageNo(pageNo)
            .setPageSize(pageSize)
            .setTotalPages(5)
            .setTotalElements(25L)
            .build();

    when(grpcNotificationClient.getNotifications(pageNo, pageSize)).thenReturn(grpcResponse);

    PagedNotificationsDto result = notificationService.getNotifications(pageNo, pageSize);

    assertThat(result).isNotNull();
    assertThat(result.getNotifications()).hasSize(1);
    assertThat(result.getPageNo()).isEqualTo(2);
    assertThat(result.getTotalPages()).isEqualTo(5);
    assertThat(result.getTotalElements()).isEqualTo(25L);

    verify(grpcNotificationClient).getNotifications(pageNo, pageSize);
  }

  @Test
  @DisplayName("Should correctly map notification types")
  void shouldCorrectlyMapNotificationTypes() {
    Notification urlCreatedNotification =
        Notification.newBuilder()
            .setId(1L)
            .setMessage("URL created")
            .setShortCode("abc123")
            .setNotificationType(NotificationType.NEWURL)
            .setNotificationStatus(NotificationStatus.PENDING)
            .build();

    Notification thresholdNotification =
        Notification.newBuilder()
            .setId(2L)
            .setMessage("Threshold reached")
            .setShortCode("xyz789")
            .setNotificationType(NotificationType.THRESHOLD)
            .setNotificationStatus(NotificationStatus.SUCCESS)
            .build();

    GetNotificationsResponse grpcResponse =
        GetNotificationsResponse.newBuilder()
            .addAllNotifications(Arrays.asList(urlCreatedNotification, thresholdNotification))
            .setPageNo(0)
            .setPageSize(10)
            .setTotalPages(1)
            .setTotalElements(2L)
            .build();

    when(grpcNotificationClient.getNotifications(anyInt(), anyInt())).thenReturn(grpcResponse);

    PagedNotificationsDto result = notificationService.getNotifications(0, 10);

    assertThat(result.getNotifications().get(0).getNotificationType()).isEqualTo("NEWURL");
    assertThat(result.getNotifications().get(1).getNotificationType()).isEqualTo("THRESHOLD");
  }

  @Test
  @DisplayName("Should correctly map notification statuses")
  void shouldCorrectlyMapNotificationStatuses() {
    Notification pendingNotification =
        Notification.newBuilder()
            .setId(1L)
            .setMessage("Pending notification")
            .setShortCode("abc123")
            .setNotificationType(NotificationType.NEWURL)
            .setNotificationStatus(NotificationStatus.PENDING)
            .build();

    Notification successNotification =
        Notification.newBuilder()
            .setId(2L)
            .setMessage("Success notification")
            .setShortCode("xyz789")
            .setNotificationType(NotificationType.NEWURL)
            .setNotificationStatus(NotificationStatus.SUCCESS)
            .build();

    Notification failedNotification =
        Notification.newBuilder()
            .setId(3L)
            .setMessage("Failed notification")
            .setShortCode("def456")
            .setNotificationType(NotificationType.NEWURL)
            .setNotificationStatus(NotificationStatus.FAILURE)
            .build();

    GetNotificationsResponse grpcResponse =
        GetNotificationsResponse.newBuilder()
            .addAllNotifications(
                Arrays.asList(pendingNotification, successNotification, failedNotification))
            .setPageNo(0)
            .setPageSize(10)
            .setTotalPages(1)
            .setTotalElements(3L)
            .build();

    when(grpcNotificationClient.getNotifications(anyInt(), anyInt())).thenReturn(grpcResponse);

    PagedNotificationsDto result = notificationService.getNotifications(0, 10);

    List<NotificationDto> notifications = result.getNotifications();
    assertThat(notifications.get(0).getNotificationStatus()).isEqualTo("PENDING");
    assertThat(notifications.get(1).getNotificationStatus()).isEqualTo("SUCCESS");
    assertThat(notifications.get(2).getNotificationStatus()).isEqualTo("FAILURE");
  }

  @Test
  @DisplayName("Should preserve all notification fields during mapping")
  void shouldPreserveAllNotificationFieldsDuringMapping() {
    Notification notification =
        Notification.newBuilder()
            .setId(123L)
            .setMessage("Complete notification message")
            .setShortCode("complete123")
            .setNotificationType(NotificationType.NEWURL)
            .setNotificationStatus(NotificationStatus.SUCCESS)
            .build();

    GetNotificationsResponse grpcResponse =
        GetNotificationsResponse.newBuilder()
            .addNotifications(notification)
            .setPageNo(0)
            .setPageSize(1)
            .setTotalPages(1)
            .setTotalElements(1L)
            .build();

    when(grpcNotificationClient.getNotifications(anyInt(), anyInt())).thenReturn(grpcResponse);

    PagedNotificationsDto result = notificationService.getNotifications(0, 1);

    NotificationDto dto = result.getNotifications().get(0);
    assertThat(dto.getId()).isEqualTo(123L);
    assertThat(dto.getMessage()).isEqualTo("Complete notification message");
    assertThat(dto.getShortCode()).isEqualTo("complete123");
    assertThat(dto.getNotificationType()).isEqualTo("NEWURL");
    assertThat(dto.getNotificationStatus()).isEqualTo("SUCCESS");
  }
}
