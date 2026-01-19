package com.example.util;

import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * Utility class for creating JWT tokens in tests
 */
public class TestJwtUtil {

    /**
     * Creates a JWT token with specified parameters
     *
     * @param secret   The secret key to sign the token (minimum 256 bits)
     * @param userId   The user ID claim
     * @param email    The email (subject)
     * @param role     The role claim (USER or ADMIN)
     * @param type     The token type (auth or refresh)
     * @param expired  Whether the token should be expired
     * @return The JWT token string
     */
    public static String createTestToken(String secret, Long userId, String email,
                                          String role, String type, boolean expired) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes());
        Date issuedAt = new Date();
        Date expiration = expired
            ? new Date(System.currentTimeMillis() - 1000)
            : new Date(System.currentTimeMillis() + 3600000); // 1 hour from now

        JwtBuilder builder = Jwts.builder()
            .subject(email)
            .issuedAt(issuedAt)
            .expiration(expiration)
            .signWith(key);

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

    /**
     * Creates a valid auth token with default expiration
     *
     * @param secret The secret key
     * @param userId The user ID
     * @param email  The user email
     * @param role   The user role
     * @return The JWT token string
     */
    public static String createValidAuthToken(String secret, Long userId, String email, String role) {
        return createTestToken(secret, userId, email, role, "auth", false);
    }

    /**
     * Creates a valid refresh token with default expiration
     *
     * @param secret The secret key
     * @param userId The user ID
     * @param email  The user email
     * @param role   The user role
     * @return The JWT token string
     */
    public static String createValidRefreshToken(String secret, Long userId, String email, String role) {
        return createTestToken(secret, userId, email, role, "refresh", false);
    }

    /**
     * Creates an expired token
     *
     * @param secret The secret key
     * @param userId The user ID
     * @param email  The user email
     * @param role   The user role
     * @param type   The token type
     * @return The expired JWT token string
     */
    public static String createExpiredToken(String secret, Long userId, String email, String role, String type) {
        return createTestToken(secret, userId, email, role, type, true);
    }
}
