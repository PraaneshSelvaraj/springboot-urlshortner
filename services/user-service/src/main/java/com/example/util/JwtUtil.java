package com.example.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtUtil {

  public record RefreshTokenPair(String token, String jti) {}

  private final SecretKey secretKey;

  @Value("${jwt.access-token.expiration}")
  private long accessTokenExpiration;

  @Value("${jwt.refresh-token.expiration}")
  private long refreshTokenExpiration;

  public JwtUtil(@Value("${jwt.secret}") String secret) {
    this.secretKey = Keys.hmacShaKeyFor(secret.getBytes());
  }

  public String createToken(String email, String role) {
    return Jwts.builder()
        .subject(email)
        .id(UUID.randomUUID().toString())
        .claim("role", role)
        .issuedAt(new Date())
        .expiration(new Date(System.currentTimeMillis() + accessTokenExpiration))
        .signWith(secretKey)
        .compact();
  }

  public RefreshTokenPair createRefreshToken(String email, String role) {
    String jti = UUID.randomUUID().toString();
    String token =
        Jwts.builder()
            .subject(email)
            .id(jti)
            .claim("type", "refresh")
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + refreshTokenExpiration))
            .signWith(secretKey)
            .compact();
    return new RefreshTokenPair(token, jti);
  }

  public String extractEmail(String token) {
    return Jwts.parser()
        .verifyWith(secretKey)
        .build()
        .parseSignedClaims(token)
        .getPayload()
        .getSubject();
  }

  public String extractJti(String token) {
    return Jwts.parser()
        .verifyWith(secretKey)
        .build()
        .parseSignedClaims(token)
        .getPayload()
        .getId();
  }

  public boolean validateToken(String token) {
    try {
      Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token);
      return true;
    } catch (Exception e) {
      return false;
    }
  }
}
