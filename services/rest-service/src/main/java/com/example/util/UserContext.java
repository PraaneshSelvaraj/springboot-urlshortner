package com.example.util;

import com.example.security.UserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Utility class for accessing the current authenticated user's information from Spring Security
 * context.
 *
 * <p>This replaces the old pattern of: <code>
 * UserContext user = (UserContext) request.getAttribute("authenticatedUser");
 * </code>
 *
 * <p>New pattern: <code>
 * Long userId = UserContext.getCurrentUserId();
 * String email = UserContext.getCurrentUserEmail();
 * </code>
 *
 * <p>Or in controllers, you can use @AuthenticationPrincipal: <code>
 * public ResponseEntity<?> someMethod(@AuthenticationPrincipal UserPrincipal principal) {
 *     Long userId = principal.getUserId();
 * }
 * </code>
 */
public class UserContext {

  /**
   * Gets the currently authenticated user's principal.
   *
   * @return UserPrincipal or null if not authenticated
   */
  public static UserPrincipal getCurrentUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
      return (UserPrincipal) authentication.getPrincipal();
    }

    return null;
  }

  /**
   * Gets the current user's ID.
   *
   * @return userId or null if not authenticated
   */
  public static Long getCurrentUserId() {
    UserPrincipal principal = getCurrentUser();
    return principal != null ? principal.getUserId() : null;
  }

  /**
   * Gets the current user's email.
   *
   * @return email or null if not authenticated
   */
  public static String getCurrentUserEmail() {
    UserPrincipal principal = getCurrentUser();
    return principal != null ? principal.getEmail() : null;
  }

  /**
   * Gets the current user's role.
   *
   * @return role or null if not authenticated
   */
  public static String getCurrentUserRole() {
    UserPrincipal principal = getCurrentUser();
    return principal != null ? principal.getRole() : null;
  }

  /**
   * Checks if a user is currently authenticated.
   *
   * @return true if authenticated, false otherwise
   */
  public static boolean isAuthenticated() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return authentication != null
        && authentication.isAuthenticated()
        && authentication.getPrincipal() instanceof UserPrincipal;
  }
}
