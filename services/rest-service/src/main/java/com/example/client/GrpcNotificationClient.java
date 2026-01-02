package com.example.client;

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

  public NotificationReply notify(NotificationRequest request) {
    return stub.notify(request);
  }

  public GetNotificationsResponse getNotifications(GetNotificationsRequest request) {
    return stub.getNotifications(request);
  }
}
