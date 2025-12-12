package com.example.controller;

import com.example.dto.ShortenRequest;
import com.example.dto.UrlDto;
import com.example.model.Url;
import com.example.service.UrlService;
import java.net.URI;
import java.time.Instant;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class UrlController {

  private final UrlService urlService;

  public UrlController(UrlService urlService) {
    this.urlService = urlService;
  }

  @GetMapping("/health")
  public ResponseEntity<Map<String, String>> health() {
    return new ResponseEntity<>(
        Map.of(
            "status",
            "OK",
            "message",
            "Rest Service is running.",
            "timestamp",
            Instant.now().toString()),
        HttpStatus.OK);
  }

  @PostMapping("/urls")
  public ResponseEntity<UrlDto> addUrl(@RequestBody ShortenRequest request) {
    UrlDto newUrl = urlService.addUrl(request.getUrl());
    return new ResponseEntity<>(newUrl, HttpStatus.CREATED);
  }

  @GetMapping("/urls")
  public ResponseEntity<Page<Url>> getUrls(
      @RequestParam(defaultValue = "0") int pageNo,
      @RequestParam(defaultValue = "10") int pageSize) {

    if (pageNo < 0) {
      throw new IllegalArgumentException("Page number cannot be negative");
    }
    if (pageSize <= 0) {
      throw new IllegalArgumentException("Page size must be greater than zero.");
    }

    Page<Url> urls = urlService.getUrls(pageNo, pageSize);
    return new ResponseEntity<>(urls, HttpStatus.OK);
  }

  @GetMapping("/urls/{shortCode}")
  public ResponseEntity<UrlDto> getUrlByShortCode(@PathVariable String shortCode) {
    UrlDto urlDto = urlService.getUrlByShortCode(shortCode);
    return new ResponseEntity<>(urlDto, HttpStatus.OK);
  }

  @DeleteMapping("/urls/{shortCode}")
  public ResponseEntity<Void> deleteUrl(@PathVariable String shortCode) {
    urlService.deleteUrl(shortCode);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/{shortCode}")
  public ResponseEntity<Void> redirectUrl(@PathVariable String shortCode) {
    Url url = urlService.redirect(shortCode);
    return ResponseEntity.status(HttpStatus.TEMPORARY_REDIRECT)
        .location(URI.create(url.getLongUrl()))
        .build();
  }
}
