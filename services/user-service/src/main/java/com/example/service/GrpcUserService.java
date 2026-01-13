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
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class GrpcUserService extends UserServiceGrpc.UserServiceImplBase {

  private final UserRepository userRepo;
  private final PasswordEncoder passwordEncoder;
  private final JwtUtil jwtUtil;
  private final GoogleAuthService googleAuthService;

  public GrpcUserService(
      UserRepository userRepo, JwtUtil jwtUtil, GoogleAuthService googleAuthService) {
    this.userRepo = userRepo;
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

      UserModel userAdded = userRepo.save(newUser);
      userRepo.flush();

      Instant createdAtInstant = userAdded.getCreatedAt().atZone(ZoneOffset.UTC).toInstant();
      Instant updatedAtInstant = userAdded.getUpdatedAt().atZone(ZoneOffset.UTC).toInstant();

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
          userRepo
              .findByEmail(email)
              .orElseThrow(
                  () ->
                      Status.UNAUTHENTICATED
                          .withDescription("Invalid Credentials")
                          .asRuntimeException());

      if (!passwordEncoder.matches(password, user.getPassword())) {
        throw Status.UNAUTHENTICATED.withDescription("Invalid Credentials").asRuntimeException();
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

      String accessToken = jwtUtil.createToken(user.getId(), user.getEmail(), user.getRole());
      JwtUtil.RefreshTokenPair refreshToken =
          jwtUtil.createRefreshToken(user.getId(), user.getEmail(), user.getRole());

      user.setRefreshTokenJti(refreshToken.jti());
      userRepo.save(user);

      Instant createdAtInstant = user.getCreatedAt().atZone(ZoneOffset.UTC).toInstant();
      Instant updatedAtInstant = user.getUpdatedAt().atZone(ZoneOffset.UTC).toInstant();

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
      Optional<UserModel> existingUserOpt = userRepo.findByEmail(googleUserInfo.getEmail());

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

        String accessToken =
            jwtUtil.createToken(
                existingUser.getId(), existingUser.getEmail(), existingUser.getRole());
        JwtUtil.RefreshTokenPair refreshToken =
            jwtUtil.createRefreshToken(
                existingUser.getId(), existingUser.getEmail(), existingUser.getRole());

        int rowsAffected = userRepo.updateRefreshTokenJti(existingUser.getId(), refreshToken.jti());
        if (rowsAffected <= 0) {
          throw Status.UNKNOWN.withDescription("Unable to login").asRuntimeException();
        }

        UserModel updatedUser = userRepo.findById(existingUser.getId()).orElseThrow();
        Instant createdAtInstant = updatedUser.getCreatedAt().atZone(ZoneOffset.UTC).toInstant();
        Instant updatedAtInstant = updatedUser.getUpdatedAt().atZone(ZoneOffset.UTC).toInstant();

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
          UserModel savedUser = userRepo.save(newUser);
          userRepo.flush();

          String accessToken =
              jwtUtil.createToken(savedUser.getId(), savedUser.getEmail(), savedUser.getRole());
          JwtUtil.RefreshTokenPair refreshToken =
              jwtUtil.createRefreshToken(
                  savedUser.getId(), savedUser.getEmail(), savedUser.getRole());

          userRepo.updateRefreshTokenJti(savedUser.getId(), refreshToken.jti());
          UserModel updatedUser = userRepo.findById(savedUser.getId()).orElseThrow();

          Instant createdAtInstant = updatedUser.getCreatedAt().atZone(ZoneOffset.UTC).toInstant();
          Instant updatedAtInstant = updatedUser.getUpdatedAt().atZone(ZoneOffset.UTC).toInstant();

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
  public void getUserById(GetUserByIdRequest request, StreamObserver<User> responseObserver) {
    try {
      long id = request.getId();
      if (id <= 0) {
        throw Status.INVALID_ARGUMENT.withDescription("ID should be valid.").asRuntimeException();
      }

      UserModel user =
          userRepo
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

      Instant createdAtInstant = user.getCreatedAt().atZone(ZoneOffset.UTC).toInstant();
      Instant updatedAtInstant = user.getUpdatedAt().atZone(ZoneOffset.UTC).toInstant();

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
  public void getUsers(GetUsersRequest request, StreamObserver<GetUsersResponse> responseObserver) {
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

      Set<String> allowedSortField =
          Set.of("id", "username", "email", "role", "authProvider", "createdAt", "updatedAt");
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
      Page<UserModel> userPage;

      userPage = userRepo.findAll(pageable);

      if (pageNo > 0 && userPage.getTotalPages() > 0 && pageNo >= userPage.getTotalPages()) {
        throw Status.INVALID_ARGUMENT
            .withDescription(
                "Page number " + pageNo + " exceeds total pages (" + userPage.getTotalPages() + ")")
            .asRuntimeException();
      }

      List<User> users =
          userPage.getContent().stream().map(this::mapToGrpcUser).collect(Collectors.toList());

      GetUsersResponse response =
          GetUsersResponse.newBuilder()
              .addAllUsers(users)
              .setPageNo(userPage.getNumber())
              .setPageSize(userPage.getSize())
              .setTotalElements(userPage.getTotalElements())
              .setTotalPages(userPage.getTotalPages())
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
  public void deleteUserById(
      DeleteUserRequest request, StreamObserver<DeleteUserResponse> responseObserver) {
    try {
      long id = request.getId();
      if (id <= 0) {
        throw Status.INVALID_ARGUMENT.withDescription("ID should be valid.").asRuntimeException();
      }

      int rowsAffected = userRepo.softDeleteById(id);

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
          userRepo
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

      String newAccessToken = jwtUtil.createToken(user.getId(), user.getEmail(), user.getRole());
      JwtUtil.RefreshTokenPair newRefreshToken =
          jwtUtil.createRefreshToken(user.getId(), user.getEmail(), user.getRole());

      user.setRefreshTokenJti(newRefreshToken.jti());
      userRepo.save(user);

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
    long id = request.getId();

    try {
      if (id <= 0) {
        throw Status.INVALID_ARGUMENT.withDescription("ID should be valid.").asRuntimeException();
      }

      UserModel user =
          userRepo
              .findById(id)
              .orElseThrow(
                  () ->
                      Status.NOT_FOUND.withDescription("Unable to find user").asRuntimeException());

      if (user.isDeleted()) {
        throw Status.PERMISSION_DENIED
            .withDescription("Account has been deactivated")
            .asRuntimeException();
      }

      user.setRefreshTokenJti(null);
      userRepo.save(user);

      LogoutUserResponse response =
          LogoutUserResponse.newBuilder()
              .setSuccess(true)
              .setMessage("Logout was successful")
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

  private User mapToGrpcUser(UserModel userModel) {
    UserRole role =
        switch (userModel.getRole().toUpperCase()) {
          case "ADMIN" -> UserRole.ADMIN;
          case "USER" -> UserRole.USER;
          default ->
              throw new IllegalArgumentException(
                  "Invalid role: " + userModel.getRole() + ". Must be USER or ADMIN");
        };

    AuthProvider authProvider =
        userModel.getAuthProvider().equalsIgnoreCase("LOCAL")
            ? AuthProvider.LOCAL
            : AuthProvider.GOOGLE;

    Instant createdAtInstant = userModel.getCreatedAt().toInstant(ZoneOffset.UTC);
    Timestamp createdAt =
        Timestamp.newBuilder()
            .setSeconds(createdAtInstant.getEpochSecond())
            .setNanos(createdAtInstant.getNano())
            .build();

    Instant updatedAtinstant = userModel.getCreatedAt().toInstant(ZoneOffset.UTC);
    Timestamp updatedAt =
        Timestamp.newBuilder()
            .setSeconds(updatedAtinstant.getEpochSecond())
            .setNanos(updatedAtinstant.getNano())
            .build();

    User user =
        User.newBuilder()
            .setId(userModel.getId())
            .setUsername(userModel.getUsername())
            .setEmail(userModel.getEmail())
            .setRole(role)
            .setAuthProvider(authProvider)
            .setIsDeleted(userModel.isDeleted())
            .setCreatedAt(createdAt)
            .setUpdatedAt(updatedAt)
            .build();

    return user;
  }
}
