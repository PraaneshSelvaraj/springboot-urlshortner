package com.example.urlshortner.controller;

import com.example.urlshortner.dto.ShortenRequest;
import com.example.urlshortner.model.Url;
import com.example.urlshortner.service.UrlService;
import java.net.URI;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class UrlController {

  private UrlService urlService;

  public UrlController(UrlService urlService) {
    this.urlService = urlService;
  }

  @PostMapping("/urls")
  public ResponseEntity<Url> addUrl(@RequestBody ShortenRequest request) {
    Url newUrl = urlService.addUrl(request.getUrl());

    return new ResponseEntity<>(newUrl, HttpStatus.CREATED);
  }

  @GetMapping("/urls")
  public ResponseEntity<List<Url>> getUrls() {
    List<Url> urls = urlService.getUrls();

    return new ResponseEntity<>(urls, HttpStatus.OK);
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
