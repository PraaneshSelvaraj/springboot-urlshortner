package com.example.util;

import io.jsonwebtoken.Jwts;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtUtil {

  public record RefreshTokenPair(String token, String jti) {}

  private final PrivateKey privateKey;
  private final PublicKey publicKey;

  @Value("${jwt.access-token.expiration}")
  private long accessTokenExpiration;

  @Value("${jwt.refresh-token.expiration}")
  private long refreshTokenExpiration;

  public JwtUtil(@Value("${jwt.rsa.private-key}") String privateKeyBase64) {
    try {
      byte[] keyBytes = Base64.getDecoder().decode(privateKeyBase64);
      PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
      KeyFactory keyFactory = KeyFactory.getInstance("RSA");
      this.privateKey = keyFactory.generatePrivate(keySpec);

      RSAPrivateCrtKey rsaPrivateKey = (RSAPrivateCrtKey) this.privateKey;
      RSAPublicKeySpec publicKeySpec =
          new RSAPublicKeySpec(rsaPrivateKey.getModulus(), rsaPrivateKey.getPublicExponent());
      this.publicKey = keyFactory.generatePublic(publicKeySpec);
    } catch (Exception e) {
      throw new IllegalArgumentException("Failed to load RSA private key", e);
    }
  }

  public String createToken(Long userId, String email, String role) {
    return Jwts.builder()
        .subject(email)
        .id(UUID.randomUUID().toString())
        .claim("userId", userId)
        .claim("role", role)
        .claim("type", "auth")
        .issuedAt(new Date())
        .expiration(new Date(System.currentTimeMillis() + accessTokenExpiration))
        .signWith(privateKey, Jwts.SIG.RS256)
        .compact();
  }

  public RefreshTokenPair createRefreshToken(Long userId, String email, String role) {
    String jti = UUID.randomUUID().toString();
    String token =
        Jwts.builder()
            .subject(email)
            .id(jti)
            .claim("userId", userId)
            .claim("role", role)
            .claim("type", "refresh")
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + refreshTokenExpiration))
            .signWith(privateKey, Jwts.SIG.RS256)
            .compact();
    return new RefreshTokenPair(token, jti);
  }

  public String extractEmail(String token) {
    return Jwts.parser()
        .verifyWith(publicKey)
        .build()
        .parseSignedClaims(token)
        .getPayload()
        .getSubject();
  }

  public String extractJti(String token) {
    return Jwts.parser()
        .verifyWith(publicKey)
        .build()
        .parseSignedClaims(token)
        .getPayload()
        .getId();
  }

  public boolean validateToken(String token) {
    try {
      Jwts.parser().verifyWith(publicKey).build().parseSignedClaims(token);
      return true;
    } catch (Exception e) {
      return false;
    }
  }
}
