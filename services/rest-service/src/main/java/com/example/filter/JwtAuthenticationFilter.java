package com.example.filter;

import com.example.security.UserPrincipal;
import com.example.service.TokenBlacklistService;
import com.example.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtUtil jwtUtil;
  private final TokenBlacklistService tokenBlacklistService;

  public JwtAuthenticationFilter(JwtUtil jwtUtil, TokenBlacklistService tokenBlacklistService) {
    this.jwtUtil = jwtUtil;
    this.tokenBlacklistService = tokenBlacklistService;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    try {
      String authHeader = request.getHeader("Authorization");
      if (authHeader == null || !authHeader.startsWith("Bearer ")) {
        filterChain.doFilter(request, response);
        return;
      }

      String token = authHeader.substring(7);
      if (tokenBlacklistService.isTokenBlacklisted(token)) {
        filterChain.doFilter(request, response);
        return;
      }

      if (jwtUtil.validateToken(token)) {
        Long userId = jwtUtil.extractUserId(token);
        String email = jwtUtil.extractEmail(token);
        String role = jwtUtil.extractRole(token);
        String tokenType = jwtUtil.extractTokenType(token);

        if (!tokenType.equalsIgnoreCase("auth")) {
          filterChain.doFilter(request, response);
          return;
        }

        String roleWithPrefix = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        List<SimpleGrantedAuthority> authorities =
            List.of(new SimpleGrantedAuthority(roleWithPrefix));

        UserPrincipal principal = new UserPrincipal(userId, email, role);

        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(principal, token, authorities);

        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        SecurityContextHolder.getContext().setAuthentication(authentication);
      }
    } catch (Exception e) {
    }

    filterChain.doFilter(request, response);
  }
}
