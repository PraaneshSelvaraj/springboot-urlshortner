package com.example.util;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.util.JwtUtil.RefreshTokenPair;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("JwtUtil Tests")
class JwtUtilTest {

  private JwtUtil jwtUtil;
  private PublicKey testPublicKey;
  private PrivateKey testPrivateKey;

  @BeforeEach
  void setUp() throws Exception {
    // Generate test RSA key pair
    KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
    keyGen.initialize(2048);
    KeyPair pair = keyGen.generateKeyPair();

    testPrivateKey = pair.getPrivate();
    testPublicKey = pair.getPublic();

    // Create JwtUtil with private key
    String privateKeyBase64 = Base64.getEncoder().encodeToString(testPrivateKey.getEncoded());
    jwtUtil = new JwtUtil(privateKeyBase64);

    // Set expiration times via reflection
    ReflectionTestUtils.setField(jwtUtil, "accessTokenExpiration", 3600000L);
    ReflectionTestUtils.setField(jwtUtil, "refreshTokenExpiration", 604800000L);
  }

  @Test
  @DisplayName("Should create valid access token")
  void shouldCreateValidAccessToken() {
    String token = jwtUtil.createToken(1L, "test@example.com", "USER");

    assertThat(token).isNotNull();

    // Verify token can be parsed with public key
    Claims claims =
        Jwts.parser()
            .verifyWith(testPublicKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();

    assertThat(claims.getSubject()).isEqualTo("test@example.com");
    assertThat(claims.get("userId", Long.class)).isEqualTo(1L);
    assertThat(claims.get("role", String.class)).isEqualTo("USER");
    assertThat(claims.get("type", String.class)).isEqualTo("auth");
  }

  @Test
  @DisplayName("Should create valid refresh token with JTI")
  void shouldCreateValidRefreshTokenWithJti() {
    RefreshTokenPair pair = jwtUtil.createRefreshToken(2L, "test@example.com", "ADMIN");

    assertThat(pair.token()).isNotNull();
    assertThat(pair.jti()).isNotNull();

    // Verify token can be parsed
    Claims claims =
        Jwts.parser()
            .verifyWith(testPublicKey)
            .build()
            .parseSignedClaims(pair.token())
            .getPayload();

    assertThat(claims.getId()).isEqualTo(pair.jti());
    assertThat(claims.getSubject()).isEqualTo("test@example.com");
    assertThat(claims.get("userId", Long.class)).isEqualTo(2L);
    assertThat(claims.get("role", String.class)).isEqualTo("ADMIN");
    assertThat(claims.get("type", String.class)).isEqualTo("refresh");
  }

  @Test
  @DisplayName("Should validate token successfully")
  void shouldValidateTokenSuccessfully() {
    String token = jwtUtil.createToken(1L, "test@example.com", "USER");

    boolean isValid = jwtUtil.validateToken(token);

    assertThat(isValid).isTrue();
  }

  @Test
  @DisplayName("Should extract email from token")
  void shouldExtractEmailFromToken() {
    String token = jwtUtil.createToken(1L, "user@example.com", "USER");

    String email = jwtUtil.extractEmail(token);

    assertThat(email).isEqualTo("user@example.com");
  }

  @Test
  @DisplayName("Should extract JTI from refresh token")
  void shouldExtractJtiFromRefreshToken() {
    RefreshTokenPair pair = jwtUtil.createRefreshToken(1L, "test@example.com", "USER");

    String jti = jwtUtil.extractJti(pair.token());

    assertThat(jti).isEqualTo(pair.jti());
  }

  @Test
  @DisplayName("Should reject token with invalid signature")
  void shouldRejectTokenWithInvalidSignature() throws Exception {
    // Generate different key pair
    KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
    keyGen.initialize(2048);
    KeyPair differentPair = keyGen.generateKeyPair();

    // Create token with different private key
    String token =
        Jwts.builder()
            .subject("test@example.com")
            .claim("userId", 1L)
            .claim("role", "USER")
            .claim("type", "auth")
            .signWith(differentPair.getPrivate(), SignatureAlgorithm.RS256)
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
}
