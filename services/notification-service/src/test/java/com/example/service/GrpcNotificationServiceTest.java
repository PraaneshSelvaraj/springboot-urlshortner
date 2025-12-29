package com.example.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.example.grpc.notification.*;
import com.example.model.NotificationModel;
import com.example.model.NotificationStatusModel;
import com.example.model.NotificationTypeModel;
import com.example.repository.NotificationRepository;
import com.example.repository.NotificationStatusRepository;
import com.example.repository.NotificationTypeRepository;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
@DisplayName("GrpcNotificationService Tests")
class GrpcNotificationServiceTest {

  @Mock private NotificationRepository notificationRepo;

  @Mock private NotificationTypeRepository notificationTypeRepo;

  @Mock private NotificationStatusRepository notificationStatusRepo;

  @Mock private StreamObserver<NotificationReply> notifyResponseObserver;

  @Mock private StreamObserver<GetNotificationsResponse> getNotificationsResponseObserver;

  @InjectMocks private GrpcNotificationService grpcNotificationService;

  private NotificationTypeModel testType;
  private NotificationStatusModel testStatus;

  @BeforeEach
  void setUp() {
    testType = new NotificationTypeModel();
    testType.setId(1);
    testType.setName("NEWURL");

    testStatus = new NotificationStatusModel();
    testStatus.setId(1);
    testStatus.setName("SUCCESS");
  }

  @Test
  @DisplayName("Should save notification successfully when notify is called")
  void shouldSaveNotificationSuccessfullyWhenNotifyIsCalled() {
    NotificationRequest request =
        NotificationRequest.newBuilder()
            .setNotificationType(NotificationType.NEWURL)
            .setMessage("New URL created")
            .setShortCode("abc123")
            .build();

    NotificationModel savedNotification = new NotificationModel();
    savedNotification.setId(1L);
    savedNotification.setMessage("New URL created");
    savedNotification.setShortCode("abc123");
    savedNotification.setType(testType);
    savedNotification.setStatus(testStatus);

    when(notificationTypeRepo.findByName("NEWURL")).thenReturn(Optional.of(testType));
    when(notificationStatusRepo.findByName("SUCCESS")).thenReturn(Optional.of(testStatus));
    when(notificationRepo.save(any(NotificationModel.class))).thenReturn(savedNotification);

    grpcNotificationService.notify(request, notifyResponseObserver);

    verify(notificationRepo).save(any(NotificationModel.class));
    verify(notifyResponseObserver).onNext(any(NotificationReply.class));
    verify(notifyResponseObserver).onCompleted();
    verify(notifyResponseObserver, never()).onError(any());
  }

  @Test
  @DisplayName("Should handle notification without short code")
  void shouldHandleNotificationWithoutShortCode() {
    NotificationRequest request =
        NotificationRequest.newBuilder()
            .setNotificationType(NotificationType.NEWURL)
            .setMessage("New URL created")
            .build();

    NotificationModel savedNotification = new NotificationModel();
    savedNotification.setId(1L);
    savedNotification.setMessage("New URL created");
    savedNotification.setType(testType);
    savedNotification.setStatus(testStatus);

    when(notificationTypeRepo.findByName("NEWURL")).thenReturn(Optional.of(testType));
    when(notificationStatusRepo.findByName("SUCCESS")).thenReturn(Optional.of(testStatus));
    when(notificationRepo.save(any(NotificationModel.class))).thenReturn(savedNotification);

    grpcNotificationService.notify(request, notifyResponseObserver);

    ArgumentCaptor<NotificationModel> captor = ArgumentCaptor.forClass(NotificationModel.class);
    verify(notificationRepo).save(captor.capture());

    NotificationModel captured = captor.getValue();
    assertThat(captured.getShortCode()).isNull();
    assertThat(captured.getMessage()).isEqualTo("New URL created");
  }

