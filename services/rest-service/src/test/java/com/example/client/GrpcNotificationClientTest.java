package com.example.client;

import com.example.client.GrpcNotificationClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
@DisplayName("GrpcNotificationClient Tests - Infrastructure Layer")
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
  @DisplayName("Should call gRPC stub notify method with provided request")
  void shouldCallGrpcStubNotifyMethodWithProvidedRequest() {
    NotificationRequest request =
        NotificationRequest.newBuilder()
            .setNotificationType(NotificationType.NEWURL)
            .setShortCode("abc123")
            .setMessage("Test message")
            .build();

    NotificationReply mockReply =
        NotificationReply.newBuilder()
            .setSuccess(true)
            .setMessage("Notification sent")
            .setNotificationStatus(NotificationStatus.SUCCESS)
            .build();

    when(stub.notify(any(NotificationRequest.class))).thenReturn(mockReply);

    NotificationReply result = grpcNotificationClient.notify(request);

    assertThat(result).isNotNull();
    assertThat(result.getSuccess()).isTrue();
    assertThat(result.getMessage()).isEqualTo("Notification sent");

    ArgumentCaptor<NotificationRequest> captor = ArgumentCaptor.forClass(NotificationRequest.class);
    verify(stub).notify(captor.capture());

    NotificationRequest capturedRequest = captor.getValue();
    assertThat(capturedRequest.getNotificationType()).isEqualTo(NotificationType.NEWURL);
    assertThat(capturedRequest.getShortCode()).isEqualTo("abc123");
    assertThat(capturedRequest.getMessage()).isEqualTo("Test message");
  }

  @Test
  @DisplayName("Should call gRPC stub getNotifications method with provided request")
  void shouldCallGrpcStubGetNotificationsMethodWithProvidedRequest() {
    GetNotificationsRequest request =
        GetNotificationsRequest.newBuilder()
            .setPageNo(0)
            .setPageSize(10)
            .setSortBy("id")
            .setSortDirection("desc")
            .build();

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
            .setPageNo(0)
            .setPageSize(10)
            .setTotalPages(1)
            .setTotalElements(1L)
            .build();

    when(stub.getNotifications(any(GetNotificationsRequest.class))).thenReturn(mockResponse);

    GetNotificationsResponse result = grpcNotificationClient.getNotifications(request);

    assertThat(result).isNotNull();
    assertThat(result.getNotificationsList()).hasSize(1);
    assertThat(result.getPageNo()).isEqualTo(0);
    assertThat(result.getPageSize()).isEqualTo(10);
    assertThat(result.getTotalPages()).isEqualTo(1);
    assertThat(result.getTotalElements()).isEqualTo(1L);

    ArgumentCaptor<GetNotificationsRequest> captor =
        ArgumentCaptor.forClass(GetNotificationsRequest.class);
    verify(stub).getNotifications(captor.capture());

    GetNotificationsRequest capturedRequest = captor.getValue();
    assertThat(capturedRequest.getPageNo()).isEqualTo(0);
    assertThat(capturedRequest.getPageSize()).isEqualTo(10);
    assertThat(capturedRequest.getSortBy()).isEqualTo("id");
    assertThat(capturedRequest.getSortDirection()).isEqualTo("desc");
  }

  @Test
  @DisplayName("Should propagate exception when notify fails")
  void shouldPropagateExceptionWhenNotifyFails() {
    NotificationRequest request =
        NotificationRequest.newBuilder()
            .setNotificationType(NotificationType.NEWURL)
            .setShortCode("abc123")
            .setMessage("Test message")
            .build();

    when(stub.notify(any(NotificationRequest.class)))
        .thenThrow(new RuntimeException("gRPC error"));

    assertThatThrownBy(() -> grpcNotificationClient.notify(request))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("gRPC error");

    verify(stub).notify(any(NotificationRequest.class));
  }

  @Test
  @DisplayName("Should propagate exception when getNotifications fails")
  void shouldPropagateExceptionWhenGetNotificationsFails() {
    GetNotificationsRequest request =
        GetNotificationsRequest.newBuilder().setPageNo(0).setPageSize(10).build();

    when(stub.getNotifications(any(GetNotificationsRequest.class)))
        .thenThrow(new RuntimeException("gRPC error"));

    assertThatThrownBy(() -> grpcNotificationClient.getNotifications(request))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("gRPC error");

    verify(stub).getNotifications(any(GetNotificationsRequest.class));
  }

  @Test
  @DisplayName("Should handle empty notifications response")
  void shouldHandleEmptyNotificationsResponse() {
    GetNotificationsRequest request =
        GetNotificationsRequest.newBuilder().setPageNo(0).setPageSize(10).build();

    GetNotificationsResponse mockResponse =
        GetNotificationsResponse.newBuilder()
            .setPageNo(0)
            .setPageSize(10)
            .setTotalPages(0)
            .setTotalElements(0L)
            .build();

    when(stub.getNotifications(any(GetNotificationsRequest.class))).thenReturn(mockResponse);

    GetNotificationsResponse result = grpcNotificationClient.getNotifications(request);

    assertThat(result).isNotNull();
    assertThat(result.getNotificationsList()).isEmpty();
    assertThat(result.getTotalElements()).isEqualTo(0L);
  }

  @Test
  @DisplayName("Should handle multiple notifications in response")
  void shouldHandleMultipleNotificationsInResponse() {
    GetNotificationsRequest request =
        GetNotificationsRequest.newBuilder().setPageNo(0).setPageSize(10).build();

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
            .setPageNo(0)
            .setPageSize(10)
            .setTotalPages(1)
            .setTotalElements(2L)
            .build();

    when(stub.getNotifications(any(GetNotificationsRequest.class))).thenReturn(mockResponse);

    GetNotificationsResponse result = grpcNotificationClient.getNotifications(request);

    assertThat(result).isNotNull();
    assertThat(result.getNotificationsList()).hasSize(2);
    assertThat(result.getNotificationsList().get(0).getId()).isEqualTo(1L);
    assertThat(result.getNotificationsList().get(1).getId()).isEqualTo(2L);
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
  @DisplayName("Should pass through different notification types correctly")
  void shouldPassThroughDifferentNotificationTypesCorrectly() {
    NotificationRequest newUrlRequest =
        NotificationRequest.newBuilder()
            .setNotificationType(NotificationType.NEWURL)
            .setShortCode("test123")
            .setMessage("New URL")
            .build();

    NotificationReply mockReply =
        NotificationReply.newBuilder()
            .setSuccess(true)
            .setMessage("Success")
            .setNotificationStatus(NotificationStatus.SUCCESS)
            .build();

    when(stub.notify(any(NotificationRequest.class))).thenReturn(mockReply);

    grpcNotificationClient.notify(newUrlRequest);

    ArgumentCaptor<NotificationRequest> captor = ArgumentCaptor.forClass(NotificationRequest.class);
    verify(stub).notify(captor.capture());

    NotificationRequest captured = captor.getValue();
    assertThat(captured.getNotificationType()).isEqualTo(NotificationType.NEWURL);
  }

  @Test
  @DisplayName("Should pass through threshold notification type correctly")
  void shouldPassThroughThresholdNotificationTypeCorrectly() {
    NotificationRequest thresholdRequest =
        NotificationRequest.newBuilder()
            .setNotificationType(NotificationType.THRESHOLD)
            .setShortCode("test123")
            .setMessage("Threshold reached")
            .build();

    NotificationReply mockReply =
        NotificationReply.newBuilder()
            .setSuccess(true)
            .setMessage("Success")
            .setNotificationStatus(NotificationStatus.SUCCESS)
            .build();

    when(stub.notify(any(NotificationRequest.class))).thenReturn(mockReply);

    grpcNotificationClient.notify(thresholdRequest);

    ArgumentCaptor<NotificationRequest> captor = ArgumentCaptor.forClass(NotificationRequest.class);
    verify(stub).notify(captor.capture());

    NotificationRequest captured = captor.getValue();
    assertThat(captured.getNotificationType()).isEqualTo(NotificationType.THRESHOLD);
  }
}
