package com.example.exception;

public class ThresholdReachedException extends RuntimeException {
  public ThresholdReachedException() {
    super("URL has reached the click threshold");
  }
}