  @Test
  @DisplayName("Should return error when notification type not found")
  void shouldReturnErrorWhenNotificationTypeNotFound() {
    NotificationRequest request =
        NotificationRequest.newBuilder()
            .setNotificationType(NotificationType.NEWURL)
            .setMessage("Test message")
            .build();

    when(notificationTypeRepo.findByName("NEWURL")).thenReturn(Optional.empty());

    grpcNotificationService.notify(request, notifyResponseObserver);

    ArgumentCaptor<StatusRuntimeException> errorCaptor =
        ArgumentCaptor.forClass(StatusRuntimeException.class);
    verify(notifyResponseObserver).onError(errorCaptor.capture());

    StatusRuntimeException exception = errorCaptor.getValue();
    assertThat(exception.getStatus().getCode()).isEqualTo(Status.Code.NOT_FOUND);
    assertThat(exception.getStatus().getDescription())
        .contains("Notification Type not found in DB");

    verify(notificationRepo, never()).save(any());
    verify(notifyResponseObserver, never()).onNext(any());
    verify(notifyResponseObserver, never()).onCompleted();
  }

  @Test
  @DisplayName("Should return error when notification status not found")
  void shouldReturnErrorWhenNotificationStatusNotFound() {
    NotificationRequest request =
        NotificationRequest.newBuilder()
            .setNotificationType(NotificationType.THRESHOLD)
            .setMessage("Test message")
            .build();

    NotificationTypeModel thresholdType = new NotificationTypeModel();
    thresholdType.setName("THRESHOLD");

    when(notificationTypeRepo.findByName("THRESHOLD")).thenReturn(Optional.of(thresholdType));
    when(notificationStatusRepo.findByName("SUCCESS")).thenReturn(Optional.empty());

    grpcNotificationService.notify(request, notifyResponseObserver);

    ArgumentCaptor<StatusRuntimeException> errorCaptor =
        ArgumentCaptor.forClass(StatusRuntimeException.class);
    verify(notifyResponseObserver).onError(errorCaptor.capture());

    StatusRuntimeException exception = errorCaptor.getValue();
    assertThat(exception.getStatus().getCode()).isEqualTo(Status.Code.FAILED_PRECONDITION);
    assertThat(exception.getStatus().getDescription())
        .contains("Server Error: Status 'SUCCESS' not configured in Database");

    verify(notificationRepo, never()).save(any());
  }

  @Test
  @DisplayName("Should handle threshold notification type")
  void shouldHandleThresholdNotificationType() {
    NotificationTypeModel thresholdType = new NotificationTypeModel();
    thresholdType.setId(2);
    thresholdType.setName("THRESHOLD");

    NotificationRequest request =
        NotificationRequest.newBuilder()
            .setNotificationType(NotificationType.THRESHOLD)
            .setMessage("Threshold reached")
            .setShortCode("xyz789")
            .build();

    NotificationModel savedNotification = new NotificationModel();
    savedNotification.setId(2L);
    savedNotification.setMessage("Threshold reached");
    savedNotification.setShortCode("xyz789");
    savedNotification.setType(thresholdType);
    savedNotification.setStatus(testStatus);

    when(notificationTypeRepo.findByName("THRESHOLD")).thenReturn(Optional.of(thresholdType));
    when(notificationStatusRepo.findByName("SUCCESS")).thenReturn(Optional.of(testStatus));
    when(notificationRepo.save(any(NotificationModel.class))).thenReturn(savedNotification);

    grpcNotificationService.notify(request, notifyResponseObserver);

    verify(notificationRepo).save(any(NotificationModel.class));
    verify(notifyResponseObserver).onNext(any(NotificationReply.class));
    verify(notifyResponseObserver).onCompleted();
  }

  @Test
  @DisplayName("Should return success reply with notification id")
  void shouldReturnSuccessReplyWithNotificationId() {
    NotificationRequest request =
        NotificationRequest.newBuilder()
            .setNotificationType(NotificationType.NEWURL)
            .setMessage("Test message")
            .build();

    NotificationModel savedNotification = new NotificationModel();
    savedNotification.setId(123L);
    savedNotification.setType(testType);
    savedNotification.setStatus(testStatus);

    when(notificationTypeRepo.findByName("NEWURL")).thenReturn(Optional.of(testType));
    when(notificationStatusRepo.findByName("SUCCESS")).thenReturn(Optional.of(testStatus));
    when(notificationRepo.save(any(NotificationModel.class))).thenReturn(savedNotification);

    grpcNotificationService.notify(request, notifyResponseObserver);

    ArgumentCaptor<NotificationReply> replyCaptor =
        ArgumentCaptor.forClass(NotificationReply.class);
    verify(notifyResponseObserver).onNext(replyCaptor.capture());

    NotificationReply reply = replyCaptor.getValue();
    assertThat(reply.getSuccess()).isTrue();
    assertThat(reply.getMessage()).startsWith("Notification added successfully");
    assertThat(reply.getNotificationStatus()).isEqualTo(NotificationStatus.SUCCESS);
  }

