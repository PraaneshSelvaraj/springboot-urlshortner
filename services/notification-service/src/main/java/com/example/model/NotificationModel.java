package com.example.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
public class NotificationModel {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String message;

  @Column(name = "short_code")
  private String shortCode;

  @ManyToOne
  @JoinColumn(name = "notification_type_id", nullable = false)
  private NotificationTypeModel type;

  @ManyToOne
  @JoinColumn(name = "notification_status_id", nullable = false)
  private NotificationStatusModel status;

  @Column(name = "created_at")
  private LocalDateTime createdAt = LocalDateTime.now();
}
