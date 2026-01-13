package com.example.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.example.client.GrpcUserClient;
import com.example.dto.CreateUserDto;
import com.example.dto.PagedUsersDto;
import com.example.dto.UserDto;
import com.example.grpc.user.*;
import com.google.protobuf.Timestamp;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Tests - Business Logic Layer")
class UserServiceTest {

  @Mock private GrpcUserClient userClient;

  @Mock private NotificationService notificationService;

  @InjectMocks private UserService userService;

  // createUser Tests (8 tests)

  @Test
  @DisplayName("Should build correct CreateUserRequest for USER role")
  void shouldBuildCorrectCreateUserRequestForUserRole() {
    CreateUserDto dto = new CreateUserDto();
    dto.setUsername("testuser");
    dto.setEmail("test@example.com");
    dto.setPassword("password123");
    dto.setRole("USER");

    User mockUser =
        User.newBuilder()
            .setId(1L)
            .setUsername("testuser")
            .setEmail("test@example.com")
            .setRole(UserRole.USER)
            .setAuthProvider(AuthProvider.LOCAL)
            .setCreatedAt(Timestamp.newBuilder().setSeconds(System.currentTimeMillis() / 1000))
            .setUpdatedAt(Timestamp.newBuilder().setSeconds(System.currentTimeMillis() / 1000))
            .build();

    when(userClient.createUser(any(CreateUserRequest.class))).thenReturn(mockUser);
    doNothing().when(notificationService).sendUserCreatedNotification(anyString());

    UserDto result = userService.createUser(dto);

    ArgumentCaptor<CreateUserRequest> captor = ArgumentCaptor.forClass(CreateUserRequest.class);
    verify(userClient).createUser(captor.capture());

    CreateUserRequest capturedRequest = captor.getValue();
    assertThat(capturedRequest.getUsername()).isEqualTo("testuser");
    assertThat(capturedRequest.getEmail()).isEqualTo("test@example.com");
    assertThat(capturedRequest.getPassword()).isEqualTo("password123");
    assertThat(capturedRequest.getUserRole()).isEqualTo(UserRole.USER);

    assertThat(result).isNotNull();
    assertThat(result.getUsername()).isEqualTo("testuser");
  }

  @Test
  @DisplayName("Should build correct CreateUserRequest for ADMIN role")
  void shouldBuildCorrectCreateUserRequestForAdminRole() {
    CreateUserDto dto = new CreateUserDto();
    dto.setUsername("admin");
    dto.setEmail("admin@example.com");
    dto.setPassword("adminpass");
    dto.setRole("ADMIN");

    User mockUser =
        User.newBuilder()
            .setId(1L)
            .setUsername("admin")
            .setEmail("admin@example.com")
            .setRole(UserRole.ADMIN)
            .setAuthProvider(AuthProvider.LOCAL)
            .setCreatedAt(Timestamp.newBuilder().setSeconds(System.currentTimeMillis() / 1000))
            .setUpdatedAt(Timestamp.newBuilder().setSeconds(System.currentTimeMillis() / 1000))
            .build();

    when(userClient.createUser(any(CreateUserRequest.class))).thenReturn(mockUser);
    doNothing().when(notificationService).sendUserCreatedNotification(anyString());

    UserDto result = userService.createUser(dto);

    ArgumentCaptor<CreateUserRequest> captor = ArgumentCaptor.forClass(CreateUserRequest.class);
    verify(userClient).createUser(captor.capture());

    CreateUserRequest capturedRequest = captor.getValue();
    assertThat(capturedRequest.getUserRole()).isEqualTo(UserRole.ADMIN);

    assertThat(result.getRole()).isEqualTo("ADMIN");
  }