  @Test
  @DisplayName("Should get notifications with pagination successfully")
  void shouldGetNotificationsWithPaginationSuccessfully() {
    NotificationModel notification1 = new NotificationModel();
    notification1.setId(1L);
    notification1.setMessage("First notification");
    notification1.setShortCode("abc123");
    notification1.setType(testType);
    notification1.setStatus(testStatus);
    notification1.setCreatedAt(LocalDateTime.now());

    NotificationModel notification2 = new NotificationModel();
    notification2.setId(2L);
    notification2.setMessage("Second notification");
    notification2.setShortCode("xyz789");
    notification2.setType(testType);
    notification2.setStatus(testStatus);
    notification2.setCreatedAt(LocalDateTime.now());

    Page<NotificationModel> page = new PageImpl<>(Arrays.asList(notification1, notification2));

    GetNotificationsRequest request =
        GetNotificationsRequest.newBuilder().setPageNo(0).setPageSize(10).build();

    when(notificationRepo.findAll(any(Pageable.class))).thenReturn(page);

    grpcNotificationService.getNotifications(request, getNotificationsResponseObserver);

    ArgumentCaptor<GetNotificationsResponse> responseCaptor =
        ArgumentCaptor.forClass(GetNotificationsResponse.class);
    verify(getNotificationsResponseObserver).onNext(responseCaptor.capture());
    verify(getNotificationsResponseObserver).onCompleted();
    verify(getNotificationsResponseObserver, never()).onError(any());

    GetNotificationsResponse response = responseCaptor.getValue();
    assertThat(response.getNotificationsList()).hasSize(2);
    assertThat(response.getPageNo()).isEqualTo(0);
    assertThat(response.getPageSize()).isEqualTo(10);
    assertThat(response.getTotalElements()).isEqualTo(2);
  }

  @Test
  @DisplayName("Should map notification model to grpc notification correctly")
  void shouldMapNotificationModelToGrpcNotificationCorrectly() {
    NotificationModel notification = new NotificationModel();
    notification.setId(1L);
    notification.setMessage("Test notification");
    notification.setShortCode("abc123");
    notification.setType(testType);
    notification.setStatus(testStatus);
    notification.setCreatedAt(LocalDateTime.now());

    Page<NotificationModel> page = new PageImpl<>(Arrays.asList(notification));

    GetNotificationsRequest request =
        GetNotificationsRequest.newBuilder().setPageNo(0).setPageSize(10).build();

    when(notificationRepo.findAll(any(Pageable.class))).thenReturn(page);

    grpcNotificationService.getNotifications(request, getNotificationsResponseObserver);

    ArgumentCaptor<GetNotificationsResponse> responseCaptor =
        ArgumentCaptor.forClass(GetNotificationsResponse.class);
    verify(getNotificationsResponseObserver).onNext(responseCaptor.capture());

    GetNotificationsResponse response = responseCaptor.getValue();
    Notification grpcNotification = response.getNotifications(0);

    assertThat(grpcNotification.getId()).isEqualTo(1L);
    assertThat(grpcNotification.getMessage()).isEqualTo("Test notification");
    assertThat(grpcNotification.getShortCode()).isEqualTo("abc123");
    assertThat(grpcNotification.getNotificationType()).isEqualTo(NotificationType.NEWURL);
    assertThat(grpcNotification.getNotificationStatus()).isEqualTo(NotificationStatus.SUCCESS);
    assertThat(grpcNotification.hasCreatedAt()).isTrue();
  }

  @Test
  @DisplayName("Should handle notification without short code when mapping to grpc")
  void shouldHandleNotificationWithoutShortCodeWhenMappingToGrpc() {
    NotificationModel notification = new NotificationModel();
    notification.setId(1L);
    notification.setMessage("Test notification");
    notification.setShortCode(null);
    notification.setType(testType);
    notification.setStatus(testStatus);
    notification.setCreatedAt(LocalDateTime.now());

    Page<NotificationModel> page = new PageImpl<>(Arrays.asList(notification));

    GetNotificationsRequest request =
        GetNotificationsRequest.newBuilder().setPageNo(0).setPageSize(10).build();

    when(notificationRepo.findAll(any(Pageable.class))).thenReturn(page);

    grpcNotificationService.getNotifications(request, getNotificationsResponseObserver);

    ArgumentCaptor<GetNotificationsResponse> responseCaptor =
        ArgumentCaptor.forClass(GetNotificationsResponse.class);
    verify(getNotificationsResponseObserver).onNext(responseCaptor.capture());

    GetNotificationsResponse response = responseCaptor.getValue();
    Notification grpcNotification = response.getNotifications(0);

    assertThat(grpcNotification.getShortCode()).isEmpty();
  }

