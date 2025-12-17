package com.example.service;

import com.example.dto.UrlDto;
import com.example.exception.*;
import com.example.model.Url;
import com.example.repository.UrlRepository;
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
import org.springframework.stereotype.Service;

@Service
public class UrlService {
  private final UrlRepository urlRepo;
  private final NotificationService notificationService;

  private static final Random random = new Random();

  @Value("${bannedHosts}")
  private Set<String> bannedHosts;

  @Value("${urlExpirationHours}")
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
    urlDto.setCreatedAt(currentTime);
    urlDto.setUpdatedAt(currentTime);
    urlDto.setExpiresAt(expiresAt);

    return urlDto;
  }

  public Page<Url> getUrls(int pageNo, int pageSize) {
    Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by("id").descending());
    return urlRepo.findAll(pageable);
  }

  public UrlDto getUrlByShortCode(String shortCode) {
    Optional<Url> urlOpt = urlRepo.findByShortCode(shortCode);
    Url url =
        urlOpt.orElseThrow(
            () ->
                new NoSuchElementException("Url with shortcocde " + shortCode + " does not exist"));

    UrlDto urlDto = new UrlDto();
    urlDto.setId(url.getId());
    urlDto.setLongUrl(url.getLongUrl());
    urlDto.setShortCode(url.getShortCode());
    urlDto.setShortUrl(baseUrl + "/" + url.getShortCode());
    urlDto.setClicks(url.getClicks());
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
    Optional<Url> urlOpt = urlRepo.findByShortCode(shortCode);

    Url url =
        urlOpt.orElseThrow(
            () -> new NoSuchElementException("URL with short code '" + shortCode + "' not found"));

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
