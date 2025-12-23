package com.example.controller;

import com.example.dto.UrlDto;
import com.example.model.Url;
import com.example.service.UrlService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class WebController {

  private final UrlService urlService;

  @Value("${app.base-url}")
  private String baseUrl;

  public WebController(UrlService urlService) {
    this.urlService = urlService;
  }

  @GetMapping("/")
  public String home(Model model) {
    model.addAttribute("message", "Welcome to URL Shortener!");
    return "index";
  }

  @GetMapping("/urls")
  public String urls(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      Model model) {
    Page<Url> urls = urlService.getUrls(page, size);
    List<UrlDto> urlDtos = urls.stream().map(this::mapToUrlDto).collect(Collectors.toList());
    model.addAttribute("urls", urlDtos);
    model.addAttribute("currentPage", urls.getNumber());
    model.addAttribute("pageSize", urls.getSize());
    model.addAttribute("totalPages", urls.getTotalPages());
    model.addAttribute("totalElements", urls.getTotalElements());
    return "urls";
  }

  private UrlDto mapToUrlDto(Url url) {
    UrlDto urlDto = new UrlDto();
    urlDto.setId(url.getId());
    urlDto.setLongUrl(url.getLongUrl());
    urlDto.setShortCode(url.getShortCode());
    urlDto.setShortUrl(baseUrl + "/" + url.getShortCode());
    urlDto.setClicks(url.getClicks());
    urlDto.setCreatedAt(url.getCreatedAt());
    urlDto.setUpdatedAt(url.getUpdatedAt());
    urlDto.setExpiresAt(url.getExpiresAt());
    urlDto.setExpired(
        url.getExpiresAt() != null && LocalDateTime.now().isAfter(url.getExpiresAt()));
    return urlDto;
  }
}