  @Test
  @DisplayName("Should handle empty notifications page")
  void shouldHandleEmptyNotificationsPage() {
    Page<NotificationModel> emptyPage = new PageImpl<>(Arrays.asList());

    GetNotificationsRequest request =
        GetNotificationsRequest.newBuilder().setPageNo(0).setPageSize(10).build();

    when(notificationRepo.findAll(any(Pageable.class))).thenReturn(emptyPage);

    grpcNotificationService.getNotifications(request, getNotificationsResponseObserver);

    ArgumentCaptor<GetNotificationsResponse> responseCaptor =
        ArgumentCaptor.forClass(GetNotificationsResponse.class);
    verify(getNotificationsResponseObserver).onNext(responseCaptor.capture());

    GetNotificationsResponse response = responseCaptor.getValue();
    assertThat(response.getNotificationsList()).isEmpty();
    assertThat(response.getTotalElements()).isEqualTo(0);
    assertThat(response.getTotalPages()).isEqualTo(1);
  }

  @Test
  @DisplayName("Should handle exception when getting notifications")
  void shouldHandleExceptionWhenGettingNotifications() {
    GetNotificationsRequest request =
        GetNotificationsRequest.newBuilder().setPageNo(0).setPageSize(10).build();

    when(notificationRepo.findAll(any(Pageable.class)))
        .thenThrow(new RuntimeException("Database error"));

    grpcNotificationService.getNotifications(request, getNotificationsResponseObserver);

    ArgumentCaptor<StatusRuntimeException> errorCaptor =
        ArgumentCaptor.forClass(StatusRuntimeException.class);
    verify(getNotificationsResponseObserver).onError(errorCaptor.capture());

    StatusRuntimeException exception = errorCaptor.getValue();
    assertThat(exception.getStatus().getCode()).isEqualTo(Status.Code.INTERNAL);
    assertThat(exception.getStatus().getDescription()).contains("An unexpected error occurred");

    verify(getNotificationsResponseObserver, never()).onNext(any());
    verify(getNotificationsResponseObserver, never()).onCompleted();
  }

  @Test
  @DisplayName("Should handle exception when saving notification")
  void shouldHandleExceptionWhenSavingNotification() {
    NotificationRequest request =
        NotificationRequest.newBuilder()
            .setNotificationType(NotificationType.NEWURL)
            .setMessage("Test message")
            .build();

    when(notificationTypeRepo.findByName("NEWURL")).thenReturn(Optional.of(testType));
    when(notificationStatusRepo.findByName("SUCCESS")).thenReturn(Optional.of(testStatus));
    when(notificationRepo.save(any(NotificationModel.class)))
        .thenThrow(new RuntimeException("Database error"));

    grpcNotificationService.notify(request, notifyResponseObserver);

    ArgumentCaptor<StatusRuntimeException> errorCaptor =
        ArgumentCaptor.forClass(StatusRuntimeException.class);
    verify(notifyResponseObserver).onError(errorCaptor.capture());

    StatusRuntimeException exception = errorCaptor.getValue();
    assertThat(exception.getStatus().getCode()).isEqualTo(Status.Code.INTERNAL);
    assertThat(exception.getStatus().getDescription()).contains("An unexpected error occurred");
  }

