package com.example.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.example.grpc.user.*;
import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
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
@DisplayName("GrpcUserClient Tests - Infrastructure Layer")
class GrpcUserClientTest {

  @Mock private ManagedChannel channel;

  @Mock private UserServiceGrpc.UserServiceBlockingStub stub;

  @InjectMocks private GrpcUserClient grpcUserClient;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(grpcUserClient, "host", "localhost");
    ReflectionTestUtils.setField(grpcUserClient, "port", 9091);
    ReflectionTestUtils.setField(grpcUserClient, "channel", channel);
    ReflectionTestUtils.setField(grpcUserClient, "stub", stub);
  }

  // Lifecycle Tests

  @Test
  @DisplayName("Should shutdown channel when not already shutdown")
  void shouldShutdownChannelWhenNotShutdown() {
    when(channel.isShutdown()).thenReturn(false);

    grpcUserClient.shutdown();

    verify(channel).shutdown();
  }

  @Test
  @DisplayName("Should not shutdown channel when already shutdown")
  void shouldNotShutdownChannelWhenAlreadyShutdown() {
    when(channel.isShutdown()).thenReturn(true);

    grpcUserClient.shutdown();

    verify(channel, never()).shutdown();
  }

  @Test
  @DisplayName("Should not throw exception when channel is null during shutdown")
  void shouldNotThrowExceptionWhenChannelIsNullDuringShutdown() {
    ReflectionTestUtils.setField(grpcUserClient, "channel", null);

    // Should not throw any exception
    grpcUserClient.shutdown();
  }

  // createUser Tests

  @Test
  @DisplayName("Should call gRPC stub createUser with provided request")
  void shouldCallGrpcStubCreateUserWithProvidedRequest() {
    CreateUserRequest request =
        CreateUserRequest.newBuilder()
            .setUsername("testuser")
            .setEmail("test@example.com")
            .setPassword("password123")
            .setUserRole(UserRole.USER)
            .build();

    User mockUser =
        User.newBuilder()
            .setId(1L)
            .setUsername("testuser")
            .setEmail("test@example.com")
            .setRole(UserRole.USER)
            .setAuthProvider(AuthProvider.LOCAL)
            .build();

    when(stub.createUser(any(CreateUserRequest.class))).thenReturn(mockUser);

    User result = grpcUserClient.createUser(request);

    assertThat(result).isNotNull();
    assertThat(result.getId()).isEqualTo(1L);
    assertThat(result.getUsername()).isEqualTo("testuser");
    assertThat(result.getEmail()).isEqualTo("test@example.com");

    ArgumentCaptor<CreateUserRequest> captor = ArgumentCaptor.forClass(CreateUserRequest.class);
    verify(stub).createUser(captor.capture());

    CreateUserRequest capturedRequest = captor.getValue();
    assertThat(capturedRequest.getUsername()).isEqualTo("testuser");
    assertThat(capturedRequest.getEmail()).isEqualTo("test@example.com");
    assertThat(capturedRequest.getPassword()).isEqualTo("password123");
    assertThat(capturedRequest.getUserRole()).isEqualTo(UserRole.USER);
  }

  @Test
  @DisplayName("Should propagate exception when createUser fails")
  void shouldPropagateExceptionWhenCreateUserFails() {
    CreateUserRequest request =
        CreateUserRequest.newBuilder()
            .setUsername("testuser")
            .setEmail("test@example.com")
            .setPassword("password123")
            .setUserRole(UserRole.USER)
            .build();

    StatusRuntimeException exception =
        new StatusRuntimeException(Status.ALREADY_EXISTS.withDescription("User already exists"));
    when(stub.createUser(any(CreateUserRequest.class))).thenThrow(exception);

    assertThatThrownBy(() -> grpcUserClient.createUser(request))
        .isInstanceOf(StatusRuntimeException.class)
        .hasMessageContaining("User already exists");

    verify(stub).createUser(any(CreateUserRequest.class));
  }

  // userLogin Tests

  @Test
  @DisplayName("Should call gRPC stub userLogin with provided request")
  void shouldCallGrpcStubUserLoginWithProvidedRequest() {
    LoginRequest request =
        LoginRequest.newBuilder().setEmail("test@example.com").setPassword("password123").build();

    LoginResponse mockResponse =
        LoginResponse.newBuilder()
            .setAccessToken("access-token")
            .setRefreshToken("refresh-token")
            .setSuccess(true)
            .build();

    when(stub.userLogin(any(LoginRequest.class))).thenReturn(mockResponse);

    LoginResponse result = grpcUserClient.userLogin(request);

    assertThat(result).isNotNull();
    assertThat(result.getAccessToken()).isEqualTo("access-token");
    assertThat(result.getRefreshToken()).isEqualTo("refresh-token");
    assertThat(result.getSuccess()).isTrue();

    ArgumentCaptor<LoginRequest> captor = ArgumentCaptor.forClass(LoginRequest.class);
    verify(stub).userLogin(captor.capture());

    LoginRequest capturedRequest = captor.getValue();
    assertThat(capturedRequest.getEmail()).isEqualTo("test@example.com");
    assertThat(capturedRequest.getPassword()).isEqualTo("password123");
  }

  @Test
  @DisplayName("Should propagate exception when userLogin fails")
  void shouldPropagateExceptionWhenUserLoginFails() {
    LoginRequest request =
        LoginRequest.newBuilder().setEmail("test@example.com").setPassword("wrong").build();

    StatusRuntimeException exception =
        new StatusRuntimeException(
            Status.UNAUTHENTICATED.withDescription("Invalid credentials"));
    when(stub.userLogin(any(LoginRequest.class))).thenThrow(exception);

    assertThatThrownBy(() -> grpcUserClient.userLogin(request))
        .isInstanceOf(StatusRuntimeException.class)
        .hasMessageContaining("Invalid credentials");

    verify(stub).userLogin(any(LoginRequest.class));
  }

  // googleLogin Tests

  @Test
  @DisplayName("Should call gRPC stub googleLogin with provided request")
  void shouldCallGrpcStubGoogleLoginWithProvidedRequest() {
    GoogleLoginRequest request = GoogleLoginRequest.newBuilder().setIdToken("google-token").build();

    LoginResponse mockResponse =
        LoginResponse.newBuilder()
            .setAccessToken("access-token")
            .setRefreshToken("refresh-token")
            .setSuccess(true)
            .build();

    when(stub.googleLogin(any(GoogleLoginRequest.class))).thenReturn(mockResponse);

    LoginResponse result = grpcUserClient.googleLogin(request);

    assertThat(result).isNotNull();
    assertThat(result.getAccessToken()).isEqualTo("access-token");
    assertThat(result.getRefreshToken()).isEqualTo("refresh-token");

    ArgumentCaptor<GoogleLoginRequest> captor = ArgumentCaptor.forClass(GoogleLoginRequest.class);
    verify(stub).googleLogin(captor.capture());

    GoogleLoginRequest capturedRequest = captor.getValue();
    assertThat(capturedRequest.getIdToken()).isEqualTo("google-token");
  }

  @Test
  @DisplayName("Should propagate exception when googleLogin fails")
  void shouldPropagateExceptionWhenGoogleLoginFails() {
    GoogleLoginRequest request = GoogleLoginRequest.newBuilder().setIdToken("invalid-token").build();

    StatusRuntimeException exception =
        new StatusRuntimeException(Status.INVALID_ARGUMENT.withDescription("Invalid token"));
    when(stub.googleLogin(any(GoogleLoginRequest.class))).thenThrow(exception);

    assertThatThrownBy(() -> grpcUserClient.googleLogin(request))
        .isInstanceOf(StatusRuntimeException.class)
        .hasMessageContaining("Invalid token");

    verify(stub).googleLogin(any(GoogleLoginRequest.class));
  }

  // getUserById Tests

  @Test
  @DisplayName("Should call gRPC stub getUserById with provided request")
  void shouldCallGrpcStubGetUserByIdWithProvidedRequest() {
    GetUserByIdRequest request = GetUserByIdRequest.newBuilder().setId(1L).build();

    User mockUser =
        User.newBuilder()
            .setId(1L)
            .setUsername("testuser")
            .setEmail("test@example.com")
            .setRole(UserRole.USER)
            .setAuthProvider(AuthProvider.LOCAL)
            .build();

    when(stub.getUserById(any(GetUserByIdRequest.class))).thenReturn(mockUser);

    User result = grpcUserClient.getUserById(request);

    assertThat(result).isNotNull();
    assertThat(result.getId()).isEqualTo(1L);
    assertThat(result.getUsername()).isEqualTo("testuser");

    ArgumentCaptor<GetUserByIdRequest> captor = ArgumentCaptor.forClass(GetUserByIdRequest.class);
    verify(stub).getUserById(captor.capture());

    GetUserByIdRequest capturedRequest = captor.getValue();
    assertThat(capturedRequest.getId()).isEqualTo(1L);
  }

  @Test
  @DisplayName("Should propagate exception when getUserById fails")
  void shouldPropagateExceptionWhenGetUserByIdFails() {
    GetUserByIdRequest request = GetUserByIdRequest.newBuilder().setId(999L).build();

    StatusRuntimeException exception =
        new StatusRuntimeException(Status.NOT_FOUND.withDescription("User not found"));
    when(stub.getUserById(any(GetUserByIdRequest.class))).thenThrow(exception);

    assertThatThrownBy(() -> grpcUserClient.getUserById(request))
        .isInstanceOf(StatusRuntimeException.class)
        .hasMessageContaining("User not found");

    verify(stub).getUserById(any(GetUserByIdRequest.class));
  }

  // getUsers Tests

  @Test
  @DisplayName("Should call gRPC stub getUsers with provided request")
  void shouldCallGrpcStubGetUsersWithProvidedRequest() {
    GetUsersRequest request =
        GetUsersRequest.newBuilder()
            .setPageNo(0)
            .setPageSize(10)
            .setSortBy("id")
            .setSortDirection("DESC")
            .build();

    User user1 =
        User.newBuilder()
            .setId(1L)
            .setUsername("user1")
            .setEmail("user1@example.com")
            .setRole(UserRole.USER)
            .setAuthProvider(AuthProvider.LOCAL)
            .build();

    GetUsersResponse mockResponse =
        GetUsersResponse.newBuilder()
            .addUsers(user1)
            .setPageNo(0)
            .setPageSize(10)
            .setTotalPages(1)
            .setTotalElements(1L)
            .build();

    when(stub.getUsers(any(GetUsersRequest.class))).thenReturn(mockResponse);

    GetUsersResponse result = grpcUserClient.getUsers(request);

    assertThat(result).isNotNull();
    assertThat(result.getUsersCount()).isEqualTo(1);
    assertThat(result.getPageNo()).isEqualTo(0);
    assertThat(result.getPageSize()).isEqualTo(10);

    ArgumentCaptor<GetUsersRequest> captor = ArgumentCaptor.forClass(GetUsersRequest.class);
    verify(stub).getUsers(captor.capture());

    GetUsersRequest capturedRequest = captor.getValue();
    assertThat(capturedRequest.getPageNo()).isEqualTo(0);
    assertThat(capturedRequest.getPageSize()).isEqualTo(10);
    assertThat(capturedRequest.getSortBy()).isEqualTo("id");
    assertThat(capturedRequest.getSortDirection()).isEqualTo("DESC");
  }

  @Test
  @DisplayName("Should propagate exception when getUsers fails")
  void shouldPropagateExceptionWhenGetUsersFails() {
    GetUsersRequest request =
        GetUsersRequest.newBuilder()
            .setPageNo(-1)
            .setPageSize(10)
            .setSortBy("id")
            .setSortDirection("DESC")
            .build();

    StatusRuntimeException exception =
        new StatusRuntimeException(
            Status.INVALID_ARGUMENT.withDescription("Invalid page number"));
    when(stub.getUsers(any(GetUsersRequest.class))).thenThrow(exception);

    assertThatThrownBy(() -> grpcUserClient.getUsers(request))
        .isInstanceOf(StatusRuntimeException.class)
        .hasMessageContaining("Invalid page number");

    verify(stub).getUsers(any(GetUsersRequest.class));
  }

  // deleteUserById Tests

  @Test
  @DisplayName("Should call gRPC stub deleteUserById with provided request")
  void shouldCallGrpcStubDeleteUserByIdWithProvidedRequest() {
    DeleteUserRequest request = DeleteUserRequest.newBuilder().setId(1L).build();

    DeleteUserResponse mockResponse =
        DeleteUserResponse.newBuilder().setId(1L).setSuccess(true).build();

    when(stub.deleteUserById(any(DeleteUserRequest.class))).thenReturn(mockResponse);

    DeleteUserResponse result = grpcUserClient.deleteUserById(request);

    assertThat(result).isNotNull();
    assertThat(result.getId()).isEqualTo(1L);
    assertThat(result.getSuccess()).isTrue();

    ArgumentCaptor<DeleteUserRequest> captor = ArgumentCaptor.forClass(DeleteUserRequest.class);
    verify(stub).deleteUserById(captor.capture());

    DeleteUserRequest capturedRequest = captor.getValue();
    assertThat(capturedRequest.getId()).isEqualTo(1L);
  }

  @Test
  @DisplayName("Should propagate exception when deleteUserById fails")
  void shouldPropagateExceptionWhenDeleteUserByIdFails() {
    DeleteUserRequest request = DeleteUserRequest.newBuilder().setId(999L).build();

    StatusRuntimeException exception =
        new StatusRuntimeException(Status.NOT_FOUND.withDescription("User not found"));
    when(stub.deleteUserById(any(DeleteUserRequest.class))).thenThrow(exception);

    assertThatThrownBy(() -> grpcUserClient.deleteUserById(request))
        .isInstanceOf(StatusRuntimeException.class)
        .hasMessageContaining("User not found");

    verify(stub).deleteUserById(any(DeleteUserRequest.class));
  }

  // refreshTokens Tests

  @Test
  @DisplayName("Should call gRPC stub refreshTokens with provided request")
  void shouldCallGrpcStubRefreshTokensWithProvidedRequest() {
    RefreshTokenRequest request =
        RefreshTokenRequest.newBuilder().setRefreshToken("refresh-token").build();

    RefreshTokenResponse mockResponse =
        RefreshTokenResponse.newBuilder()
            .setAccessToken("new-access-token")
            .setRefreshToken("new-refresh-token")
            .build();

    when(stub.refreshTokens(any(RefreshTokenRequest.class))).thenReturn(mockResponse);

    RefreshTokenResponse result = grpcUserClient.refreshTokens(request);

    assertThat(result).isNotNull();
    assertThat(result.getAccessToken()).isEqualTo("new-access-token");
    assertThat(result.getRefreshToken()).isEqualTo("new-refresh-token");

    ArgumentCaptor<RefreshTokenRequest> captor = ArgumentCaptor.forClass(RefreshTokenRequest.class);
    verify(stub).refreshTokens(captor.capture());

    RefreshTokenRequest capturedRequest = captor.getValue();
    assertThat(capturedRequest.getRefreshToken()).isEqualTo("refresh-token");
  }

  @Test
  @DisplayName("Should propagate exception when refreshTokens fails")
  void shouldPropagateExceptionWhenRefreshTokensFails() {
    RefreshTokenRequest request =
        RefreshTokenRequest.newBuilder().setRefreshToken("invalid-token").build();

    StatusRuntimeException exception =
        new StatusRuntimeException(Status.UNAUTHENTICATED.withDescription("Invalid refresh token"));
    when(stub.refreshTokens(any(RefreshTokenRequest.class))).thenThrow(exception);

    assertThatThrownBy(() -> grpcUserClient.refreshTokens(request))
        .isInstanceOf(StatusRuntimeException.class)
        .hasMessageContaining("Invalid refresh token");

    verify(stub).refreshTokens(any(RefreshTokenRequest.class));
  }

  // logoutUser Tests

  @Test
  @DisplayName("Should call gRPC stub logoutUser with provided request")
  void shouldCallGrpcStubLogoutUserWithProvidedRequest() {
    LogoutUserRequest request =
        LogoutUserRequest.newBuilder().setId(1L).build();

    LogoutUserResponse mockResponse =
        LogoutUserResponse.newBuilder().setSuccess(true).setMessage("Logged out").build();

    when(stub.logoutUser(any(LogoutUserRequest.class))).thenReturn(mockResponse);

    LogoutUserResponse result = grpcUserClient.logoutUser(request);

    assertThat(result).isNotNull();
    assertThat(result.getSuccess()).isTrue();
    assertThat(result.getMessage()).isEqualTo("Logged out");

    ArgumentCaptor<LogoutUserRequest> captor = ArgumentCaptor.forClass(LogoutUserRequest.class);
    verify(stub).logoutUser(captor.capture());

    LogoutUserRequest capturedRequest = captor.getValue();
    assertThat(capturedRequest.getId()).isEqualTo(1L);
  }

  @Test
  @DisplayName("Should propagate exception when logoutUser fails")
  void shouldPropagateExceptionWhenLogoutUserFails() {
    LogoutUserRequest request =
        LogoutUserRequest.newBuilder().setId(999L).build();

    StatusRuntimeException exception =
        new StatusRuntimeException(Status.INVALID_ARGUMENT.withDescription("Invalid token"));
    when(stub.logoutUser(any(LogoutUserRequest.class))).thenThrow(exception);

    assertThatThrownBy(() -> grpcUserClient.logoutUser(request))
        .isInstanceOf(StatusRuntimeException.class)
        .hasMessageContaining("Invalid token");

    verify(stub).logoutUser(any(LogoutUserRequest.class));
  }
}
