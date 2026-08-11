package com.townpet.notification;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notification")
class NotificationEntity {
  @Id private UUID id;

  @Column(nullable = false)
  private UUID recipientMemberId;

  @Column(nullable = false, length = 40)
  private String type;

  @Column(nullable = false, length = 160)
  private String title;

  @Column(nullable = false, length = 1000)
  private String body;

  private Instant readAt;

  @Column(nullable = false)
  private Instant createdAt;

  protected NotificationEntity() {}

  UUID getId() {
    return id;
  }

  UUID getRecipientMemberId() {
    return recipientMemberId;
  }

  String getType() {
    return type;
  }

  String getTitle() {
    return title;
  }

  String getBody() {
    return body;
  }

  Instant getReadAt() {
    return readAt;
  }

  Instant getCreatedAt() {
    return createdAt;
  }

  void markRead() {
    readAt = Instant.now();
  }
}