  @Test
  @DisplayName("Should handle different notification types correctly")
  void shouldHandleDifferentNotificationTypesCorrectly() {
    NotificationTypeModel newUserType = new NotificationTypeModel();
    newUserType.setId(3);
    newUserType.setName("NEWUSER");

    NotificationRequest request =
        NotificationRequest.newBuilder()
            .setNotificationType(NotificationType.NEWUSER)
            .setMessage("New user registered")
            .build();

    NotificationModel savedNotification = new NotificationModel();
    savedNotification.setId(1L);
    savedNotification.setType(newUserType);
    savedNotification.setStatus(testStatus);

    when(notificationTypeRepo.findByName("NEWUSER")).thenReturn(Optional.of(newUserType));
    when(notificationStatusRepo.findByName("SUCCESS")).thenReturn(Optional.of(testStatus));
    when(notificationRepo.save(any(NotificationModel.class))).thenReturn(savedNotification);

    grpcNotificationService.notify(request, notifyResponseObserver);

    verify(notificationTypeRepo).findByName("NEWUSER");
    verify(notificationRepo).save(any(NotificationModel.class));
    verify(notifyResponseObserver).onCompleted();
  }

  @Test
  @DisplayName("Should use default values when optional parameters not provided")
  void shouldUseDefaultValuesWhenOptionalParametersNotProvided() {
    Page<NotificationModel> emptyPage = new PageImpl<>(Arrays.asList());

    GetNotificationsRequest request = GetNotificationsRequest.newBuilder().build();

    when(notificationRepo.findAll(any(Pageable.class))).thenReturn(emptyPage);

    grpcNotificationService.getNotifications(request, getNotificationsResponseObserver);

    ArgumentCaptor<GetNotificationsResponse> responseCaptor =
        ArgumentCaptor.forClass(GetNotificationsResponse.class);
    verify(getNotificationsResponseObserver).onNext(responseCaptor.capture());

    GetNotificationsResponse response = responseCaptor.getValue();
    assertThat(response.getPageNo()).isEqualTo(0);
    assertThat(response.getPageSize()).isEqualTo(10);
  }

  @Test
  @DisplayName("Should throw INVALID_ARGUMENT when page number is negative")
  void shouldThrowInvalidArgumentWhenPageNumberIsNegative() {
    GetNotificationsRequest request =
        GetNotificationsRequest.newBuilder().setPageNo(-1).setPageSize(10).build();

    grpcNotificationService.getNotifications(request, getNotificationsResponseObserver);

    ArgumentCaptor<StatusRuntimeException> errorCaptor =
        ArgumentCaptor.forClass(StatusRuntimeException.class);
    verify(getNotificationsResponseObserver).onError(errorCaptor.capture());

    StatusRuntimeException exception = errorCaptor.getValue();
    assertThat(exception.getStatus().getCode()).isEqualTo(Status.Code.INVALID_ARGUMENT);
    assertThat(exception.getStatus().getDescription())
        .contains("Page number cannot be negative");

    verify(notificationRepo, never()).findAll(any(Pageable.class));
  }

  @Test
  @DisplayName("Should throw INVALID_ARGUMENT when page size is zero")
  void shouldThrowInvalidArgumentWhenPageSizeIsZero() {
    GetNotificationsRequest request =
        GetNotificationsRequest.newBuilder().setPageNo(0).setPageSize(0).build();

    grpcNotificationService.getNotifications(request, getNotificationsResponseObserver);

    ArgumentCaptor<StatusRuntimeException> errorCaptor =
        ArgumentCaptor.forClass(StatusRuntimeException.class);
    verify(getNotificationsResponseObserver).onError(errorCaptor.capture());

    StatusRuntimeException exception = errorCaptor.getValue();
    assertThat(exception.getStatus().getCode()).isEqualTo(Status.Code.INVALID_ARGUMENT);
    assertThat(exception.getStatus().getDescription())
        .contains("Page size must be greater than zero");

    verify(notificationRepo, never()).findAll(any(Pageable.class));
  }

  @Test
  @DisplayName("Should throw INVALID_ARGUMENT when page size is negative")
  void shouldThrowInvalidArgumentWhenPageSizeIsNegative() {
    GetNotificationsRequest request =
        GetNotificationsRequest.newBuilder().setPageNo(0).setPageSize(-5).build();

    grpcNotificationService.getNotifications(request, getNotificationsResponseObserver);

    ArgumentCaptor<StatusRuntimeException> errorCaptor =
        ArgumentCaptor.forClass(StatusRuntimeException.class);
    verify(getNotificationsResponseObserver).onError(errorCaptor.capture());

    StatusRuntimeException exception = errorCaptor.getValue();
    assertThat(exception.getStatus().getCode()).isEqualTo(Status.Code.INVALID_ARGUMENT);
    assertThat(exception.getStatus().getDescription())
        .contains("Page size must be greater than zero");

    verify(notificationRepo, never()).findAll(any(Pageable.class));
  }

