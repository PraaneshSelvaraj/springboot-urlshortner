package com.example.exception;

public class TokenBlacklistException extends RuntimeException {

  public TokenBlacklistException(String message) {
    super(message);
  }

  public TokenBlacklistException(String message, Throwable cause) {
    super(message, cause);
  }
}
