package com.example.service;

import com.example.grpc.notification.*;
import com.example.model.*;
import com.example.repository.*;
import com.google.protobuf.Timestamp;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

@Service
public class GrpcNotificationService extends NotificationServiceGrpc.NotificationServiceImplBase {

  private final NotificationRepository notificationRepo;
  private final NotificationTypeRepository notificationTypeRepo;
  private final NotificationStatusRepository notificationStatusRepo;

  public GrpcNotificationService(
      NotificationRepository notificationRepo,
      NotificationTypeRepository notificationTypeRepo,
      NotificationStatusRepository notificationStatusRepo) {
    this.notificationRepo = notificationRepo;
    this.notificationTypeRepo = notificationTypeRepo;
    this.notificationStatusRepo = notificationStatusRepo;
  }

  @Override
  public void notify(NotificationRequest req, StreamObserver<NotificationReply> responseObserver) {
    try {
      NotificationTypeModel typeModel =
          notificationTypeRepo
              .findByName(req.getNotificationType().name())
              .orElseThrow(
                  () ->
                      Status.NOT_FOUND
                          .withDescription(
                              "Notification Type not found in DB: " + req.getNotificationType())
                          .asRuntimeException());

      NotificationStatusModel statusModel =
          notificationStatusRepo
              .findByName("SUCCESS")
              .orElseThrow(
                  () ->
                      Status.FAILED_PRECONDITION
                          .withDescription(
                              "Server Error: Status 'SUCCESS' not configured in Database.")
                          .asRuntimeException());

      System.out.println(">>> Received Notification Request <<<");
      System.out.println("Type:      " + req.getNotificationType());
      System.out.println("Message:   " + req.getMessage());
      if (req.hasShortCode()) {
        System.out.println("ShortCode: " + req.getShortCode());
      }
      System.out.println("-------------------------------------");

      NotificationModel notificationModel = new NotificationModel();
      notificationModel.setMessage(req.getMessage());
      notificationModel.setType(typeModel);
      notificationModel.setStatus(statusModel);

      if (req.hasShortCode()) notificationModel.setShortCode(req.getShortCode());

      notificationRepo.save(notificationModel);

      NotificationReply reply =
          NotificationReply.newBuilder()
              .setSuccess(true)
              .setMessage("Notification added successfully ID: " + notificationModel.getId())
              .setNotificationStatus(NotificationStatus.SUCCESS)
              .build();

      responseObserver.onNext(reply);
      responseObserver.onCompleted();

    } catch (StatusRuntimeException e) {
      System.err.println("gRPC Error: " + e.getStatus().getDescription());
      responseObserver.onError(e);

    } catch (Exception e) {
      responseObserver.onError(
          Status.INTERNAL
              .withDescription("An unexpected error occurred: " + e.getMessage())
              .asRuntimeException());
    }
  }

  @Override
  public void getNotifications(
      GetNotificationsRequest request, StreamObserver<GetNotificationsResponse> responseObserver) {
    try {
      int pageNo = request.hasPageNo() ? request.getPageNo() : 0;
      int pageSize = request.hasPageSize() ? request.getPageSize() : 10;
      String sortBy = request.hasSortBy() ? request.getSortBy() : "id";
      String sortDirection = request.hasSortDirection() ? request.getSortDirection() : "DESC";

      if (pageNo < 0) {
        throw Status.INVALID_ARGUMENT
            .withDescription("Page number cannot be negative")
            .asRuntimeException();
      }

      if (pageSize <= 0) {
        throw Status.INVALID_ARGUMENT
            .withDescription("Page size must be greater than zero")
            .asRuntimeException();
      }

      Set<String> allowedSortField = Set.of("id", "shortCode", "createdAt");
      if (sortBy != null && !allowedSortField.contains(sortBy)) {
        throw Status.INVALID_ARGUMENT
            .withDescription(
                "Invalid sortBy field: '"
                    + sortBy
                    + "'. Allowed fields: "
                    + String.join(", ", allowedSortField))
            .asRuntimeException();
      }

      Sort.Direction direction =
          switch (sortDirection.toUpperCase()) {
            case "ASC" -> Sort.Direction.ASC;
            case "DESC" -> Sort.Direction.DESC;
            default ->
                throw Status.INVALID_ARGUMENT
                    .withDescription(
                        "Invalid sortDirection field: '"
                            + sortDirection
                            + "'. Allowed fields: ASC, DESC")
                    .asRuntimeException();
          };

      Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by(direction, sortBy));
      Page<NotificationModel> notificationPage;

      notificationPage = notificationRepo.findAll(pageable);

      if (pageNo > 0
          && notificationPage.getTotalPages() > 0
          && pageNo >= notificationPage.getTotalPages()) {
        throw Status.INVALID_ARGUMENT
            .withDescription(
                "Page number "
                    + pageNo
                    + " exceeds total pages ("
                    + notificationPage.getTotalPages()
                    + ")")
            .asRuntimeException();
      }

      List<Notification> grpcList =
          notificationPage.getContent().stream()
              .map(this::mapToGrpcNotification)
              .collect(Collectors.toList());

      GetNotificationsResponse response =
          GetNotificationsResponse.newBuilder()
              .addAllNotifications(grpcList)
              .setTotalPages(notificationPage.getTotalPages())
              .setTotalElements(notificationPage.getTotalElements())
              .setPageNo(pageNo)
              .setPageSize(pageSize)
              .build();

      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (StatusRuntimeException e) {
      System.err.println("gRPC Error: " + e.getStatus().getDescription());
      responseObserver.onError(e);
    } catch (Exception e) {
      responseObserver.onError(
          Status.INTERNAL
              .withDescription("An unexpected error occurred: " + e.getMessage())
              .asRuntimeException());
    }
  }

  private Notification mapToGrpcNotification(NotificationModel notification) {
    Instant instant = notification.getCreatedAt().toInstant(ZoneOffset.UTC);
    Timestamp createdAt =
        Timestamp.newBuilder()
            .setSeconds(instant.getEpochSecond())
            .setNanos(instant.getNano())
            .build();

    Notification.Builder builder =
        Notification.newBuilder()
            .setId(notification.getId())
            .setMessage(notification.getMessage())
            .setNotificationType(NotificationType.valueOf(notification.getType().getName()))
            .setNotificationStatus(NotificationStatus.valueOf(notification.getStatus().getName()))
            .setCreatedAt(createdAt);

    if (notification.getShortCode() != null) {
      builder.setShortCode(notification.getShortCode());
    }

    return builder.build();
  }
}
