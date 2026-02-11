package com.example.service;

import com.example.dto.UrlDto;
import com.example.exception.*;
import com.example.model.Url;
import com.example.repository.UrlRepository;
import com.example.util.UserContext;
import io.grpc.Status;
import java.net.InetAddress;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class UrlService {
  private final UrlRepository urlRepo;
  private final NotificationService notificationService;

  private static final Random random = new Random();

  @Value("${banned-hosts}")
  private Set<String> bannedHosts;

  @Value("${url-expiration-hours}")
  private int urlExpirationHours;

  @Value("${notification.threshold}")
  private int notificationThreshold;

  @Value("${app.base-url}")
  private String baseUrl;

  public UrlService(UrlRepository urlRepo, NotificationService notificationService) {
    this.urlRepo = urlRepo;
    this.notificationService = notificationService;
  }

  public UrlDto addUrl(String url) {
    long userId = UserContext.getCurrentUserId();

    if (!isValidUrl(url)) {
      throw new InvalidUrlException();
    }

    String code = generateShortCode(7);
    LocalDateTime currentTime = LocalDateTime.now();
    LocalDateTime expiresAt = currentTime.plusHours(urlExpirationHours);

    Url newUrl = new Url();
    newUrl.setShortCode(code);
    newUrl.setLongUrl(url);
    newUrl.setClicks(0);
    newUrl.setDeleted(false);
    newUrl.setCreatedBy(userId);
    newUrl.setCreatedAt(currentTime);
    newUrl.setUpdatedAt(currentTime);
    newUrl.setExpiresAt(expiresAt);
    Url urlAdded = urlRepo.save(newUrl);
    notificationService.sendUrlCreatedNotification(code, url);

    UrlDto urlDto = new UrlDto();
    urlDto.setId(urlAdded.getId());
    urlDto.setLongUrl(url);
    urlDto.setShortCode(code);
    urlDto.setShortUrl(baseUrl + "/" + code);
    urlDto.setClicks(0);
    urlDto.setExpired(false);
    urlDto.setCreatedAt(currentTime);
    urlDto.setUpdatedAt(currentTime);
    urlDto.setExpiresAt(expiresAt);

    return urlDto;
  }

  public Page<Url> getUrls(int pageNo, int pageSize, String sortBy, String sortDirection) {

    if (pageNo < 0) {
      throw new IllegalArgumentException("Page number cannot be negative");
    }

    if (pageSize <= 0) {
      throw new IllegalArgumentException("Page size must be greater than zero.");
    }

    Set<String> allowedSortField = Set.of("id", "shortCode", "clicks", "createdAt", "expiresAt");

    if (sortBy != null && !allowedSortField.contains(sortBy)) {
      throw new IllegalArgumentException(
          "Invalid sortBy: '" + sortBy + "'. Allowed values: " + allowedSortField);
    }
    String validSortBy = sortBy != null ? sortBy : "id";

    if (sortDirection != null
        && !sortDirection.equalsIgnoreCase("asc")
        && !sortDirection.equalsIgnoreCase("desc")) {
      throw new IllegalArgumentException(
          "Invalid sortDirection: '" + sortDirection + "'. Allowed values: asc, desc");
    }
    String validDirection = sortDirection != null ? sortDirection : "desc";

    Sort.Direction direction =
        switch (validDirection.toUpperCase()) {
          case "ASC" -> Sort.Direction.ASC;
          case "DESC" -> Sort.Direction.DESC;
          default ->
              throw Status.INVALID_ARGUMENT
                  .withDescription(
                      "Invalid sortDirection field: '"
                          + sortDirection
                          + "'. Allowed fields: ASC, DESC")
                  .asRuntimeException();
        };

    Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by(direction, validSortBy));

    String userRole = UserContext.getCurrentUserRole();

    Page<Url> urlPage;
    if (userRole.equalsIgnoreCase("admin")) {
      urlPage = urlRepo.findAll(pageable);
    } else {
      long userId = UserContext.getCurrentUserId();
      urlPage = urlRepo.findByCreatedBy(userId, pageable);
    }

    if (pageNo > 0 && urlPage.getTotalPages() > 0 && pageNo >= urlPage.getTotalPages()) {
      throw Status.INVALID_ARGUMENT
          .withDescription(
              "Page number " + pageNo + " exceeds total pages (" + urlPage.getTotalPages() + ")")
          .asRuntimeException();
    }

    return urlPage;
  }

  public UrlDto getUrlByShortCode(String shortCode) {
    Optional<Url> urlOpt = urlRepo.findByShortCode(shortCode);
    Url url =
        urlOpt.orElseThrow(
            () ->
                new NoSuchElementException("Url with shortcocde " + shortCode + " does not exist"));

    long userId = UserContext.getCurrentUserId();
    String userRole = UserContext.getCurrentUserRole();
    if (userRole.equalsIgnoreCase("user") && userId != url.getCreatedBy()) {
      throw new AccessDeniedException("You do not have permission to view this URL");
    }

    UrlDto urlDto = new UrlDto();
    urlDto.setId(url.getId());
    urlDto.setLongUrl(url.getLongUrl());
    urlDto.setShortCode(url.getShortCode());
    urlDto.setShortUrl(baseUrl + "/" + url.getShortCode());
    urlDto.setClicks(url.getClicks());
    urlDto.setExpired(
        url.getExpiresAt() != null && LocalDateTime.now().isAfter(url.getExpiresAt()));
    urlDto.setCreatedAt(url.getCreatedAt());
    urlDto.setUpdatedAt(url.getUpdatedAt());
    urlDto.setExpiresAt(url.getExpiresAt());

    return urlDto;
  }

  public Url redirect(String shortCode) {
    Optional<Url> urlOpt = urlRepo.findByShortCode(shortCode);

    Url url =
        urlOpt.orElseThrow(
            () ->
                new NoSuchElementException("Url with shortcode " + shortCode + " does not exist"));

    LocalDateTime currentTime = LocalDateTime.now();
    if (url.getExpiresAt() != null && currentTime.isAfter(url.getExpiresAt())) {
      throw new UrlExpiredException();
    }

    urlRepo.incrementClickCount(shortCode);
    Integer count = urlRepo.getClickCount(shortCode);

    if (count != null && count > notificationThreshold) {
      notificationService.sendThresholdNotification(shortCode);
      throw new ThresholdReachedException();
    }

    return url;
  }

  public void deleteUrl(String shortCode) {
    long userId = UserContext.getCurrentUserId();

    Optional<Url> urlOpt = urlRepo.findByShortCode(shortCode);

    Url url =
        urlOpt.orElseThrow(
            () -> new NoSuchElementException("URL with short code '" + shortCode + "' not found"));

    String userRole = UserContext.getCurrentUserRole();
    if (userRole.equalsIgnoreCase("user") && userId != url.getCreatedBy()) {
      throw new AccessDeniedException("You do not have permission to view this URL");
    }

    urlRepo.delete(url);
  }

  private String getRandomString(int length) {
    StringBuilder sb = new StringBuilder();
    while (sb.length() < length) {
      char c = (char) (random.nextInt(95) + 32);
      if (Character.isLetterOrDigit(c)) {
        sb.append(c);
      }
    }
    return sb.toString();
  }

  private String generateShortCode(int length) {
    String code = getRandomString(length);
    Optional<Url> existing = urlRepo.findByShortCode(code);

    if (existing.isEmpty()) {
      return code;
    } else {
      return generateShortCode(length);
    }
  }

  private boolean isValidUrl(String url) {
    try {
      URI uri = new URI(url);

      String scheme = uri.getScheme();
      if (!(scheme.equals("http") || scheme.equals("https"))) {
        return false;
      }

      URI baseUri = new URI(baseUrl);
      if (uri.getHost().equalsIgnoreCase(baseUri.getHost())) {
        return false;
      }

      String host = uri.getHost();
      if (host == null || bannedHosts.contains(host)) {
        return false;
      }

      try {
        InetAddress.getByName(uri.getHost());
      } catch (Exception e) {
        return false;
      }

      return true;
    } catch (Exception e) {
      return false;
    }
  }
}
