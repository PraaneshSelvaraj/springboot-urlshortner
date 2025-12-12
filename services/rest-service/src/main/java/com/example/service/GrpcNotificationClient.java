package com.example.service;

import com.example.grpc.notification.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GrpcNotificationClient {

  private ManagedChannel channel;
  private NotificationServiceGrpc.NotificationServiceBlockingStub stub;

  @Value("${grpc.notification.host}")
  private String host;

  @Value("${grpc.notification.port}")
  private int port;

  @PostConstruct
  public void init() {
    this.channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
    this.stub = NotificationServiceGrpc.newBlockingStub(channel);
    System.out.println("gRPC Client connected to " + host + ":" + port);
  }

  @PreDestroy
  public void shutdown() {
    if (channel != null && !channel.isShutdown()) {
      channel.shutdown();
    }
  }

  public void sendUrlCreatedNotification(String shortCode, String longUrl) {
    try {
      NotificationRequest request =
          NotificationRequest.newBuilder()
              .setNotificationType(NotificationType.NEWURL)
              .setShortCode(shortCode)
              .setMessage("New URL Created: " + longUrl)
              .build();

      stub.notify(request);

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

      stub.notify(request);

    } catch (Exception e) {
      System.err.println("Failed to send notification: " + e.getMessage());
    }
  }

  public GetNotificationsResponse getNotifications(int pageNo, int pageSize) {
    try {
      GetNotificationsRequest request =
          GetNotificationsRequest.newBuilder().setPageNo(pageNo).setPageSize(pageSize).build();

      GetNotificationsResponse response = stub.getNotifications(request);

      return response;
    } catch (Exception e) {
      System.err.println("Failed to fetch notifications: " + e.getMessage());
      return GetNotificationsResponse.getDefaultInstance();
    }
  }
}
