package com.example.service;

import com.example.client.GrpcUserClient;
import com.example.dto.CreateUserDto;
import com.example.dto.PagedUsersDto;
import com.example.dto.UserDto;
import com.example.grpc.user.*;
import com.example.util.GrpcExceptionHandler;
import io.grpc.StatusRuntimeException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class UserService {
  private final GrpcUserClient userClient;
  private final NotificationService notificationService;

  public UserService(GrpcUserClient userClient, NotificationService notificationService) {
    this.userClient = userClient;
    this.notificationService = notificationService;
  }

  public UserDto createUser(CreateUserDto requestDto) {
    try {
      UserRole role =
          switch (requestDto.getRole().toUpperCase()) {
            case "ADMIN" -> UserRole.ADMIN;
            case "USER" -> UserRole.USER;
            default ->
                throw new IllegalArgumentException(
                    "Invalid role: " + requestDto.getRole() + ". Must be USER or ADMIN");
          };

      CreateUserRequest request =
          CreateUserRequest.newBuilder()
              .setUsername(requestDto.getUsername())
              .setEmail(requestDto.getEmail())
              .setPassword(requestDto.getPassword())
              .setUserRole(role)
              .build();

      User response = userClient.createUser(request);
      notificationService.sendUserCreatedNotification(response.getUsername());

      return mapToUserDto(response);
    } catch (StatusRuntimeException e) {
      throw GrpcExceptionHandler.handleGrpcException(e, "Failed to create user");
    }
  }

  public UserDto getUserById(long id) {
    try {
      GetUserByIdRequest request = GetUserByIdRequest.newBuilder().setId(id).build();
      User user = userClient.getUserById(request);
      return mapToUserDto(user);
    } catch (StatusRuntimeException e) {
      throw GrpcExceptionHandler.handleGrpcException(e, "Failed to get user");
    }
  }

  public PagedUsersDto getUsers(int pageNo, int pageSize, String sortBy, String sortDirection) {
    try {
      if (pageNo < 0) {
        throw new IllegalArgumentException("Page number cannot be negative");
      }

      if (pageSize <= 0) {
        throw new IllegalArgumentException("Page size must be greater than zero.");
      }

      Set<String> allowedSortField =
          Set.of("id", "username", "email", "role", "authProvider", "createdAt", "updatedAt");
      if (sortBy != null && !allowedSortField.contains(sortBy)) {
        throw new IllegalArgumentException(
            "Invalid sortBy: '" + sortBy + "'. Allowed values: " + allowedSortField);
      }
      String validSortBy = sortBy != null ? sortBy : "id";

      if (sortDirection != null
          && !sortDirection.equalsIgnoreCase("ASC")
          && !sortDirection.equalsIgnoreCase("DESC")) {
        throw new IllegalArgumentException(
            "Invalid sortDirection: '" + sortDirection + "'. Allowed values: ASC, DESC");
      }

      String direction = sortDirection != null ? sortDirection : "DESC";

      GetUsersRequest request =
          GetUsersRequest.newBuilder()
              .setPageNo(pageNo)
              .setPageSize(pageSize)
              .setSortBy(validSortBy)
              .setSortDirection(direction)
              .build();

      GetUsersResponse response = userClient.getUsers(request);
      List<UserDto> users =
          response.getUsersList().stream().map(this::mapToUserDto).collect(Collectors.toList());

      PagedUsersDto pagedUsersDto = new PagedUsersDto();
      pagedUsersDto.setUsers(users);
      pagedUsersDto.setPageNo(response.getPageNo());
      pagedUsersDto.setPageSize(response.getPageSize());
      pagedUsersDto.setTotalPages(response.getTotalPages());
      pagedUsersDto.setTotalElements(response.getTotalElements());

      return pagedUsersDto;
    } catch (StatusRuntimeException e) {
      throw GrpcExceptionHandler.handleGrpcException(e, "Failed to fetch users");
    }
  }

  public boolean deleteUserById(long id) {
    try {
      DeleteUserRequest request = DeleteUserRequest.newBuilder().setId(id).build();
      DeleteUserResponse response = userClient.deleteUserById(request);
      return response.getSuccess();
    } catch (StatusRuntimeException e) {
      throw GrpcExceptionHandler.handleGrpcException(e, "Failed to delete user");
    }
  }

  private UserDto mapToUserDto(User user) {
    UserDto userDto = new UserDto();
    userDto.setId(user.getId());
    userDto.setUsername(user.getUsername());
    userDto.setEmail(user.getEmail());
    userDto.setRole(user.getRole().toString());
    userDto.setAuthProvider(user.getAuthProvider().toString());
    userDto.setCreatedAt(
        LocalDateTime.ofInstant(
            Instant.ofEpochSecond(user.getCreatedAt().getSeconds(), user.getCreatedAt().getNanos()),
            ZoneOffset.UTC));
    userDto.setUpdatedAt(
        LocalDateTime.ofInstant(
            Instant.ofEpochSecond(user.getUpdatedAt().getSeconds(), user.getUpdatedAt().getNanos()),
            ZoneOffset.UTC));

    return userDto;
  }
}