  @Test
  @DisplayName("Should handle case-insensitive role validation")
  void shouldHandleCaseInsensitiveRoleValidation() {
    String[] roleVariants = {"user", "USER", "User", "admin", "ADMIN", "Admin"};

    for (String roleVariant : roleVariants) {
      CreateUserDto dto = new CreateUserDto();
      dto.setUsername("testuser");
      dto.setEmail("test@example.com");
      dto.setPassword("password123");
      dto.setRole(roleVariant);

      UserRole expectedRole =
          roleVariant.toUpperCase().equals("ADMIN") ? UserRole.ADMIN : UserRole.USER;

      User mockUser =
          User.newBuilder()
              .setId(1L)
              .setUsername("testuser")
              .setEmail("test@example.com")
              .setRole(expectedRole)
              .setAuthProvider(AuthProvider.LOCAL)
              .setCreatedAt(Timestamp.newBuilder().setSeconds(System.currentTimeMillis() / 1000))
              .setUpdatedAt(Timestamp.newBuilder().setSeconds(System.currentTimeMillis() / 1000))
              .build();

      when(userClient.createUser(any(CreateUserRequest.class))).thenReturn(mockUser);
      doNothing().when(notificationService).sendUserCreatedNotification(anyString());

      userService.createUser(dto);

      ArgumentCaptor<CreateUserRequest> captor = ArgumentCaptor.forClass(CreateUserRequest.class);
      verify(userClient, atLeastOnce()).createUser(captor.capture());

      CreateUserRequest capturedRequest = captor.getValue();
      assertThat(capturedRequest.getUserRole()).isEqualTo(expectedRole);
    }
  }

  @Test
  @DisplayName("Should throw IllegalArgumentException for invalid role")
  void shouldThrowIllegalArgumentExceptionForInvalidRole() {
    CreateUserDto dto = new CreateUserDto();
    dto.setUsername("testuser");
    dto.setEmail("test@example.com");
    dto.setPassword("password123");
    dto.setRole("SUPERUSER");

    assertThatThrownBy(() -> userService.createUser(dto))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Invalid role: SUPERUSER");

    verify(userClient, never()).createUser(any(CreateUserRequest.class));
  }

  @Test
  @DisplayName("Should call notification service after user creation")
  void shouldCallNotificationServiceAfterUserCreation() {
    CreateUserDto dto = new CreateUserDto();
    dto.setUsername("testuser");
    dto.setEmail("test@example.com");
    dto.setPassword("password123");
    dto.setRole("USER");

    User mockUser =
        User.newBuilder()
            .setId(1L)
            .setUsername("testuser")
            .setEmail("test@example.com")
            .setRole(UserRole.USER)
            .setAuthProvider(AuthProvider.LOCAL)
            .setCreatedAt(Timestamp.newBuilder().setSeconds(System.currentTimeMillis() / 1000))
            .setUpdatedAt(Timestamp.newBuilder().setSeconds(System.currentTimeMillis() / 1000))
            .build();

    when(userClient.createUser(any(CreateUserRequest.class))).thenReturn(mockUser);
    doNothing().when(notificationService).sendUserCreatedNotification(anyString());

    userService.createUser(dto);

    verify(notificationService).sendUserCreatedNotification("testuser");
  }

  @Test
  @DisplayName("Should map user response to UserDto with all fields")
  void shouldMapUserResponseToUserDto() {
    CreateUserDto dto = new CreateUserDto();
    dto.setUsername("testuser");
    dto.setEmail("test@example.com");
    dto.setPassword("password123");
    dto.setRole("USER");

    long createdSeconds = 1704067200L; // 2024-01-01 00:00:00 UTC
    long updatedSeconds = 1704153600L; // 2024-01-02 00:00:00 UTC

    User mockUser =
        User.newBuilder()
            .setId(42L)
            .setUsername("testuser")
            .setEmail("test@example.com")
            .setRole(UserRole.ADMIN)
            .setAuthProvider(AuthProvider.GOOGLE)
            .setCreatedAt(Timestamp.newBuilder().setSeconds(createdSeconds))
            .setUpdatedAt(Timestamp.newBuilder().setSeconds(updatedSeconds))
            .build();

    when(userClient.createUser(any(CreateUserRequest.class))).thenReturn(mockUser);
    doNothing().when(notificationService).sendUserCreatedNotification(anyString());

    UserDto result = userService.createUser(dto);

    assertThat(result.getId()).isEqualTo(42L);
    assertThat(result.getUsername()).isEqualTo("testuser");
    assertThat(result.getEmail()).isEqualTo("test@example.com");
    assertThat(result.getRole()).isEqualTo("ADMIN");
    assertThat(result.getAuthProvider()).isEqualTo("GOOGLE");
    assertThat(result.getCreatedAt())
        .isEqualTo(LocalDateTime.ofInstant(Instant.ofEpochSecond(createdSeconds), ZoneOffset.UTC));
    assertThat(result.getUpdatedAt())
        .isEqualTo(LocalDateTime.ofInstant(Instant.ofEpochSecond(updatedSeconds), ZoneOffset.UTC));
  }

