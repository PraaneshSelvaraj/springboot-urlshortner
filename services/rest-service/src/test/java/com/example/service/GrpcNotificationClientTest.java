package com.example.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.example.grpc.notification.*;
import io.grpc.ManagedChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("GrpcNotificationClient Tests")
class GrpcNotificationClientTest {

  @Mock private ManagedChannel channel;

  @Mock private NotificationServiceGrpc.NotificationServiceBlockingStub stub;

  @InjectMocks private GrpcNotificationClient grpcNotificationClient;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(grpcNotificationClient, "host", "localhost");
    ReflectionTestUtils.setField(grpcNotificationClient, "port", 9090);
    ReflectionTestUtils.setField(grpcNotificationClient, "channel", channel);
    ReflectionTestUtils.setField(grpcNotificationClient, "stub", stub);
  }

  @Test
  @DisplayName("Should send URL created notification successfully")
  void shouldSendUrlCreatedNotificationSuccessfully() {
    String shortCode = "abc123";
    String longUrl = "https://www.example.com";

    grpcNotificationClient.sendUrlCreatedNotification(shortCode, longUrl);

    ArgumentCaptor<NotificationRequest> captor = ArgumentCaptor.forClass(NotificationRequest.class);
    verify(stub).notify(captor.capture());

    NotificationRequest capturedRequest = captor.getValue();
    assertThat(capturedRequest.getNotificationType()).isEqualTo(NotificationType.NEWURL);
    assertThat(capturedRequest.getShortCode()).isEqualTo(shortCode);
    assertThat(capturedRequest.getMessage()).contains(longUrl);
    assertThat(capturedRequest.getMessage()).startsWith("New URL Created:");
  }

  @Test
  @DisplayName("Should send threshold notification successfully")
  void shouldSendThresholdNotificationSuccessfully() {
    String shortCode = "abc123";

    grpcNotificationClient.sendThresholdNotification(shortCode);

    ArgumentCaptor<NotificationRequest> captor = ArgumentCaptor.forClass(NotificationRequest.class);
    verify(stub).notify(captor.capture());

    NotificationRequest capturedRequest = captor.getValue();
    assertThat(capturedRequest.getNotificationType()).isEqualTo(NotificationType.THRESHOLD);
    assertThat(capturedRequest.getShortCode()).isEqualTo(shortCode);
    assertThat(capturedRequest.getMessage()).contains(shortCode);
    assertThat(capturedRequest.getMessage()).startsWith("Threshold reached");
  }

  @Test
  @DisplayName("Should get notifications successfully")
  void shouldGetNotificationsSuccessfully() {
    int pageNo = 0;
    int pageSize = 10;

    Notification notification1 =
        Notification.newBuilder()
            .setId(1L)
            .setMessage("Test notification")
            .setShortCode("abc123")
            .setNotificationType(NotificationType.NEWURL)
            .setNotificationStatus(NotificationStatus.PENDING)
            .build();

    GetNotificationsResponse mockResponse =
        GetNotificationsResponse.newBuilder()
            .addNotifications(notification1)
            .setPageNo(pageNo)
            .setPageSize(pageSize)
            .setTotalPages(1)
            .setTotalElements(1L)
            .build();

    when(stub.getNotifications(any(GetNotificationsRequest.class))).thenReturn(mockResponse);

    GetNotificationsResponse result = grpcNotificationClient.getNotifications(pageNo, pageSize);

    assertThat(result).isNotNull();
    assertThat(result.getNotificationsList()).hasSize(1);
    assertThat(result.getPageNo()).isEqualTo(pageNo);
    assertThat(result.getPageSize()).isEqualTo(pageSize);
    assertThat(result.getTotalPages()).isEqualTo(1);
    assertThat(result.getTotalElements()).isEqualTo(1L);

    ArgumentCaptor<GetNotificationsRequest> captor =
        ArgumentCaptor.forClass(GetNotificationsRequest.class);
    verify(stub).getNotifications(captor.capture());

    GetNotificationsRequest capturedRequest = captor.getValue();
    assertThat(capturedRequest.getPageNo()).isEqualTo(pageNo);
    assertThat(capturedRequest.getPageSize()).isEqualTo(pageSize);
  }

  @Test
  @DisplayName("Should handle exception when sending URL created notification")
  void shouldHandleExceptionWhenSendingUrlCreatedNotification() {
    String shortCode = "abc123";
    String longUrl = "https://www.example.com";

    when(stub.notify(any(NotificationRequest.class))).thenThrow(new RuntimeException("gRPC error"));

    grpcNotificationClient.sendUrlCreatedNotification(shortCode, longUrl);

    verify(stub).notify(any(NotificationRequest.class));
  }

  @Test
  @DisplayName("Should handle exception when sending threshold notification")
  void shouldHandleExceptionWhenSendingThresholdNotification() {
    String shortCode = "abc123";

    when(stub.notify(any(NotificationRequest.class))).thenThrow(new RuntimeException("gRPC error"));

    grpcNotificationClient.sendThresholdNotification(shortCode);

    verify(stub).notify(any(NotificationRequest.class));
  }

  @Test
  @DisplayName("Should return default instance when getting notifications fails")
  void shouldReturnDefaultInstanceWhenGettingNotificationsFails() {
    int pageNo = 0;
    int pageSize = 10;

    when(stub.getNotifications(any(GetNotificationsRequest.class)))
        .thenThrow(new RuntimeException("gRPC error"));

    GetNotificationsResponse result = grpcNotificationClient.getNotifications(pageNo, pageSize);

    assertThat(result).isNotNull();
    assertThat(result).isEqualTo(GetNotificationsResponse.getDefaultInstance());
    assertThat(result.getNotificationsList()).isEmpty();

    verify(stub).getNotifications(any(GetNotificationsRequest.class));
  }

  @Test
  @DisplayName("Should shutdown channel when channel is not shutdown")
  void shouldShutdownChannelWhenChannelIsNotShutdown() {
    when(channel.isShutdown()).thenReturn(false);

    grpcNotificationClient.shutdown();

    verify(channel).isShutdown();
    verify(channel).shutdown();
  }

  @Test
  @DisplayName("Should not shutdown channel when channel is already shutdown")
  void shouldNotShutdownChannelWhenChannelIsAlreadyShutdown() {
    when(channel.isShutdown()).thenReturn(true);

    grpcNotificationClient.shutdown();

    verify(channel).isShutdown();
    verify(channel, never()).shutdown();
  }

  @Test
  @DisplayName("Should not shutdown when channel is null")
  void shouldNotShutdownWhenChannelIsNull() {
    ReflectionTestUtils.setField(grpcNotificationClient, "channel", null);

    grpcNotificationClient.shutdown();

    verify(channel, never()).isShutdown();
    verify(channel, never()).shutdown();
  }

  @Test
  @DisplayName("Should send correct notification type for URL created")
  void shouldSendCorrectNotificationTypeForUrlCreated() {
    String shortCode = "test123";
    String longUrl = "https://test.com";

    grpcNotificationClient.sendUrlCreatedNotification(shortCode, longUrl);

    ArgumentCaptor<NotificationRequest> captor = ArgumentCaptor.forClass(NotificationRequest.class);
    verify(stub).notify(captor.capture());

    NotificationRequest request = captor.getValue();
    assertThat(request.getNotificationType()).isEqualTo(NotificationType.NEWURL);
    assertThat(request.getNotificationType()).isNotEqualTo(NotificationType.THRESHOLD);
  }

  @Test
  @DisplayName("Should send correct notification type for threshold")
  void shouldSendCorrectNotificationTypeForThreshold() {
    String shortCode = "test123";

    grpcNotificationClient.sendThresholdNotification(shortCode);

    ArgumentCaptor<NotificationRequest> captor = ArgumentCaptor.forClass(NotificationRequest.class);
    verify(stub).notify(captor.capture());

    NotificationRequest request = captor.getValue();
    assertThat(request.getNotificationType()).isEqualTo(NotificationType.THRESHOLD);
    assertThat(request.getNotificationType()).isNotEqualTo(NotificationType.NEWURL);
  }

  @Test
  @DisplayName("Should handle empty notifications response")
  void shouldHandleEmptyNotificationsResponse() {
    int pageNo = 0;
    int pageSize = 10;

    GetNotificationsResponse mockResponse =
        GetNotificationsResponse.newBuilder()
            .setPageNo(pageNo)
            .setPageSize(pageSize)
            .setTotalPages(0)
            .setTotalElements(0L)
            .build();

    when(stub.getNotifications(any(GetNotificationsRequest.class))).thenReturn(mockResponse);

    GetNotificationsResponse result = grpcNotificationClient.getNotifications(pageNo, pageSize);

    assertThat(result).isNotNull();
    assertThat(result.getNotificationsList()).isEmpty();
    assertThat(result.getTotalElements()).isEqualTo(0L);
  }

  @Test
  @DisplayName("Should handle multiple notifications in response")
  void shouldHandleMultipleNotificationsInResponse() {
    int pageNo = 0;
    int pageSize = 10;

    Notification notification1 =
        Notification.newBuilder()
            .setId(1L)
            .setMessage("First notification")
            .setShortCode("abc123")
            .setNotificationType(NotificationType.NEWURL)
            .setNotificationStatus(NotificationStatus.SUCCESS)
            .build();

    Notification notification2 =
        Notification.newBuilder()
            .setId(2L)
            .setMessage("Second notification")
            .setShortCode("xyz789")
            .setNotificationType(NotificationType.THRESHOLD)
            .setNotificationStatus(NotificationStatus.PENDING)
            .build();

    GetNotificationsResponse mockResponse =
        GetNotificationsResponse.newBuilder()
            .addNotifications(notification1)
            .addNotifications(notification2)
            .setPageNo(pageNo)
            .setPageSize(pageSize)
            .setTotalPages(1)
            .setTotalElements(2L)
            .build();

    when(stub.getNotifications(any(GetNotificationsRequest.class))).thenReturn(mockResponse);

    GetNotificationsResponse result = grpcNotificationClient.getNotifications(pageNo, pageSize);

    assertThat(result).isNotNull();
    assertThat(result.getNotificationsList()).hasSize(2);
    assertThat(result.getNotificationsList().get(0).getId()).isEqualTo(1L);
    assertThat(result.getNotificationsList().get(1).getId()).isEqualTo(2L);
  }
}
