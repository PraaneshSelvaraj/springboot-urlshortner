package com.example.service;

import com.example.dto.GoogleUserInfo;
import com.example.grpc.user.*;
import com.example.model.UserModel;
import com.example.repository.*;
import com.example.util.JwtUtil;
import com.google.protobuf.Timestamp;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class GrpcUserService extends UserServiceGrpc.UserServiceImplBase {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtUtil jwtUtil;
  private final GoogleAuthService googleAuthService;

  public GrpcUserService(
      UserRepository userRepository, JwtUtil jwtUtil, GoogleAuthService googleAuthService) {
    this.userRepository = userRepository;
    this.passwordEncoder = new BCryptPasswordEncoder();
    this.jwtUtil = jwtUtil;
    this.googleAuthService = googleAuthService;
  }

  @Override
  public void createUser(CreateUserRequest request, StreamObserver<User> responseObserver) {
    try {
      String username = request.getUsername();
      if (!username.matches("^[a-zA-Z0-9_-]{3,30}$")) {
        throw Status.INVALID_ARGUMENT
            .withDescription("Username must be 3-30 characters (letters, numbers, _, -).")
            .asRuntimeException();
      }

      String email = request.getEmail();
      if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
        throw Status.INVALID_ARGUMENT.withDescription("Invalid email format.").asRuntimeException();
      }

      String password = request.getPassword();
      if (!password.matches(
          "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$")) {
        throw Status.INVALID_ARGUMENT
            .withDescription(
                "Password must be 8+ characters with uppercase, lowercase, digit, and special"
                    + " character.")
            .asRuntimeException();
      }

      UserRole role = request.hasUserRole() ? request.getUserRole() : UserRole.USER;

      UserModel newUser = new UserModel();
      newUser.setUsername(username);
      newUser.setEmail(email);
      newUser.setPassword(passwordEncoder.encode(password));
      newUser.setRole(role.toString());
      newUser.setAuthProvider("LOCAL");

      UserModel userAdded = userRepository.save(newUser);
      userRepository.flush();

      Instant createdAtInstant =
          userAdded.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant();
      Instant updatedAtInstant =
          userAdded.getUpdatedAt().atZone(ZoneId.systemDefault()).toInstant();

      User user =
          User.newBuilder()
              .setId(userAdded.getId())
              .setUsername(userAdded.getUsername())
              .setEmail(userAdded.getEmail())
              .setRole(UserRole.valueOf(userAdded.getRole()))
              .setAuthProvider(AuthProvider.LOCAL)
              .setIsDeleted(userAdded.isDeleted())
              .setCreatedAt(
                  Timestamp.newBuilder()
                      .setSeconds(createdAtInstant.getEpochSecond())
                      .setNanos(createdAtInstant.getNano())
                      .build())
              .setUpdatedAt(
                  Timestamp.newBuilder()
                      .setSeconds(updatedAtInstant.getEpochSecond())
                      .setNanos(updatedAtInstant.getNano())
                      .build())
              .build();

      responseObserver.onNext(user);
      responseObserver.onCompleted();

    } catch (StatusRuntimeException e) {
      System.err.println("gRPC Error: " + e.getStatus().getDescription());
      responseObserver.onError(e);

    } catch (DataIntegrityViolationException e) {
      responseObserver.onError(
          Status.ALREADY_EXISTS
              .withDescription("Username or Email already exists.")
              .asRuntimeException());

    } catch (Exception e) {
      responseObserver.onError(
          Status.INTERNAL
              .withDescription("An unexpected error occurred: " + e.getMessage())
              .asRuntimeException());
    }
  }

  @Override
  public void userLogin(LoginRequest request, StreamObserver<LoginResponse> responseObserver) {
    try {
      String email = request.getEmail();
      if (email.isEmpty()) {
        throw Status.INVALID_ARGUMENT.withDescription("Email is required").asRuntimeException();
      }

      String password = request.getPassword();
      if (password.isEmpty()) {
        throw Status.INVALID_ARGUMENT.withDescription("Password is required").asRuntimeException();
      }

      UserModel user =
          userRepository
              .findByEmail(email)
              .orElseThrow(
                  () ->
                      Status.PERMISSION_DENIED
                          .withDescription("Invalid Credentials")
                          .asRuntimeException());

      if (!passwordEncoder.matches(password, user.getPassword())) {
        throw Status.PERMISSION_DENIED.withDescription("Invalid Credentials").asRuntimeException();
      }

      if (user.isDeleted()) {
        throw Status.PERMISSION_DENIED
            .withDescription("Account has been deactivated")
            .asRuntimeException();
      }

      if (user.getAuthProvider() != null && !user.getAuthProvider().equals("LOCAL")) {
        throw Status.PERMISSION_DENIED
            .withDescription(
                "This account uses "
                    + user.getAuthProvider()
                    + " authentication. Please use the appropriate login method.")
            .asRuntimeException();
      }

      String accessToken = jwtUtil.createToken(user.getEmail(), user.getRole());
      JwtUtil.RefreshTokenPair refreshToken =
          jwtUtil.createRefreshToken(user.getEmail(), user.getRole());

      user.setRefreshTokenJti(refreshToken.jti());
      userRepository.save(user);

      Instant createdAtInstant = user.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant();
      Instant updatedAtInstant = user.getUpdatedAt().atZone(ZoneId.systemDefault()).toInstant();

      User userProto =
          User.newBuilder()
              .setId(user.getId())
              .setUsername(user.getUsername())
              .setEmail(user.getEmail())
              .setRole(UserRole.valueOf(user.getRole()))
              .setAuthProvider(AuthProvider.LOCAL)
              .setIsDeleted(user.isDeleted())
              .setCreatedAt(
                  Timestamp.newBuilder()
                      .setSeconds(createdAtInstant.getEpochSecond())
                      .setNanos(createdAtInstant.getNano())
                      .build())
              .setUpdatedAt(
                  Timestamp.newBuilder()
                      .setSeconds(updatedAtInstant.getEpochSecond())
                      .setNanos(updatedAtInstant.getNano())
                      .build())
              .build();

      LoginResponse response =
          LoginResponse.newBuilder()
              .setSuccess(true)
              .setIsUserCreated(false)
              .setAccessToken(accessToken)
              .setRefreshToken(refreshToken.token())
              .setMessage("Login was Successful")
              .setUser(userProto)
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

  @Override
  public void googleLogin(
      GoogleLoginRequest request, StreamObserver<LoginResponse> responseObserver) {
    try {
      String idToken = request.getIdToken();
      if (idToken.isEmpty()) {
        throw Status.INVALID_ARGUMENT.withDescription("ID token is required").asRuntimeException();
      }

      Optional<GoogleUserInfo> googleUserInfoOpt = googleAuthService.verifyToken(idToken);

      if (googleUserInfoOpt.isEmpty()) {
        throw Status.UNAUTHENTICATED
            .withDescription("Invalid Google ID token")
            .asRuntimeException();
      }

      GoogleUserInfo googleUserInfo = googleUserInfoOpt.get();
      Optional<UserModel> existingUserOpt = userRepository.findByEmail(googleUserInfo.getEmail());

      if (existingUserOpt.isPresent()) {
        UserModel existingUser = existingUserOpt.get();

        if (existingUser.isDeleted()) {
          throw Status.PERMISSION_DENIED
              .withDescription("Account has been deactivated")
              .asRuntimeException();
        }

        if (existingUser.getAuthProvider() != null
            && !existingUser.getAuthProvider().equals("GOOGLE")) {
          throw Status.ALREADY_EXISTS
              .withDescription(
                  "Account exists with "
                      + existingUser.getAuthProvider()
                      + " authentication. Please use the appropriate login method.")
              .asRuntimeException();
        }

        String accessToken = jwtUtil.createToken(existingUser.getEmail(), existingUser.getRole());
        JwtUtil.RefreshTokenPair refreshToken =
            jwtUtil.createRefreshToken(existingUser.getEmail(), existingUser.getRole());

        int rowsAffected =
            userRepository.updateRefreshToken(existingUser.getId(), refreshToken.jti());
        if (rowsAffected <= 0) {
          throw Status.UNKNOWN.withDescription("Unable to login").asRuntimeException();
        }

        UserModel updatedUser = userRepository.findById(existingUser.getId()).orElseThrow();
        Instant createdAtInstant =
            updatedUser.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant();
        Instant updatedAtInstant =
            updatedUser.getUpdatedAt().atZone(ZoneId.systemDefault()).toInstant();

        User userProto =
            User.newBuilder()
                .setId(updatedUser.getId())
                .setUsername(updatedUser.getUsername())
                .setEmail(updatedUser.getEmail())
                .setRole(UserRole.valueOf(updatedUser.getRole()))
                .setAuthProvider(AuthProvider.GOOGLE)
                .setIsDeleted(updatedUser.isDeleted())
                .setCreatedAt(
                    Timestamp.newBuilder()
                        .setSeconds(createdAtInstant.getEpochSecond())
                        .setNanos(createdAtInstant.getNano())
                        .build())
                .setUpdatedAt(
                    Timestamp.newBuilder()
                        .setSeconds(updatedAtInstant.getEpochSecond())
                        .setNanos(updatedAtInstant.getNano())
                        .build())
                .build();

        LoginResponse response =
            LoginResponse.newBuilder()
                .setSuccess(true)
                .setIsUserCreated(false)
                .setAccessToken(accessToken)
                .setRefreshToken(refreshToken.token())
                .setMessage("Login was Successful")
                .setUser(userProto)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();

      } else {
        UserModel newUser = new UserModel();
        newUser.setUsername(googleUserInfo.getName());
        newUser.setEmail(googleUserInfo.getEmail());
        newUser.setPassword(null);
        newUser.setRole("USER");
        newUser.setGoogleId(googleUserInfo.getGoogleId());
        newUser.setAuthProvider("GOOGLE");
        newUser.setRefreshTokenJti(null);
        newUser.setDeleted(false);

        try {
          UserModel savedUser = userRepository.save(newUser);
          userRepository.flush();

          String accessToken = jwtUtil.createToken(savedUser.getEmail(), savedUser.getRole());
          JwtUtil.RefreshTokenPair refreshToken =
              jwtUtil.createRefreshToken(savedUser.getEmail(), savedUser.getRole());

          userRepository.updateRefreshToken(savedUser.getId(), refreshToken.token());
          UserModel updatedUser = userRepository.findById(savedUser.getId()).orElseThrow();

          Instant createdAtInstant =
              updatedUser.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant();
          Instant updatedAtInstant =
              updatedUser.getUpdatedAt().atZone(ZoneId.systemDefault()).toInstant();

          User userProto =
              User.newBuilder()
                  .setId(updatedUser.getId())
                  .setUsername(updatedUser.getUsername())
                  .setEmail(updatedUser.getEmail())
                  .setRole(UserRole.USER)
                  .setAuthProvider(AuthProvider.GOOGLE)
                  .setIsDeleted(updatedUser.isDeleted())
                  .setCreatedAt(
                      Timestamp.newBuilder()
                          .setSeconds(createdAtInstant.getEpochSecond())
                          .setNanos(createdAtInstant.getNano())
                          .build())
                  .setUpdatedAt(
                      Timestamp.newBuilder()
                          .setSeconds(updatedAtInstant.getEpochSecond())
                          .setNanos(updatedAtInstant.getNano())
                          .build())
                  .build();

          LoginResponse response =
              LoginResponse.newBuilder()
                  .setSuccess(true)
                  .setIsUserCreated(true)
                  .setAccessToken(accessToken)
                  .setRefreshToken(refreshToken.token())
                  .setMessage("Account created and login successful")
                  .setUser(userProto)
                  .build();

          responseObserver.onNext(response);
          responseObserver.onCompleted();

        } catch (DataIntegrityViolationException e) {
          responseObserver.onError(
              Status.ALREADY_EXISTS.withDescription("User already exists").asRuntimeException());
        } catch (Exception e) {
          responseObserver.onError(
              Status.INTERNAL
                  .withDescription("Failed to create user: " + e.getMessage())
                  .asRuntimeException());
        }
      }

    } catch (StatusRuntimeException e) {
      System.err.println("gRPC Error: " + e.getStatus().getDescription());
      responseObserver.onError(e);

    } catch (Exception e) {
      responseObserver.onError(
          Status.INTERNAL
              .withDescription("Authentication failed: " + e.getMessage())
              .asRuntimeException());
    }
  }

  @Override
  public void getUserById(GetUserRequest request, StreamObserver<User> responseObserver) {
    try {
      long id = request.getId();
      if (id <= 0) {
        throw Status.INVALID_ARGUMENT.withDescription("ID should be valid.").asRuntimeException();
      }

      UserModel user =
          userRepository
              .findById(id)
              .orElseThrow(
                  () ->
                      Status.NOT_FOUND
                          .withDescription("Unable to find User with Id: " + id)
                          .asRuntimeException());

      if (user.isDeleted()) {
        throw Status.NOT_FOUND
            .withDescription("Unable to find user with Id: " + id)
            .asRuntimeException();
      }

      AuthProvider authProvider;
      String authProviderStr = user.getAuthProvider();
      if (authProviderStr == null) {
        authProvider = AuthProvider.LOCAL;
      } else {
        switch (authProviderStr) {
          case "LOCAL":
            authProvider = AuthProvider.LOCAL;
            break;
          case "GOOGLE":
            authProvider = AuthProvider.GOOGLE;
            break;
          default:
            throw new IllegalArgumentException("Unknown auth provider: " + authProviderStr);
        }
      }

      UserRole userRole;
      String roleStr = user.getRole();
      if (roleStr == null) {
        throw Status.INTERNAL.withDescription("User role cannot be null").asRuntimeException();
      }
      switch (roleStr) {
        case "ADMIN":
          userRole = UserRole.ADMIN;
          break;
        case "USER":
          userRole = UserRole.USER;
          break;
        default:
          throw new IllegalArgumentException("Unknown user role: " + roleStr);
      }

      Instant createdAtInstant = user.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant();
      Instant updatedAtInstant = user.getUpdatedAt().atZone(ZoneId.systemDefault()).toInstant();

      User userProto =
          User.newBuilder()
              .setId(user.getId())
              .setUsername(user.getUsername())
              .setEmail(user.getEmail())
              .setRole(userRole)
              .setAuthProvider(authProvider)
              .setIsDeleted(user.isDeleted())
              .setCreatedAt(
                  Timestamp.newBuilder()
                      .setSeconds(createdAtInstant.getEpochSecond())
                      .setNanos(createdAtInstant.getNano())
                      .build())
              .setUpdatedAt(
                  Timestamp.newBuilder()
                      .setSeconds(updatedAtInstant.getEpochSecond())
                      .setNanos(updatedAtInstant.getNano())
                      .build())
              .build();

      responseObserver.onNext(userProto);
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
  public void deleteUserById(
      DeleteUserRequest request, StreamObserver<DeleteUserResponse> responseObserver) {
    try {
      long id = request.getId();
      if (id <= 0) {
        throw Status.INVALID_ARGUMENT.withDescription("ID should be valid.").asRuntimeException();
      }

      int rowsAffected = userRepository.softDeleteById(id);

      if (rowsAffected == 0) {
        throw Status.UNKNOWN
            .withDescription("Unable to delete User with Id: " + id)
            .asRuntimeException();
      }

      DeleteUserResponse response =
          DeleteUserResponse.newBuilder().setId(id).setSuccess(true).build();

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

  @Override
  public void refreshTokens(
      RefreshTokenRequest request, StreamObserver<RefreshTokenResponse> responseObserver) {
    try {
      String refreshToken = request.getRefreshToken();
      if (refreshToken.isEmpty()) {
        throw Status.UNAUTHENTICATED
            .withDescription("Refresh token is required")
            .asRuntimeException();
      }

      if (!jwtUtil.validateToken(refreshToken)) {
        throw Status.UNAUTHENTICATED.withDescription("Invalid refresh token").asRuntimeException();
      }

      String email = jwtUtil.extractEmail(refreshToken);
      if (email == null || email.isEmpty()) {
        throw Status.UNAUTHENTICATED
            .withDescription("Invalid refresh token claims")
            .asRuntimeException();
      }

      UserModel user =
          userRepository
              .findByEmail(email)
              .orElseThrow(
                  () ->
                      Status.UNAUTHENTICATED
                          .withDescription("Unable to find the user")
                          .asRuntimeException());

      String storedRefreshTokenJti = user.getRefreshTokenJti();
      String refreshTokenJti = jwtUtil.extractJti(refreshToken);
      if (storedRefreshTokenJti == null || !storedRefreshTokenJti.equals(refreshTokenJti)) {
        throw Status.UNAUTHENTICATED.withDescription("Invalid Refresh Token").asRuntimeException();
      }

      String newAccessToken = jwtUtil.createToken(user.getEmail(), user.getRole());
      JwtUtil.RefreshTokenPair newRefreshToken =
          jwtUtil.createRefreshToken(user.getEmail(), user.getRole());

      user.setRefreshTokenJti(newRefreshToken.jti());
      userRepository.save(user);

      RefreshTokenResponse response =
          RefreshTokenResponse.newBuilder()
              .setAccessToken(newAccessToken)
              .setRefreshToken(newRefreshToken.token())
              .build();

      responseObserver.onNext(response);
      responseObserver.onCompleted();

    } catch (StatusRuntimeException e) {
      System.err.println("gRPC Error: " + e.getStatus().getDescription());
      responseObserver.onError(e);

    } catch (Exception e) {
      responseObserver.onError(
          Status.UNAUTHENTICATED.withDescription("Invalid refresh token").asRuntimeException());
    }
  }

  @Override
  public void logoutUser(
      LogoutUserRequest request, StreamObserver<LogoutUserResponse> responseObserver) {
    responseObserver.onNext(LogoutUserResponse.newBuilder().build());
    responseObserver.onCompleted();
  }
}
