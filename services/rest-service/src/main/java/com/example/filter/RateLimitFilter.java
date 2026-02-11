package com.example.filter;

import com.example.config.RateLimitRule;
import com.example.exception.RateLimitExceededException;
import com.example.service.RateLimitService;
import com.example.util.UserContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

  private final RateLimitService rateLimitService;
  private final List<RateLimitRule> rateLimitRules;
  private final AntPathMatcher pathMatcher;

  public RateLimitFilter(
      RateLimitService rateLimitService,
      List<RateLimitRule> rateLimitRules,
      AntPathMatcher pathMatcher) {
    this.rateLimitService = rateLimitService;
    this.rateLimitRules = rateLimitRules;
    this.pathMatcher = pathMatcher;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String path = request.getServletPath();
    String method = request.getMethod();

    RateLimitRule matchedRule = findMatchingRule(path, method);

    if (matchedRule != null) {
      String identifier = getIdentifier(request);
      String redisKey = buildRedisKey(matchedRule.getKeyPrefix(), method, identifier);

      boolean isLimited =
          rateLimitService.isRateLimited(
              redisKey, matchedRule.getMaxRequests(), matchedRule.getWindowSeconds());

      if (isLimited) {
        throw new RateLimitExceededException(
            String.format(
                "Rate limit exceeded. Maximum %d requests per %d seconds.",
                matchedRule.getMaxRequests(), matchedRule.getWindowSeconds()));
      }
    }

    filterChain.doFilter(request, response);
  }

  private RateLimitRule findMatchingRule(String path, String method) {
    return rateLimitRules.stream()
        .filter(rule -> rule.getMethod().matches(method))
        .filter(rule -> pathMatcher.match(rule.getPathPattern(), path))
        .findFirst()
        .orElse(null);
  }

  private String getIdentifier(HttpServletRequest request) {
    try {
      Long userId = UserContext.getCurrentUserId();
      if (userId != null) {
        return userId.toString();
      }
    } catch (Exception e) {
      return request.getRemoteAddr();
    }

    return request.getRemoteAddr();
  }

  private String buildRedisKey(String prefix, String method, String identifier) {
    return String.format("%s:%s:%s", prefix, method, identifier);
  }
}
