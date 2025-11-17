package com.example.urlshortner.exception;

public class UrlExpiredException extends RuntimeException {
  public UrlExpiredException() {
    super("URL has expired");
  }
}