  @Test
  @DisplayName("Should throw INVALID_ARGUMENT when sortBy field is invalid")
  void shouldThrowInvalidArgumentWhenSortByFieldIsInvalid() {
    GetNotificationsRequest request =
        GetNotificationsRequest.newBuilder()
            .setPageNo(0)
            .setPageSize(10)
            .setSortBy("invalidField")
            .setSortDirection("desc")
            .build();

    grpcNotificationService.getNotifications(request, getNotificationsResponseObserver);

    ArgumentCaptor<StatusRuntimeException> errorCaptor =
        ArgumentCaptor.forClass(StatusRuntimeException.class);
    verify(getNotificationsResponseObserver).onError(errorCaptor.capture());

    StatusRuntimeException exception = errorCaptor.getValue();
    assertThat(exception.getStatus().getCode()).isEqualTo(Status.Code.INVALID_ARGUMENT);
    assertThat(exception.getStatus().getDescription()).contains("Invalid sortBy field");

    verify(notificationRepo, never()).findAll(any(Pageable.class));
  }

  @Test
  @DisplayName("Should get notifications sorted by id")
  void shouldGetNotificationsSortedById() {
    NotificationModel notification = new NotificationModel();
    notification.setId(1L);
    notification.setMessage("Test");
    notification.setType(testType);
    notification.setStatus(testStatus);
    notification.setCreatedAt(LocalDateTime.now());

    Page<NotificationModel> page = new PageImpl<>(Arrays.asList(notification));

    GetNotificationsRequest request =
        GetNotificationsRequest.newBuilder()
            .setPageNo(0)
            .setPageSize(10)
            .setSortBy("id")
            .setSortDirection("asc")
            .build();

    when(notificationRepo.findAll(any(Pageable.class))).thenReturn(page);

    grpcNotificationService.getNotifications(request, getNotificationsResponseObserver);

    verify(getNotificationsResponseObserver).onNext(any(GetNotificationsResponse.class));
    verify(getNotificationsResponseObserver).onCompleted();
    verify(notificationRepo).findAll(any(Pageable.class));
  }

  @Test
  @DisplayName("Should get notifications sorted by shortCode in descending order")
  void shouldGetNotificationsSortedByShortCodeDescending() {
    NotificationModel notification = new NotificationModel();
    notification.setId(1L);
    notification.setMessage("Test");
    notification.setShortCode("abc123");
    notification.setType(testType);
    notification.setStatus(testStatus);
    notification.setCreatedAt(LocalDateTime.now());

    Page<NotificationModel> page = new PageImpl<>(Arrays.asList(notification));

    GetNotificationsRequest request =
        GetNotificationsRequest.newBuilder()
            .setPageNo(0)
            .setPageSize(10)
            .setSortBy("shortCode")
            .setSortDirection("desc")
            .build();

    when(notificationRepo.findAll(any(Pageable.class))).thenReturn(page);

    grpcNotificationService.getNotifications(request, getNotificationsResponseObserver);

    verify(getNotificationsResponseObserver).onNext(any(GetNotificationsResponse.class));
    verify(getNotificationsResponseObserver).onCompleted();
    verify(notificationRepo).findAll(any(Pageable.class));
  }

  @Test
  @DisplayName("Should get notifications sorted by createdAt")
  void shouldGetNotificationsSortedByCreatedAt() {
    NotificationModel notification = new NotificationModel();
    notification.setId(1L);
    notification.setMessage("Test");
    notification.setType(testType);
    notification.setStatus(testStatus);
    notification.setCreatedAt(LocalDateTime.now());

    Page<NotificationModel> page = new PageImpl<>(Arrays.asList(notification));

    GetNotificationsRequest request =
        GetNotificationsRequest.newBuilder()
            .setPageNo(0)
            .setPageSize(10)
            .setSortBy("createdAt")
            .setSortDirection("asc")
            .build();

    when(notificationRepo.findAll(any(Pageable.class))).thenReturn(page);

    grpcNotificationService.getNotifications(request, getNotificationsResponseObserver);

    verify(getNotificationsResponseObserver).onNext(any(GetNotificationsResponse.class));
    verify(getNotificationsResponseObserver).onCompleted();
    verify(notificationRepo).findAll(any(Pageable.class));
  }
}