  @Test
  @DisplayName("Should handle gRPC StatusRuntimeException")
  void shouldHandleGrpcStatusRuntimeException() {
    CreateUserDto dto = new CreateUserDto();
    dto.setUsername("testuser");
    dto.setEmail("test@example.com");
    dto.setPassword("password123");
    dto.setRole("USER");

    StatusRuntimeException exception =
        new StatusRuntimeException(Status.ALREADY_EXISTS.withDescription("User already exists"));
    when(userClient.createUser(any(CreateUserRequest.class))).thenThrow(exception);

    assertThatThrownBy(() -> userService.createUser(dto))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("User already exists");

    verify(notificationService, never()).sendUserCreatedNotification(anyString());
  }

  @Test
  @DisplayName("Should propagate gRPC exception as RuntimeException")
  void shouldPropagateGrpcExceptionAsRuntimeException() {
    CreateUserDto dto = new CreateUserDto();
    dto.setUsername("testuser");
    dto.setEmail("test@example.com");
    dto.setPassword("password123");
    dto.setRole("USER");

    StatusRuntimeException exception =
        new StatusRuntimeException(Status.UNAVAILABLE.withDescription("Service unavailable"));
    when(userClient.createUser(any(CreateUserRequest.class))).thenThrow(exception);

    assertThatThrownBy(() -> userService.createUser(dto))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Service unavailable");
  }

  // getUserById Tests (4 tests)

  @Test
  @DisplayName("Should get user by ID successfully")
  void shouldGetUserByIdSuccessfully() {
    long userId = 123L;
    User mockUser =
        User.newBuilder()
            .setId(userId)
            .setUsername("testuser")
            .setEmail("test@example.com")
            .setRole(UserRole.USER)
            .setAuthProvider(AuthProvider.LOCAL)
            .setCreatedAt(Timestamp.newBuilder().setSeconds(System.currentTimeMillis() / 1000))
            .setUpdatedAt(Timestamp.newBuilder().setSeconds(System.currentTimeMillis() / 1000))
            .build();

    when(userClient.getUserById(any(GetUserByIdRequest.class))).thenReturn(mockUser);

    UserDto result = userService.getUserById(userId);

    assertThat(result).isNotNull();
    assertThat(result.getId()).isEqualTo(userId);
    assertThat(result.getUsername()).isEqualTo("testuser");
  }

  @Test
  @DisplayName("Should build correct GetUserByIdRequest")
  void shouldBuildCorrectGetUserByIdRequest() {
    long userId = 456L;
    User mockUser =
        User.newBuilder()
            .setId(userId)
            .setUsername("testuser")
            .setEmail("test@example.com")
            .setRole(UserRole.USER)
            .setAuthProvider(AuthProvider.LOCAL)
            .setCreatedAt(Timestamp.newBuilder().setSeconds(System.currentTimeMillis() / 1000))
            .setUpdatedAt(Timestamp.newBuilder().setSeconds(System.currentTimeMillis() / 1000))
            .build();

    when(userClient.getUserById(any(GetUserByIdRequest.class))).thenReturn(mockUser);

    userService.getUserById(userId);

    ArgumentCaptor<GetUserByIdRequest> captor =
        ArgumentCaptor.forClass(GetUserByIdRequest.class);
    verify(userClient).getUserById(captor.capture());

    GetUserByIdRequest capturedRequest = captor.getValue();
    assertThat(capturedRequest.getId()).isEqualTo(userId);
  }

