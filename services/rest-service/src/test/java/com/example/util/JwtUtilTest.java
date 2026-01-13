package com.example.util;

import static org.assertj.core.api.Assertions.assertThat;

import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.util.Date;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("JwtUtil Tests")
class JwtUtilTest {

  private JwtUtil jwtUtil;
  private String testSecret;

  @BeforeEach
  void setUp() {
    testSecret = "testSecretKeyThatIsAtLeast256BitsLongForHS256Algorithm12345678";
    jwtUtil = new JwtUtil(testSecret);
  }

  @Test
  @DisplayName("Should validate valid token successfully")
  void shouldValidateValidTokenSuccessfully() {
    String token = createTestToken(1L, "test@example.com", "USER", "auth", false);

    boolean isValid = jwtUtil.validateToken(token);

    assertThat(isValid).isTrue();
  }

  @Test
  @DisplayName("Should reject expired token")
  void shouldRejectExpiredToken() {
    String token = createTestToken(1L, "test@example.com", "USER", "auth", true);

    boolean isValid = jwtUtil.validateToken(token);

    assertThat(isValid).isFalse();
  }

  @Test
  @DisplayName("Should reject token with invalid signature")
  void shouldRejectTokenWithInvalidSignature() {
    String differentSecret = "differentSecretKeyThatIsAtLeast256BitsLongForHS256Algorithm1234";
    SecretKey key = Keys.hmacShaKeyFor(differentSecret.getBytes());
    String token =
        Jwts.builder()
            .subject("test@example.com")
            .claim("userId", 1L)
            .claim("role", "USER")
            .claim("type", "auth")
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + 3600000))
            .signWith(key)
            .compact();

    boolean isValid = jwtUtil.validateToken(token);

    assertThat(isValid).isFalse();
  }

  @Test
  @DisplayName("Should reject malformed token")
  void shouldRejectMalformedToken() {
    String malformedToken = "invalid.token.format";

    boolean isValid = jwtUtil.validateToken(malformedToken);

    assertThat(isValid).isFalse();
  }

  @Test
  @DisplayName("Should reject null token")
  void shouldRejectNullToken() {
    boolean isValid = jwtUtil.validateToken(null);

    assertThat(isValid).isFalse();
  }

  @Test
  @DisplayName("Should extract email from token")
  void shouldExtractEmailFromToken() {
    String email = "user@example.com";
    String token = createTestToken(1L, email, "USER", "auth", false);

    String extractedEmail = jwtUtil.extractEmail(token);

    assertThat(extractedEmail).isEqualTo(email);
  }

  @Test
  @DisplayName("Should extract user ID from token")
  void shouldExtractUserIdFromToken() {
    Long userId = 42L;
    String token = createTestToken(userId, "test@example.com", "USER", "auth", false);

    Long extractedUserId = jwtUtil.extractUserId(token);

    assertThat(extractedUserId).isEqualTo(userId);
  }

  @Test
  @DisplayName("Should extract role from token")
  void shouldExtractRoleFromToken() {
    String role = "ADMIN";
    String token = createTestToken(1L, "admin@example.com", role, "auth", false);

    String extractedRole = jwtUtil.extractRole(token);

    assertThat(extractedRole).isEqualTo(role);
  }

  @Test
  @DisplayName("Should extract token type from token")
  void shouldExtractTokenTypeFromToken() {
    String token = createTestToken(1L, "test@example.com", "USER", "auth", false);

    String tokenType = jwtUtil.extractTokenType(token);

    assertThat(tokenType).isEqualTo("auth");
  }

  @Test
  @DisplayName("Should handle token without userId claim")
  void shouldHandleTokenWithoutUserIdClaim() {
    SecretKey key = Keys.hmacShaKeyFor(testSecret.getBytes());
    String token =
        Jwts.builder()
            .subject("test@example.com")
            .claim("role", "USER")
            .claim("type", "auth")
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + 3600000))
            .signWith(key)
            .compact();

    Long userId = jwtUtil.extractUserId(token);

    assertThat(userId).isNull();
  }

  @Test
  @DisplayName("Should extract ADMIN role correctly")
  void shouldExtractAdminRoleCorrectly() {
    String token = createTestToken(1L, "admin@example.com", "ADMIN", "auth", false);

    String role = jwtUtil.extractRole(token);

    assertThat(role).isEqualTo("ADMIN");
  }

  @Test
  @DisplayName("Should extract USER role correctly")
  void shouldExtractUserRoleCorrectly() {
    String token = createTestToken(1L, "user@example.com", "USER", "auth", false);

    String role = jwtUtil.extractRole(token);

    assertThat(role).isEqualTo("USER");
  }

  @Test
  @DisplayName("Should extract refresh token type")
  void shouldExtractRefreshTokenType() {
    String token = createTestToken(1L, "test@example.com", "USER", "refresh", false);

    String tokenType = jwtUtil.extractTokenType(token);

    assertThat(tokenType).isEqualTo("refresh");
  }

  private String createTestToken(
      Long userId, String email, String role, String type, boolean expired) {
    SecretKey key = Keys.hmacShaKeyFor(testSecret.getBytes());
    Date issuedAt = new Date();
    Date expiration =
        expired
            ? new Date(System.currentTimeMillis() - 1000)
            : new Date(System.currentTimeMillis() + 3600000);

    JwtBuilder builder =
        Jwts.builder().subject(email).issuedAt(issuedAt).expiration(expiration).signWith(key);

    if (userId != null) {
      builder.claim("userId", userId);
    }
    if (role != null) {
      builder.claim("role", role);
    }
    if (type != null) {
      builder.claim("type", type);
    }

    return builder.compact();
  }
}
