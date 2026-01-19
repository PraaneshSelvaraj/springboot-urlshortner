package com.example.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtUtil {

  private final PublicKey publicKey;

  public JwtUtil(@Value("${jwt.rsa.public-key}") String publicKeyBase64) {
    try {
      byte[] keyBytes = Base64.getDecoder().decode(publicKeyBase64);
      X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
      KeyFactory keyFactory = KeyFactory.getInstance("RSA");
      this.publicKey = keyFactory.generatePublic(keySpec);
    } catch (Exception e) {
      throw new IllegalArgumentException("Failed to load RSA public key", e);
    }
  }

  public boolean validateToken(String token) {
    try {
      Jwts.parser().verifyWith(publicKey).build().parseSignedClaims(token);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  public String extractEmail(String token) {
    return Jwts.parser()
        .verifyWith(publicKey)
        .build()
        .parseSignedClaims(token)
        .getPayload()
        .getSubject();
  }

  public Long extractUserId(String token) {
    Claims claims = extractAllClaims(token);
    return claims.get("userId", Long.class);
  }

  public String extractRole(String token) {
    Claims claims = extractAllClaims(token);
    return claims.get("role", String.class);
  }

  public String extractTokenType(String token) {
    Claims claims = extractAllClaims(token);
    return claims.get("type", String.class);
  }

  private Claims extractAllClaims(String token) {
    return Jwts.parser().verifyWith(publicKey).build().parseSignedClaims(token).getPayload();
  }
}