  @Test
  @DisplayName("Should map user to UserDto")
  void shouldMapUserToUserDto() {
    long userId = 789L;
    long createdSeconds = 1704067200L;
    long updatedSeconds = 1704153600L;

    User mockUser =
        User.newBuilder()
            .setId(userId)
            .setUsername("mappeduser")
            .setEmail("mapped@example.com")
            .setRole(UserRole.ADMIN)
            .setAuthProvider(AuthProvider.GOOGLE)
            .setCreatedAt(Timestamp.newBuilder().setSeconds(createdSeconds))
            .setUpdatedAt(Timestamp.newBuilder().setSeconds(updatedSeconds))
            .build();

    when(userClient.getUserById(any(GetUserByIdRequest.class))).thenReturn(mockUser);

    UserDto result = userService.getUserById(userId);

    assertThat(result.getId()).isEqualTo(userId);
    assertThat(result.getUsername()).isEqualTo("mappeduser");
    assertThat(result.getEmail()).isEqualTo("mapped@example.com");
    assertThat(result.getRole()).isEqualTo("ADMIN");
    assertThat(result.getAuthProvider()).isEqualTo("GOOGLE");
  }

  @Test
  @DisplayName("Should handle StatusRuntimeException for getUserById")
  void shouldHandleStatusRuntimeExceptionForGetUserById() {
    long userId = 999L;
    StatusRuntimeException exception =
        new StatusRuntimeException(Status.NOT_FOUND.withDescription("User not found"));
    when(userClient.getUserById(any(GetUserByIdRequest.class))).thenThrow(exception);

    assertThatThrownBy(() -> userService.getUserById(userId))
        .isInstanceOf(NoSuchElementException.class)
        .hasMessage("User not found");
  }

  // getUsers Tests (15 tests)

  @Test
  @DisplayName("Should get users with pagination successfully")
  void shouldGetUsersWithPaginationSuccessfully() {
    User user1 =
        User.newBuilder()
            .setId(1L)
            .setUsername("user1")
            .setEmail("user1@example.com")
            .setRole(UserRole.USER)
            .setAuthProvider(AuthProvider.LOCAL)
            .setCreatedAt(Timestamp.newBuilder().setSeconds(System.currentTimeMillis() / 1000))
            .setUpdatedAt(Timestamp.newBuilder().setSeconds(System.currentTimeMillis() / 1000))
            .build();

    GetUsersResponse mockResponse =
        GetUsersResponse.newBuilder()
            .addUsers(user1)
            .setPageNo(0)
            .setPageSize(10)
            .setTotalPages(1)
            .setTotalElements(1L)
            .build();

    when(userClient.getUsers(any(GetUsersRequest.class))).thenReturn(mockResponse);

    PagedUsersDto result = userService.getUsers(0, 10, null, null);

    assertThat(result).isNotNull();
    assertThat(result.getUsers()).hasSize(1);
    assertThat(result.getPageNo()).isEqualTo(0);
    assertThat(result.getPageSize()).isEqualTo(10);
    assertThat(result.getTotalPages()).isEqualTo(1);
    assertThat(result.getTotalElements()).isEqualTo(1L);
  }

  @Test
  @DisplayName("Should get empty users list when no users exist")
  void shouldGetEmptyUsersListWhenNoUsersExist() {
    GetUsersResponse mockResponse =
        GetUsersResponse.newBuilder()
            .setPageNo(0)
            .setPageSize(10)
            .setTotalPages(0)
            .setTotalElements(0L)
            .build();

    when(userClient.getUsers(any(GetUsersRequest.class))).thenReturn(mockResponse);

    PagedUsersDto result = userService.getUsers(0, 10, null, null);

    assertThat(result.getUsers()).isEmpty();
    assertThat(result.getTotalElements()).isEqualTo(0L);
  }

  @Test
  @DisplayName("Should handle multiple pages of users")
  void shouldHandleMultiplePagesOfUsers() {
    GetUsersResponse mockResponse =
        GetUsersResponse.newBuilder()
            .setPageNo(2)
            .setPageSize(20)
            .setTotalPages(5)
            .setTotalElements(100L)
            .build();

    when(userClient.getUsers(any(GetUsersRequest.class))).thenReturn(mockResponse);

    PagedUsersDto result = userService.getUsers(2, 20, "id", "ASC");

    assertThat(result.getPageNo()).isEqualTo(2);
    assertThat(result.getPageSize()).isEqualTo(20);
    assertThat(result.getTotalPages()).isEqualTo(5);
    assertThat(result.getTotalElements()).isEqualTo(100L);
  }

