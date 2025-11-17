package com.example.urlshortner.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Url {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String shortCode;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String longUrl;

  @Column(nullable = false)
  private int clicks = 0;

  @Column(nullable = false)
  private boolean isDeleted = false;

  @Column(nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(nullable = false)
  private LocalDateTime updatedAt;

  @Column(nullable = true)
  private LocalDateTime expiresAt;
}
