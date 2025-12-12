package com.example.exception;

public class InvalidUrlException extends RuntimeException {

  public InvalidUrlException() {
    super("Invalid URL provided");
  }
}
