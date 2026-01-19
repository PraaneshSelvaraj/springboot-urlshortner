package com.example.context;

import io.grpc.Context;
import lombok.AllArgsConstructor;
import lombok.Data;

public class GrpcUserContext {

  public static final Context.Key<UserInfo> USER_INFO_KEY = Context.key("user-info");

  @Data
  @AllArgsConstructor
  public static class UserInfo {
    private final Long userId;
    private final String email;
    private final String role;
  }

  public static UserInfo getCurrentUser() {
    return USER_INFO_KEY.get();
  }

  public static Long getCurrentUserId() {
    UserInfo userInfo = getCurrentUser();
    return userInfo != null ? userInfo.getUserId() : null;
  }

  public static String getCurrentUserEmail() {
    UserInfo userInfo = getCurrentUser();
    return userInfo != null ? userInfo.getEmail() : null;
  }

  public static String getCurrentUserRole() {
    UserInfo userInfo = getCurrentUser();
    return userInfo != null ? userInfo.getRole() : null;
  }

  public static boolean isAuthenticated() {
    return getCurrentUser() != null;
  }
}
