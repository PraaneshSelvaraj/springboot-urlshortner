package com.example.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.controller.UrlController;
import com.example.service.UrlService;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UrlController.class)
@DisplayName("GlobalExceptionHandler Tests")
class GlobalExceptionHandlerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private UrlService urlService;

  @Test
  @DisplayName("Should handle InvalidUrlException and return 400 BAD REQUEST")
  void shouldHandleInvalidUrlExceptionAndReturn400() throws Exception {
    String invalidUrl = "not-a-valid-url";

    Mockito.when(urlService.addUrl(Mockito.anyString())).thenThrow(new InvalidUrlException());

    String requestBody = "{\"url\":\"" + invalidUrl + "\"}";

    mockMvc
        .perform(post("/api/urls").contentType("application/json").content(requestBody))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.message").value("Invalid URL provided"));
  }

  @Test
  @DisplayName("Should handle NoSuchElementException and return 404 NOT FOUND")
  void shouldHandleNoSuchElementExceptionAndReturn404() throws Exception {
    String shortCode = "notexists";

    Mockito.when(urlService.getUrlByShortCode(shortCode))
        .thenThrow(new NoSuchElementException("URL not found"));

    mockMvc
        .perform(get("/api/urls/{shortCode}", shortCode))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.message").value("URL not found"));
  }

  @Test
  @DisplayName("Should handle UrlExpiredException and return 410 GONE")
  void shouldHandleUrlExpiredExceptionAndReturn410() throws Exception {
    String shortCode = "expired123";

    Mockito.when(urlService.redirect(shortCode)).thenThrow(new UrlExpiredException());

    mockMvc
        .perform(get("/{shortCode}", shortCode))
        .andExpect(status().isGone())
        .andExpect(jsonPath("$.status").value(410))
        .andExpect(jsonPath("$.message").value("URL has expired"));
  }

  @Test
  @DisplayName("Should handle ThresholdReachedException and return 429 TOO MANY REQUESTS")
  void shouldHandleThresholdReachedExceptionAndReturn429() throws Exception {
    String shortCode = "popular123";

    Mockito.when(urlService.redirect(shortCode)).thenThrow(new ThresholdReachedException());

    mockMvc
        .perform(get("/{shortCode}", shortCode))
        .andExpect(status().isTooManyRequests())
        .andExpect(jsonPath("$.status").value(429))
        .andExpect(jsonPath("$.message").value("URL has reached the click threshold"));
  }

  @Test
  @DisplayName("Should handle generic Exception and return 500 INTERNAL SERVER ERROR")
  void shouldHandleGenericExceptionAndReturn500() throws Exception {
    Mockito.when(urlService.getUrls(Mockito.anyInt(), Mockito.anyInt()))
        .thenThrow(new RuntimeException("Unexpected error"));

    mockMvc
        .perform(get("/api/urls").param("pageNo", "0").param("pageSize", "10"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.status").value(500))
        .andExpect(jsonPath("$.message").value("An unexpected error occurred: Unexpected error"));
  }

  @Test
  @DisplayName("Should handle IllegalArgumentException and return 500 INTERNAL SERVER ERROR")
  void shouldHandleIllegalArgumentExceptionAndReturn500() throws Exception {
    mockMvc
        .perform(get("/api/urls").param("pageNo", "-1").param("pageSize", "10"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.status").value(500))
        .andExpect(
            jsonPath("$.message")
                .value("An unexpected error occurred: Page number cannot be negative"));
  }

  @Test
  @DisplayName("Should return error response with correct structure for InvalidUrlException")
  void shouldReturnErrorResponseWithCorrectStructureForInvalidUrl() throws Exception {
    Mockito.when(urlService.addUrl(Mockito.anyString())).thenThrow(new InvalidUrlException());

    String requestBody = "{\"url\":\"invalid\"}";

    mockMvc
        .perform(post("/api/urls").contentType("application/json").content(requestBody))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").exists())
        .andExpect(jsonPath("$.message").exists())
        .andExpect(jsonPath("$.status").isNumber())
        .andExpect(jsonPath("$.message").isString());
  }

  @Test
  @DisplayName("Should handle NoSuchElementException when deleting URL")
  void shouldHandleNoSuchElementExceptionWhenDeletingUrl() throws Exception {
    String shortCode = "notexists";

    Mockito.doThrow(new NoSuchElementException("URL not found"))
        .when(urlService)
        .deleteUrl(shortCode);

    mockMvc
        .perform(delete("/api/urls/{shortCode}", shortCode))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.message").value("URL not found"));
  }

  @Test
  @DisplayName("Should handle multiple NoSuchElementException scenarios with different messages")
  void shouldHandleNoSuchElementExceptionWithDifferentMessages() throws Exception {
    String shortCode1 = "abc123";
    String shortCode2 = "xyz789";

    Mockito.when(urlService.getUrlByShortCode(shortCode1))
        .thenThrow(new NoSuchElementException("URL with shortcode abc123 does not exist"));

    Mockito.when(urlService.getUrlByShortCode(shortCode2))
        .thenThrow(new NoSuchElementException("URL with shortcode xyz789 does not exist"));

    mockMvc
        .perform(get("/api/urls/{shortCode}", shortCode1))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("URL with shortcode abc123 does not exist"));

    mockMvc
        .perform(get("/api/urls/{shortCode}", shortCode2))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("URL with shortcode xyz789 does not exist"));
  }

  @Test
  @DisplayName("Should preserve exception message in error response")
  void shouldPreserveExceptionMessageInErrorResponse() throws Exception {
    String customMessage = "This is a custom error message";

    Mockito.when(urlService.getUrls(Mockito.anyInt(), Mockito.anyInt()))
        .thenThrow(new RuntimeException(customMessage));

    mockMvc
        .perform(get("/api/urls").param("pageNo", "0").param("pageSize", "10"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.message").value("An unexpected error occurred: " + customMessage));
  }
}
