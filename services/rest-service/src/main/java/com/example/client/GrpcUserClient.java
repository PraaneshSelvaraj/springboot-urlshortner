package com.example.client;

import com.example.grpc.user.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GrpcUserClient {

  private ManagedChannel channel;
  private UserServiceGrpc.UserServiceBlockingStub stub;

  @Value("${grpc.user.host}")
  private String host;

  @Value("${grpc.user.port}")
  private int port;

  private GrpcAuthClientInterceptor authInterceptor;

  public GrpcUserClient(GrpcAuthClientInterceptor authInterceptor) {
    this.authInterceptor = authInterceptor;
  }

  @PostConstruct
  public void init() {
    this.channel =
        ManagedChannelBuilder.forAddress(host, port)
            .usePlaintext()
            .intercept(authInterceptor)
            .build();
    this.stub = UserServiceGrpc.newBlockingStub(channel);
    System.out.println("User gRPC Client connected to " + host + ":" + port);
  }

  @PreDestroy
  public void shutdown() {
    if (channel != null && !channel.isShutdown()) {
      channel.shutdown();
    }
  }

  public User createUser(CreateUserRequest request) {
    return stub.createUser(request);
  }

  public LoginResponse userLogin(LoginRequest request) {
    return stub.userLogin(request);
  }

  public LoginResponse googleLogin(GoogleLoginRequest request) {
    return stub.googleLogin(request);
  }

  public User getUserById(GetUserByIdRequest request) {
    return stub.getUserById(request);
  }

  public GetUsersResponse getUsers(GetUsersRequest request) {
    return stub.getUsers(request);
  }

  public DeleteUserResponse deleteUserById(DeleteUserRequest request) {
    return stub.deleteUserById(request);
  }

  public RefreshTokenResponse refreshTokens(RefreshTokenRequest request) {
    return stub.refreshTokens(request);
  }

  public LogoutUserResponse logoutUser(LogoutUserRequest request) {
    return stub.logoutUser(request);
  }
}