  @Test
  @DisplayName("Should throw IllegalArgumentException when page number is negative")
  void shouldThrowIllegalArgumentExceptionWhenPageNumberIsNegative() {
    assertThatThrownBy(() -> userService.getUsers(-1, 10, null, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Page number cannot be negative");

    verify(userClient, never()).getUsers(any(GetUsersRequest.class));
  }

  @Test
  @DisplayName("Should throw IllegalArgumentException when page size is zero")
  void shouldThrowIllegalArgumentExceptionWhenPageSizeIsZero() {
    assertThatThrownBy(() -> userService.getUsers(0, 0, null, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Page size must be greater than zero.");

    verify(userClient, never()).getUsers(any(GetUsersRequest.class));
  }

  @Test
  @DisplayName("Should throw IllegalArgumentException when page size is negative")
  void shouldThrowIllegalArgumentExceptionWhenPageSizeIsNegative() {
    assertThatThrownBy(() -> userService.getUsers(0, -5, null, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Page size must be greater than zero.");

    verify(userClient, never()).getUsers(any(GetUsersRequest.class));
  }

  @Test
  @DisplayName("Should throw IllegalArgumentException when sortBy field is invalid")
  void shouldThrowIllegalArgumentExceptionWhenSortByFieldIsInvalid() {
    assertThatThrownBy(() -> userService.getUsers(0, 10, "invalidField", "ASC"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Invalid sortBy: 'invalidField'");

    verify(userClient, never()).getUsers(any(GetUsersRequest.class));
  }

  @Test
  @DisplayName("Should throw IllegalArgumentException when sortDirection is invalid")
  void shouldThrowIllegalArgumentExceptionWhenSortDirectionIsInvalid() {
    assertThatThrownBy(() -> userService.getUsers(0, 10, "id", "INVALID"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Invalid sortDirection: 'INVALID'");

    verify(userClient, never()).getUsers(any(GetUsersRequest.class));
  }

  @Test
  @DisplayName("Should build correct request with sortBy and sortDirection")
  void shouldBuildCorrectRequestWithSortByAndSortDirection() {
    GetUsersResponse mockResponse =
        GetUsersResponse.newBuilder()
            .setPageNo(0)
            .setPageSize(10)
            .setTotalPages(0)
            .setTotalElements(0L)
            .build();

    when(userClient.getUsers(any(GetUsersRequest.class))).thenReturn(mockResponse);

    userService.getUsers(0, 10, "username", "ASC");

    ArgumentCaptor<GetUsersRequest> captor = ArgumentCaptor.forClass(GetUsersRequest.class);
    verify(userClient).getUsers(captor.capture());

    GetUsersRequest capturedRequest = captor.getValue();
    assertThat(capturedRequest.getPageNo()).isEqualTo(0);
    assertThat(capturedRequest.getPageSize()).isEqualTo(10);
    assertThat(capturedRequest.getSortBy()).isEqualTo("username");
    assertThat(capturedRequest.getSortDirection()).isEqualTo("ASC");
  }

  @Test
  @DisplayName("Should build request with default values when sort params are null")
  void shouldBuildRequestWithDefaultValuesWhenSortParamsAreNull() {
    GetUsersResponse mockResponse =
        GetUsersResponse.newBuilder()
            .setPageNo(0)
            .setPageSize(10)
            .setTotalPages(0)
            .setTotalElements(0L)
            .build();

    when(userClient.getUsers(any(GetUsersRequest.class))).thenReturn(mockResponse);

    userService.getUsers(0, 10, null, null);

    ArgumentCaptor<GetUsersRequest> captor = ArgumentCaptor.forClass(GetUsersRequest.class);
    verify(userClient).getUsers(captor.capture());

    GetUsersRequest capturedRequest = captor.getValue();
    assertThat(capturedRequest.getSortBy()).isEqualTo("id");
    assertThat(capturedRequest.getSortDirection()).isEqualTo("DESC");
  }

  @Test
  @DisplayName("Should validate all allowed sort fields")
  void shouldValidateAllowedSortFields() {
    String[] allowedFields =
        new String[] {"id", "username", "email", "role", "authProvider", "createdAt", "updatedAt"};

    GetUsersResponse mockResponse =
        GetUsersResponse.newBuilder()
            .setPageNo(0)
            .setPageSize(10)
            .setTotalPages(0)
            .setTotalElements(0L)
            .build();

    when(userClient.getUsers(any(GetUsersRequest.class))).thenReturn(mockResponse);

    for (String field : allowedFields) {
      userService.getUsers(0, 10, field, "ASC");
      // Should not throw exception
    }

    verify(userClient, times(allowedFields.length)).getUsers(any(GetUsersRequest.class));
  }

  @Test
  @DisplayName("Should handle case-insensitive sortDirection")
  void shouldHandleCaseInsensitiveSortDirection() {
    String[] directions = new String[] {"asc", "ASC", "Asc", "desc", "DESC", "Desc"};

    GetUsersResponse mockResponse =
        GetUsersResponse.newBuilder()
            .setPageNo(0)
            .setPageSize(10)
            .setTotalPages(0)
            .setTotalElements(0L)
            .build();

    when(userClient.getUsers(any(GetUsersRequest.class))).thenReturn(mockResponse);

    for (String direction : directions) {
      userService.getUsers(0, 10, "id", direction);
      // Should not throw exception
    }

    verify(userClient, times(directions.length)).getUsers(any(GetUsersRequest.class));
  }

  @Test
  @DisplayName("Should map all user fields correctly")
  void shouldMapAllUserFieldsCorrectly() {
    long createdSeconds = 1704067200L;
    long updatedSeconds = 1704153600L;

    User user1 =
        User.newBuilder()
            .setId(123L)
            .setUsername("user1")
            .setEmail("user1@example.com")
            .setRole(UserRole.ADMIN)
            .setAuthProvider(AuthProvider.GOOGLE)
            .setCreatedAt(Timestamp.newBuilder().setSeconds(createdSeconds))
            .setUpdatedAt(Timestamp.newBuilder().setSeconds(updatedSeconds))
            .build();

    GetUsersResponse mockResponse =
        GetUsersResponse.newBuilder()
            .addUsers(user1)
            .setPageNo(0)
            .setPageSize(10)
            .setTotalPages(1)
            .setTotalElements(1L)
            .build();

    when(userClient.getUsers(any(GetUsersRequest.class))).thenReturn(mockResponse);

    PagedUsersDto result = userService.getUsers(0, 10, null, null);

    UserDto mappedUser = result.getUsers().get(0);
    assertThat(mappedUser.getId()).isEqualTo(123L);
    assertThat(mappedUser.getUsername()).isEqualTo("user1");
    assertThat(mappedUser.getEmail()).isEqualTo("user1@example.com");
    assertThat(mappedUser.getRole()).isEqualTo("ADMIN");
    assertThat(mappedUser.getAuthProvider()).isEqualTo("GOOGLE");
    assertThat(mappedUser.getCreatedAt())
        .isEqualTo(LocalDateTime.ofInstant(Instant.ofEpochSecond(createdSeconds), ZoneOffset.UTC));
    assertThat(mappedUser.getUpdatedAt())
        .isEqualTo(LocalDateTime.ofInstant(Instant.ofEpochSecond(updatedSeconds), ZoneOffset.UTC));
  }

  @Test
  @DisplayName("Should handle multiple users in response")
  void shouldHandleMultipleUsersInResponse() {
    User user1 =
        User.newBuilder()
            .setId(1L)
            .setUsername("user1")
            .setEmail("user1@example.com")
            .setRole(UserRole.USER)
            .setAuthProvider(AuthProvider.LOCAL)
            .setCreatedAt(Timestamp.newBuilder().setSeconds(System.currentTimeMillis() / 1000))
            .setUpdatedAt(Timestamp.newBuilder().setSeconds(System.currentTimeMillis() / 1000))
            .build();

    User user2 =
        User.newBuilder()
            .setId(2L)
            .setUsername("user2")
            .setEmail("user2@example.com")
            .setRole(UserRole.ADMIN)
            .setAuthProvider(AuthProvider.GOOGLE)
            .setCreatedAt(Timestamp.newBuilder().setSeconds(System.currentTimeMillis() / 1000))
            .setUpdatedAt(Timestamp.newBuilder().setSeconds(System.currentTimeMillis() / 1000))
            .build();

    GetUsersResponse mockResponse =
        GetUsersResponse.newBuilder()
            .addUsers(user1)
            .addUsers(user2)
            .setPageNo(0)
            .setPageSize(10)
            .setTotalPages(1)
            .setTotalElements(2L)
            .build();

    when(userClient.getUsers(any(GetUsersRequest.class))).thenReturn(mockResponse);

    PagedUsersDto result = userService.getUsers(0, 10, null, null);

    assertThat(result.getUsers()).hasSize(2);
    assertThat(result.getUsers().get(0).getUsername()).isEqualTo("user1");
    assertThat(result.getUsers().get(1).getUsername()).isEqualTo("user2");
  }

  @Test
  @DisplayName("Should handle StatusRuntimeException for getUsers")
  void shouldHandleStatusRuntimeExceptionForGetUsers() {
    StatusRuntimeException exception =
        new StatusRuntimeException(Status.UNAVAILABLE.withDescription("Service unavailable"));
    when(userClient.getUsers(any(GetUsersRequest.class))).thenThrow(exception);

    assertThatThrownBy(() -> userService.getUsers(0, 10, null, null))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Service unavailable");
  }

  // deleteUserById Tests (4 tests)

  @Test
  @DisplayName("Should delete user by ID successfully")
  void shouldDeleteUserByIdSuccessfully() {
    long userId = 123L;
    DeleteUserResponse mockResponse =
        DeleteUserResponse.newBuilder().setId(userId).setSuccess(true).build();

    when(userClient.deleteUserById(any(DeleteUserRequest.class))).thenReturn(mockResponse);

    boolean result = userService.deleteUserById(userId);

    assertThat(result).isTrue();
    verify(userClient).deleteUserById(any(DeleteUserRequest.class));
  }

  @Test
  @DisplayName("Should build correct DeleteUserRequest")
  void shouldBuildCorrectDeleteUserRequest() {
    long userId = 456L;
    DeleteUserResponse mockResponse =
        DeleteUserResponse.newBuilder().setId(userId).setSuccess(true).build();

    when(userClient.deleteUserById(any(DeleteUserRequest.class))).thenReturn(mockResponse);

    userService.deleteUserById(userId);

    ArgumentCaptor<DeleteUserRequest> captor = ArgumentCaptor.forClass(DeleteUserRequest.class);
    verify(userClient).deleteUserById(captor.capture());

    DeleteUserRequest capturedRequest = captor.getValue();
    assertThat(capturedRequest.getId()).isEqualTo(userId);
  }

  @Test
  @DisplayName("Should return true when delete succeeds")
  void shouldReturnTrueWhenDeleteSucceeds() {
    long userId = 789L;
    DeleteUserResponse mockResponse =
        DeleteUserResponse.newBuilder().setId(userId).setSuccess(true).build();

    when(userClient.deleteUserById(any(DeleteUserRequest.class))).thenReturn(mockResponse);

    boolean result = userService.deleteUserById(userId);

    assertThat(result).isTrue();
  }

  @Test
  @DisplayName("Should handle StatusRuntimeException for deleteUser")
  void shouldHandleStatusRuntimeExceptionForDeleteUser() {
    long userId = 999L;
    StatusRuntimeException exception =
        new StatusRuntimeException(Status.NOT_FOUND.withDescription("User not found"));
    when(userClient.deleteUserById(any(DeleteUserRequest.class))).thenThrow(exception);

    assertThatThrownBy(() -> userService.deleteUserById(userId))
        .isInstanceOf(NoSuchElementException.class)
        .hasMessage("User not found");
  }
}
