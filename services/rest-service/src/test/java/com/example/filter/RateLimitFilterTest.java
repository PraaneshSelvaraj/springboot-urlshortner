package com.example.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.example.config.RateLimitRule;
import com.example.exception.RateLimitExceededException;
import com.example.service.RateLimitService;
import com.example.util.UserContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.util.AntPathMatcher;

@ExtendWith(MockitoExtension.class)
@DisplayName("RateLimitFilter Tests")
class RateLimitFilterTest {

  @Mock private RateLimitService rateLimitService;

  @Mock private HttpServletRequest request;

  @Mock private HttpServletResponse response;

  @Mock private FilterChain filterChain;

  private RateLimitFilter rateLimitFilter;
  private List<RateLimitRule> rateLimitRules;
  private AntPathMatcher pathMatcher;
  private MockedStatic<UserContext> userContextMock;

  @BeforeEach
  void setUp() {
    pathMatcher = new AntPathMatcher();
    rateLimitRules =
        List.of(
            RateLimitRule.builder()
                .method(HttpMethod.POST)
                .pathPattern("/api/urls")
                .maxRequests(10)
                .windowSeconds(60)
                .keyPrefix("ratelimit:url:create")
                .description("URL creation rate limit")
                .build(),
            RateLimitRule.builder()
                .method(HttpMethod.DELETE)
                .pathPattern("/api/urls/**")
                .maxRequests(10)
                .windowSeconds(60)
                .keyPrefix("ratelimit:url:delete")
                .description("URL deletion rate limit")
                .build());

    rateLimitFilter = new RateLimitFilter(rateLimitService, rateLimitRules, pathMatcher);

    userContextMock = mockStatic(UserContext.class);
  }

  @AfterEach
  void tearDown() {
    if (userContextMock != null) {
      userContextMock.close();
    }
  }

  @Test
  @DisplayName("Should allow request when rate limit not exceeded")
  void shouldAllowRequestWhenRateLimitNotExceeded() throws Exception {
    when(request.getServletPath()).thenReturn("/api/urls");
    when(request.getMethod()).thenReturn("POST");
    userContextMock.when(UserContext::getCurrentUserId).thenReturn(123L);
    when(rateLimitService.isRateLimited(anyString(), eq(10), eq(60))).thenReturn(false);

    rateLimitFilter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    verify(rateLimitService).isRateLimited("ratelimit:url:create:POST:123", 10, 60);
  }

  @Test
  @DisplayName("Should throw RateLimitExceededException when rate limit exceeded")
  void shouldThrowRateLimitExceededExceptionWhenRateLimitExceeded() throws Exception {
    when(request.getServletPath()).thenReturn("/api/urls");
    when(request.getMethod()).thenReturn("POST");
    userContextMock.when(UserContext::getCurrentUserId).thenReturn(123L);
    when(rateLimitService.isRateLimited(anyString(), eq(10), eq(60))).thenReturn(true);

    assertThatThrownBy(() -> rateLimitFilter.doFilterInternal(request, response, filterChain))
        .isInstanceOf(RateLimitExceededException.class)
        .hasMessageContaining("Rate limit exceeded")
        .hasMessageContaining("10 requests per 60 seconds");

    verify(filterChain, never()).doFilter(request, response);
  }

  @Test
  @DisplayName("Should use user ID from UserContext when available")
  void shouldUseUserIdFromUserContextWhenAvailable() throws Exception {
    when(request.getServletPath()).thenReturn("/api/urls");
    when(request.getMethod()).thenReturn("POST");
    userContextMock.when(UserContext::getCurrentUserId).thenReturn(999L);
    when(rateLimitService.isRateLimited(anyString(), eq(10), eq(60))).thenReturn(false);

    rateLimitFilter.doFilterInternal(request, response, filterChain);

    ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
    verify(rateLimitService).isRateLimited(keyCaptor.capture(), eq(10), eq(60));
    assertThat(keyCaptor.getValue()).isEqualTo("ratelimit:url:create:POST:999");
  }

  @Test
  @DisplayName("Should fallback to IP address when UserContext not available")
  void shouldFallbackToIpAddressWhenUserContextNotAvailable() throws Exception {
    when(request.getServletPath()).thenReturn("/api/urls");
    when(request.getMethod()).thenReturn("POST");
    when(request.getRemoteAddr()).thenReturn("192.168.1.1");
    userContextMock.when(UserContext::getCurrentUserId).thenThrow(new RuntimeException());
    when(rateLimitService.isRateLimited(anyString(), eq(10), eq(60))).thenReturn(false);

    rateLimitFilter.doFilterInternal(request, response, filterChain);

    ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
    verify(rateLimitService).isRateLimited(keyCaptor.capture(), eq(10), eq(60));
    assertThat(keyCaptor.getValue()).isEqualTo("ratelimit:url:create:POST:192.168.1.1");
  }

  @Test
  @DisplayName("Should fallback to IP address when UserContext returns null")
  void shouldFallbackToIpAddressWhenUserContextReturnsNull() throws Exception {
    when(request.getServletPath()).thenReturn("/api/urls");
    when(request.getMethod()).thenReturn("POST");
    when(request.getRemoteAddr()).thenReturn("10.0.0.1");
    userContextMock.when(UserContext::getCurrentUserId).thenReturn(null);
    when(rateLimitService.isRateLimited(anyString(), eq(10), eq(60))).thenReturn(false);

    rateLimitFilter.doFilterInternal(request, response, filterChain);

    ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
    verify(rateLimitService).isRateLimited(keyCaptor.capture(), eq(10), eq(60));
    assertThat(keyCaptor.getValue()).isEqualTo("ratelimit:url:create:POST:10.0.0.1");
  }

