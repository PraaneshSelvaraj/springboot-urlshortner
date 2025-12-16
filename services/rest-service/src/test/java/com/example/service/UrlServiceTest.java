package com.example.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.example.dto.UrlDto;
import com.example.exception.InvalidUrlException;
import com.example.exception.ThresholdReachedException;
import com.example.exception.UrlExpiredException;
import com.example.model.Url;
import com.example.repository.UrlRepository;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("UrlService Tests")
class UrlServiceTest {

  @Mock private UrlRepository urlRepository;

  @Mock private NotificationService notificationService;

  @InjectMocks private UrlService urlService;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(urlService, "bannedHosts", new HashSet<>(Arrays.asList()));
    ReflectionTestUtils.setField(urlService, "urlExpirationHours", 24);
    ReflectionTestUtils.setField(urlService, "notificationThreshold", 100);
    ReflectionTestUtils.setField(urlService, "baseUrl", "http://short.url");
  }

  @Test
  @DisplayName("Should add URL successfully with valid URL")
  void shouldAddUrlSuccessfully() {
    String longUrl = "https://www.example.com";
    String shortCode = "abc123";

    Url savedUrl = new Url();
    savedUrl.setId(1L);
    savedUrl.setShortCode(shortCode);
    savedUrl.setLongUrl(longUrl);
    savedUrl.setClicks(0);
    savedUrl.setDeleted(false);
    savedUrl.setCreatedAt(LocalDateTime.now());
    savedUrl.setUpdatedAt(LocalDateTime.now());

    when(urlRepository.findByShortCode(anyString())).thenReturn(Optional.empty());
    when(urlRepository.save(any(Url.class))).thenReturn(savedUrl);

    UrlDto result = urlService.addUrl(longUrl);

    assertThat(result).isNotNull();
    assertThat(result.getLongUrl()).isEqualTo(longUrl);
    assertThat(result.getShortCode()).isNotNull();
    assertThat(result.getShortUrl()).contains("http://short.url/");
    assertThat(result.getClicks()).isEqualTo(0);

    verify(urlRepository).save(any(Url.class));
    verify(notificationService).sendUrlCreatedNotification(anyString(), eq(longUrl));
  }

  @Test
  @DisplayName("Should throw InvalidUrlException for invalid URL")
  void shouldThrowInvalidUrlExceptionForInvalidUrl() {
    String invalidUrl = "not-a-valid-url";

    assertThatThrownBy(() -> urlService.addUrl(invalidUrl)).isInstanceOf(InvalidUrlException.class);

    verify(urlRepository, never()).save(any(Url.class));
    verify(notificationService, never()).sendUrlCreatedNotification(anyString(), anyString());
  }

  @Test
  @DisplayName("Should throw InvalidUrlException for URL without scheme")
  void shouldThrowInvalidUrlExceptionForUrlWithoutScheme() {
    String invalidUrl = "www.example.com";

    assertThatThrownBy(() -> urlService.addUrl(invalidUrl)).isInstanceOf(InvalidUrlException.class);

    verify(urlRepository, never()).save(any(Url.class));
  }

  @Test
  @DisplayName("Should throw InvalidUrlException for FTP scheme")
  void shouldThrowInvalidUrlExceptionForFtpScheme() {
    String ftpUrl = "ftp://example.com/file.txt";

    assertThatThrownBy(() -> urlService.addUrl(ftpUrl)).isInstanceOf(InvalidUrlException.class);

    verify(urlRepository, never()).save(any(Url.class));
  }

  @Test
  @DisplayName("Should get paginated URLs")
  void shouldGetPaginatedUrls() {
    Url url1 = new Url();
    url1.setId(1L);
    url1.setShortCode("abc123");
    url1.setLongUrl("https://example1.com");

    Url url2 = new Url();
    url2.setId(2L);
    url2.setShortCode("xyz789");
    url2.setLongUrl("https://example2.com");

    Page<Url> urlPage = new PageImpl<>(Arrays.asList(url1, url2));

    when(urlRepository.findAll(any(Pageable.class))).thenReturn(urlPage);

    Page<Url> result = urlService.getUrls(0, 10);

    assertThat(result).isNotNull();
    assertThat(result.getContent()).hasSize(2);
    assertThat(result.getContent()).containsExactly(url1, url2);

    verify(urlRepository).findAll(any(Pageable.class));
  }

  @Test
  @DisplayName("Should get URL by short code successfully")
  void shouldGetUrlByShortCodeSuccessfully() {
    String shortCode = "abc123";
    Url url = new Url();
    url.setId(1L);
    url.setShortCode(shortCode);
    url.setLongUrl("https://example.com");
    url.setClicks(5);
    url.setCreatedAt(LocalDateTime.now());
    url.setUpdatedAt(LocalDateTime.now());

    when(urlRepository.findByShortCode(shortCode)).thenReturn(Optional.of(url));

    UrlDto result = urlService.getUrlByShortCode(shortCode);

    assertThat(result).isNotNull();
    assertThat(result.getShortCode()).isEqualTo(shortCode);
    assertThat(result.getLongUrl()).isEqualTo("https://example.com");
    assertThat(result.getClicks()).isEqualTo(5);
    assertThat(result.getShortUrl()).isEqualTo("http://short.url/" + shortCode);

    verify(urlRepository).findByShortCode(shortCode);
  }

  @Test
  @DisplayName("Should throw NoSuchElementException when short code does not exist")
  void shouldThrowNoSuchElementExceptionWhenShortCodeNotFound() {
    String shortCode = "notexists";

    when(urlRepository.findByShortCode(shortCode)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> urlService.getUrlByShortCode(shortCode))
        .isInstanceOf(NoSuchElementException.class);

    verify(urlRepository).findByShortCode(shortCode);
  }

  @Test
  @DisplayName("Should redirect successfully and increment click count")
  void shouldRedirectSuccessfully() {
    String shortCode = "abc123";
    Url url = new Url();
    url.setId(1L);
    url.setShortCode(shortCode);
    url.setLongUrl("https://example.com");
    url.setClicks(5);
    url.setExpiresAt(LocalDateTime.now().plusHours(1));

    when(urlRepository.findByShortCode(shortCode)).thenReturn(Optional.of(url));
    when(urlRepository.getClickCount(shortCode)).thenReturn(6);

    Url result = urlService.redirect(shortCode);

    assertThat(result).isNotNull();
    assertThat(result.getShortCode()).isEqualTo(shortCode);

    verify(urlRepository).findByShortCode(shortCode);
    verify(urlRepository).incrementClickCount(shortCode);
    verify(urlRepository).getClickCount(shortCode);
    verify(notificationService, never()).sendThresholdNotification(anyString());
  }

  @Test
  @DisplayName("Should throw UrlExpiredException when URL is expired")
  void shouldThrowUrlExpiredExceptionWhenExpired() {
    String shortCode = "abc123";
    Url url = new Url();
    url.setId(1L);
    url.setShortCode(shortCode);
    url.setLongUrl("https://example.com");
    url.setExpiresAt(LocalDateTime.now().minusHours(1));

    when(urlRepository.findByShortCode(shortCode)).thenReturn(Optional.of(url));

    assertThatThrownBy(() -> urlService.redirect(shortCode))
        .isInstanceOf(UrlExpiredException.class);

    verify(urlRepository).findByShortCode(shortCode);
    verify(urlRepository, never()).incrementClickCount(anyString());
  }

  @Test
  @DisplayName("Should throw ThresholdReachedException when click count exceeds threshold")
  void shouldThrowThresholdReachedExceptionWhenThresholdExceeded() {
    String shortCode = "abc123";
    Url url = new Url();
    url.setId(1L);
    url.setShortCode(shortCode);
    url.setLongUrl("https://example.com");
    url.setClicks(100);
    url.setExpiresAt(LocalDateTime.now().plusHours(1));

    when(urlRepository.findByShortCode(shortCode)).thenReturn(Optional.of(url));
    when(urlRepository.getClickCount(shortCode)).thenReturn(101);

    assertThatThrownBy(() -> urlService.redirect(shortCode))
        .isInstanceOf(ThresholdReachedException.class);

    verify(urlRepository).incrementClickCount(shortCode);
    verify(urlRepository).getClickCount(shortCode);
    verify(notificationService).sendThresholdNotification(shortCode);
  }

  @Test
  @DisplayName("Should redirect successfully when expires_at is null")
  void shouldRedirectSuccessfullyWhenExpiresAtIsNull() {
    String shortCode = "abc123";
    Url url = new Url();
    url.setId(1L);
    url.setShortCode(shortCode);
    url.setLongUrl("https://example.com");
    url.setClicks(5);
    url.setExpiresAt(null);

    when(urlRepository.findByShortCode(shortCode)).thenReturn(Optional.of(url));
    when(urlRepository.getClickCount(shortCode)).thenReturn(6);

    Url result = urlService.redirect(shortCode);

    assertThat(result).isNotNull();
    verify(urlRepository).incrementClickCount(shortCode);
  }

  @Test
  @DisplayName("Should delete URL successfully")
  void shouldDeleteUrlSuccessfully() {
    String shortCode = "abc123";
    Url url = new Url();
    url.setId(1L);
    url.setShortCode(shortCode);
    url.setLongUrl("https://example.com");

    when(urlRepository.findByShortCode(shortCode)).thenReturn(Optional.of(url));

    urlService.deleteUrl(shortCode);

    verify(urlRepository).findByShortCode(shortCode);
    verify(urlRepository).delete(url);
  }

  @Test
  @DisplayName("Should throw NoSuchElementException when deleting non-existent URL")
  void shouldThrowNoSuchElementExceptionWhenDeletingNonExistentUrl() {
    String shortCode = "notexists";

    when(urlRepository.findByShortCode(shortCode)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> urlService.deleteUrl(shortCode))
        .isInstanceOf(NoSuchElementException.class);

    verify(urlRepository).findByShortCode(shortCode);
    verify(urlRepository, never()).delete(any(Url.class));
  }

  @Test
  @DisplayName("Should generate unique short code when collision occurs")
  void shouldGenerateUniqueShortCodeWhenCollisionOccurs() {
    String longUrl = "https://www.example.com";

    Url existingUrl = new Url();
    existingUrl.setShortCode("abc123");

    Url savedUrl = new Url();
    savedUrl.setId(1L);
    savedUrl.setShortCode("xyz789");
    savedUrl.setLongUrl(longUrl);
    savedUrl.setClicks(0);

    when(urlRepository.findByShortCode(anyString()))
        .thenReturn(Optional.of(existingUrl))
        .thenReturn(Optional.empty());
    when(urlRepository.save(any(Url.class))).thenReturn(savedUrl);

    UrlDto result = urlService.addUrl(longUrl);

    assertThat(result).isNotNull();
    verify(urlRepository, atLeast(2)).findByShortCode(anyString());
  }

  @Test
  @DisplayName("Should handle URLs with HTTP scheme")
  void shouldHandleUrlsWithHttpScheme() {
    String httpUrl = "http://www.example.com";

    Url savedUrl = new Url();
    savedUrl.setId(1L);
    savedUrl.setShortCode("abc123");
    savedUrl.setLongUrl(httpUrl);
    savedUrl.setClicks(0);

    when(urlRepository.findByShortCode(anyString())).thenReturn(Optional.empty());
    when(urlRepository.save(any(Url.class))).thenReturn(savedUrl);

    UrlDto result = urlService.addUrl(httpUrl);

    assertThat(result).isNotNull();
    assertThat(result.getLongUrl()).isEqualTo(httpUrl);
    verify(urlRepository).save(any(Url.class));
  }

  @Test
  @DisplayName("Should set correct expiration time based on configured hours")
  void shouldSetCorrectExpirationTime() {
    String longUrl = "https://www.example.com";
    ReflectionTestUtils.setField(urlService, "urlExpirationHours", 48);

    Url savedUrl = new Url();
    savedUrl.setId(1L);
    savedUrl.setShortCode("abc123");
    savedUrl.setLongUrl(longUrl);
    savedUrl.setClicks(0);
    savedUrl.setExpiresAt(LocalDateTime.now().plusHours(48));

    when(urlRepository.findByShortCode(anyString())).thenReturn(Optional.empty());
    when(urlRepository.save(any(Url.class))).thenReturn(savedUrl);

    UrlDto result = urlService.addUrl(longUrl);

    assertThat(result).isNotNull();
    assertThat(result.getExpiresAt()).isAfter(LocalDateTime.now().plusHours(47));
  }

  @Test
  @DisplayName("Should throw InvalidUrlException for invalid DNS domain")
  void shouldThrowInvalidUrlExceptionForInvalidDnsDomain() {
    String invalidDnsUrl = "https://this-domain-definitely-does-not-exist-xyz12345.invalid";

    assertThatThrownBy(() -> urlService.addUrl(invalidDnsUrl))
        .isInstanceOf(InvalidUrlException.class);

    verify(urlRepository, never()).save(any(Url.class));
    verify(notificationService, never()).sendUrlCreatedNotification(anyString(), anyString());
  }

  @Test
  @DisplayName("Should throw InvalidUrlException for non-resolvable domain")
  void shouldThrowInvalidUrlExceptionForNonResolvableDomain() {
    String nonResolvableUrl = "https://invalid-domain-that-does-not-resolve-99999.test";

    assertThatThrownBy(() -> urlService.addUrl(nonResolvableUrl))
        .isInstanceOf(InvalidUrlException.class);

    verify(urlRepository, never()).save(any(Url.class));
    verify(notificationService, never()).sendUrlCreatedNotification(anyString(), anyString());
  }
}
