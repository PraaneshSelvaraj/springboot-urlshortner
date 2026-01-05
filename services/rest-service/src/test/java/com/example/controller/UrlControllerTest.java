package com.example.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.dto.UrlDto;
import com.example.model.Url;
import com.example.service.UrlService;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UrlController.class)
@DisplayName("UrlController Tests")
class UrlControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private UrlService urlService;

  @Test
  @DisplayName("Should return health status successfully")
  void shouldReturnHealthStatusSuccessfully() throws Exception {
    mockMvc
        .perform(get("/health"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("OK"))
        .andExpect(jsonPath("$.message").value("Rest Service is running."))
        .andExpect(jsonPath("$.timestamp").exists());
  }

  @Test
  @DisplayName("Should create short URL successfully")
  void shouldCreateShortUrlSuccessfully() throws Exception {
    String longUrl = "https://www.example.com";
    String shortCode = "abc123";

    UrlDto urlDto = new UrlDto();
    urlDto.setId(1L);
    urlDto.setLongUrl(longUrl);
    urlDto.setShortCode(shortCode);
    urlDto.setShortUrl("http://short.url/" + shortCode);
    urlDto.setClicks(0);
    urlDto.setCreatedAt(LocalDateTime.now());
    urlDto.setUpdatedAt(LocalDateTime.now());

    when(urlService.addUrl(longUrl)).thenReturn(urlDto);

    String requestBody = "{\"url\":\"" + longUrl + "\"}";

    mockMvc
        .perform(post("/api/urls").contentType(MediaType.APPLICATION_JSON).content(requestBody))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.longUrl").value(longUrl))
        .andExpect(jsonPath("$.shortCode").value(shortCode))
        .andExpect(jsonPath("$.shortUrl").value("http://short.url/" + shortCode))
        .andExpect(jsonPath("$.clicks").value(0));

    verify(urlService).addUrl(longUrl);
  }

  @Test
  @DisplayName("Should get paginated URLs successfully")
  void shouldGetPaginatedUrlsSuccessfully() throws Exception {
    Url url1 = new Url();
    url1.setId(1L);
    url1.setShortCode("abc123");
    url1.setLongUrl("https://example1.com");
    url1.setClicks(5);

    Url url2 = new Url();
    url2.setId(2L);
    url2.setShortCode("xyz789");
    url2.setLongUrl("https://example2.com");
    url2.setClicks(10);

    Page<Url> urlPage = new PageImpl<>(Arrays.asList(url1, url2));

    when(urlService.getUrls(0, 10, null, null)).thenReturn(urlPage);

    mockMvc
        .perform(get("/api/urls").param("pageNo", "0").param("pageSize", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content[0].shortCode").value("abc123"))
        .andExpect(jsonPath("$.content[1].shortCode").value("xyz789"));

    verify(urlService).getUrls(0, 10, null, null);
  }

  @Test
  @DisplayName("Should get URLs with default pagination parameters")
  void shouldGetUrlsWithDefaultPaginationParameters() throws Exception {
    Page<Url> emptyPage = new PageImpl<>(Arrays.asList());

    when(urlService.getUrls(0, 10, null, null)).thenReturn(emptyPage);

    mockMvc
        .perform(get("/api/urls"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray());

    verify(urlService).getUrls(0, 10, null, null);
  }

  @Test
  @DisplayName("Should throw exception when page number is negative")
  void shouldThrowExceptionWhenPageNumberIsNegative() throws Exception {
    when(urlService.getUrls(eq(-1), eq(10), isNull(), isNull()))
        .thenThrow(new IllegalArgumentException("Page number cannot be negative"));

    mockMvc
        .perform(get("/api/urls").param("pageNo", "-1").param("pageSize", "10"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Page number cannot be negative"));

    verify(urlService).getUrls(eq(-1), eq(10), isNull(), isNull());
  }

  @Test
  @DisplayName("Should throw exception when page size is zero")
  void shouldThrowExceptionWhenPageSizeIsZero() throws Exception {
    when(urlService.getUrls(eq(0), eq(0), isNull(), isNull()))
        .thenThrow(new IllegalArgumentException("Page size must be greater than zero."));

    mockMvc
        .perform(get("/api/urls").param("pageNo", "0").param("pageSize", "0"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Page size must be greater than zero."));

    verify(urlService).getUrls(eq(0), eq(0), isNull(), isNull());
  }

  @Test
  @DisplayName("Should throw exception when page size is negative")
  void shouldThrowExceptionWhenPageSizeIsNegative() throws Exception {
    when(urlService.getUrls(eq(0), eq(-5), isNull(), isNull()))
        .thenThrow(new IllegalArgumentException("Page size must be greater than zero."));

    mockMvc
        .perform(get("/api/urls").param("pageNo", "0").param("pageSize", "-5"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Page size must be greater than zero."));

    verify(urlService).getUrls(eq(0), eq(-5), isNull(), isNull());
  }

  @Test
  @DisplayName("Should get URL by short code successfully")
  void shouldGetUrlByShortCodeSuccessfully() throws Exception {
    String shortCode = "abc123";

    UrlDto urlDto = new UrlDto();
    urlDto.setId(1L);
    urlDto.setLongUrl("https://www.example.com");
    urlDto.setShortCode(shortCode);
    urlDto.setShortUrl("http://short.url/" + shortCode);
    urlDto.setClicks(5);

    when(urlService.getUrlByShortCode(shortCode)).thenReturn(urlDto);

    mockMvc
        .perform(get("/api/urls/{shortCode}", shortCode))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.shortCode").value(shortCode))
        .andExpect(jsonPath("$.longUrl").value("https://www.example.com"))
        .andExpect(jsonPath("$.clicks").value(5));

    verify(urlService).getUrlByShortCode(shortCode);
  }

  @Test
  @DisplayName("Should return 404 when URL not found by short code")
  void shouldReturn404WhenUrlNotFoundByShortCode() throws Exception {
    String shortCode = "notexists";

    when(urlService.getUrlByShortCode(shortCode))
        .thenThrow(new NoSuchElementException("URL not found"));

    mockMvc.perform(get("/api/urls/{shortCode}", shortCode)).andExpect(status().isNotFound());

    verify(urlService).getUrlByShortCode(shortCode);
  }

  @Test
  @DisplayName("Should delete URL successfully")
  void shouldDeleteUrlSuccessfully() throws Exception {
    String shortCode = "abc123";

    doNothing().when(urlService).deleteUrl(shortCode);

    mockMvc
        .perform(delete("/api/urls/{shortCode}", shortCode))
        .andExpect(status().isNoContent())
        .andExpect(content().string(""));

    verify(urlService).deleteUrl(shortCode);
  }

  @Test
  @DisplayName("Should return 404 when deleting non-existent URL")
  void shouldReturn404WhenDeletingNonExistentUrl() throws Exception {
    String shortCode = "notexists";

    doThrow(new NoSuchElementException("URL not found")).when(urlService).deleteUrl(shortCode);

    mockMvc.perform(delete("/api/urls/{shortCode}", shortCode)).andExpect(status().isNotFound());

    verify(urlService).deleteUrl(shortCode);
  }

  @Test
  @DisplayName("Should redirect to long URL successfully")
  void shouldRedirectToLongUrlSuccessfully() throws Exception {
    String shortCode = "abc123";
    String longUrl = "https://www.example.com";

    Url url = new Url();
    url.setId(1L);
    url.setShortCode(shortCode);
    url.setLongUrl(longUrl);

    when(urlService.redirect(shortCode)).thenReturn(url);

    mockMvc
        .perform(get("/{shortCode}", shortCode))
        .andExpect(status().isTemporaryRedirect())
        .andExpect(header().string("Location", longUrl));

    verify(urlService).redirect(shortCode);
  }

  @Test
  @DisplayName("Should return 404 when redirecting to non-existent URL")
  void shouldReturn404WhenRedirectingToNonExistentUrl() throws Exception {
    String shortCode = "notexists";

    when(urlService.redirect(shortCode)).thenThrow(new NoSuchElementException("URL not found"));

    mockMvc.perform(get("/{shortCode}", shortCode)).andExpect(status().isNotFound());

    verify(urlService).redirect(shortCode);
  }

  @Test
  @DisplayName("Should handle large page size")
  void shouldHandleLargePageSize() throws Exception {
    Page<Url> emptyPage = new PageImpl<>(Arrays.asList());

    when(urlService.getUrls(0, 1000, null, null)).thenReturn(emptyPage);

    mockMvc
        .perform(get("/api/urls").param("pageNo", "0").param("pageSize", "1000"))
        .andExpect(status().isOk());

    verify(urlService).getUrls(0, 1000, null, null);
  }

  @Test
  @DisplayName("Should handle empty URL list")
  void shouldHandleEmptyUrlList() throws Exception {
    Page<Url> emptyPage = new PageImpl<>(Arrays.asList());

    when(urlService.getUrls(0, 10, null, null)).thenReturn(emptyPage);

    mockMvc
        .perform(get("/api/urls"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content").isEmpty());

    verify(urlService).getUrls(0, 10, null, null);
  }

  @Test
  @DisplayName("Should create URL with complex long URL")
  void shouldCreateUrlWithComplexLongUrl() throws Exception {
    String longUrl = "https://www.example.com/path/to/resource?param1=value1&param2=value2";
    String shortCode = "xyz789";

    UrlDto urlDto = new UrlDto();
    urlDto.setId(1L);
    urlDto.setLongUrl(longUrl);
    urlDto.setShortCode(shortCode);
    urlDto.setShortUrl("http://short.url/" + shortCode);
    urlDto.setClicks(0);

    when(urlService.addUrl(longUrl)).thenReturn(urlDto);

    String requestBody = "{\"url\":\"" + longUrl + "\"}";

    mockMvc
        .perform(post("/api/urls").contentType(MediaType.APPLICATION_JSON).content(requestBody))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.longUrl").value(longUrl));

    verify(urlService).addUrl(longUrl);
  }

  @Test
  @DisplayName("Should redirect with different short codes")
  void shouldRedirectWithDifferentShortCodes() throws Exception {
    String shortCode1 = "abc123";
    String longUrl1 = "https://example1.com";

    Url url1 = new Url();
    url1.setShortCode(shortCode1);
    url1.setLongUrl(longUrl1);

    when(urlService.redirect(shortCode1)).thenReturn(url1);

    mockMvc
        .perform(get("/{shortCode}", shortCode1))
        .andExpect(status().isTemporaryRedirect())
        .andExpect(header().string("Location", longUrl1));

    verify(urlService).redirect(shortCode1);
  }

  @Test
  @DisplayName("Should handle pagination with specific page number")
  void shouldHandlePaginationWithSpecificPageNumber() throws Exception {
    Page<Url> urlPage = new PageImpl<>(Arrays.asList());

    when(urlService.getUrls(5, 20, null, null)).thenReturn(urlPage);

    mockMvc
        .perform(get("/api/urls").param("pageNo", "5").param("pageSize", "20"))
        .andExpect(status().isOk());

    verify(urlService).getUrls(5, 20, null, null);
  }
}