  @Test
  @DisplayName("Should match DELETE request with wildcard pattern")
  void shouldMatchDeleteRequestWithWildcardPattern() throws Exception {
    when(request.getServletPath()).thenReturn("/api/urls/abc123");
    when(request.getMethod()).thenReturn("DELETE");
    userContextMock.when(UserContext::getCurrentUserId).thenReturn(123L);
    when(rateLimitService.isRateLimited(anyString(), eq(10), eq(60))).thenReturn(false);

    rateLimitFilter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    verify(rateLimitService).isRateLimited("ratelimit:url:delete:DELETE:123", 10, 60);
  }

  @Test
  @DisplayName("Should not apply rate limiting to non-matching paths")
  void shouldNotApplyRateLimitingToNonMatchingPaths() throws Exception {
    when(request.getServletPath()).thenReturn("/api/auth/login");
    when(request.getMethod()).thenReturn("POST");

    rateLimitFilter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    verify(rateLimitService, never()).isRateLimited(anyString(), anyInt(), anyInt());
  }

  @Test
  @DisplayName("Should not apply rate limiting to GET requests on /api/urls")
  void shouldNotApplyRateLimitingToGetRequests() throws Exception {
    when(request.getServletPath()).thenReturn("/api/urls");
    when(request.getMethod()).thenReturn("GET");

    rateLimitFilter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    verify(rateLimitService, never()).isRateLimited(anyString(), anyInt(), anyInt());
  }

  @Test
  @DisplayName("Should handle multiple different users independently")
  void shouldHandleMultipleDifferentUsersIndependently() throws Exception {
    when(request.getServletPath()).thenReturn("/api/urls");
    when(request.getMethod()).thenReturn("POST");
    when(rateLimitService.isRateLimited(anyString(), eq(10), eq(60))).thenReturn(false);

    userContextMock.when(UserContext::getCurrentUserId).thenReturn(100L);
    rateLimitFilter.doFilterInternal(request, response, filterChain);

    userContextMock.when(UserContext::getCurrentUserId).thenReturn(200L);
    rateLimitFilter.doFilterInternal(request, response, filterChain);

    verify(rateLimitService).isRateLimited("ratelimit:url:create:POST:100", 10, 60);
    verify(rateLimitService).isRateLimited("ratelimit:url:create:POST:200", 10, 60);
  }

  @Test
  @DisplayName("Should use correct key prefix for different endpoints")
  void shouldUseCorrectKeyPrefixForDifferentEndpoints() throws Exception {
    when(rateLimitService.isRateLimited(anyString(), anyInt(), anyInt())).thenReturn(false);
    userContextMock.when(UserContext::getCurrentUserId).thenReturn(123L);

    when(request.getServletPath()).thenReturn("/api/urls");
    when(request.getMethod()).thenReturn("POST");
    rateLimitFilter.doFilterInternal(request, response, filterChain);

    ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
    verify(rateLimitService).isRateLimited(keyCaptor.capture(), eq(10), eq(60));
    assertThat(keyCaptor.getValue()).startsWith("ratelimit:url:create");

    reset(rateLimitService);
    when(rateLimitService.isRateLimited(anyString(), anyInt(), anyInt())).thenReturn(false);

    when(request.getServletPath()).thenReturn("/api/urls/abc123");
    when(request.getMethod()).thenReturn("DELETE");
    rateLimitFilter.doFilterInternal(request, response, filterChain);

    verify(rateLimitService).isRateLimited(keyCaptor.capture(), eq(10), eq(60));
    assertThat(keyCaptor.getValue()).startsWith("ratelimit:url:delete");
  }

  @Test
  @DisplayName("Should include HTTP method in Redis key")
  void shouldIncludeHttpMethodInRedisKey() throws Exception {
    when(request.getServletPath()).thenReturn("/api/urls");
    when(request.getMethod()).thenReturn("POST");
    userContextMock.when(UserContext::getCurrentUserId).thenReturn(123L);
    when(rateLimitService.isRateLimited(anyString(), eq(10), eq(60))).thenReturn(false);

    rateLimitFilter.doFilterInternal(request, response, filterChain);

    ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
    verify(rateLimitService).isRateLimited(keyCaptor.capture(), eq(10), eq(60));
    assertThat(keyCaptor.getValue()).contains(":POST:");
  }

  @Test
  @DisplayName("Should match exact path without wildcard")
  void shouldMatchExactPathWithoutWildcard() throws Exception {
    when(request.getServletPath()).thenReturn("/api/urls");
    when(request.getMethod()).thenReturn("POST");
    userContextMock.when(UserContext::getCurrentUserId).thenReturn(123L);
    when(rateLimitService.isRateLimited(anyString(), eq(10), eq(60))).thenReturn(false);

    rateLimitFilter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    verify(rateLimitService).isRateLimited(anyString(), eq(10), eq(60));
  }

  @Test
  @DisplayName("Should not match /api/urls/abc123 with POST method")
  void shouldNotMatchUrlsWithPathParamForPost() throws Exception {
    when(request.getServletPath()).thenReturn("/api/urls/abc123");
    when(request.getMethod()).thenReturn("POST");

    rateLimitFilter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    verify(rateLimitService, never()).isRateLimited(anyString(), anyInt(), anyInt());
  }
}
