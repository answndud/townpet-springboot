package com.townpet.notification;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import org.springframework.lang.Nullable;

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

  @Nullable private Instant readAt;

  @Column(nullable = false)
  private Instant createdAt;

  protected NotificationEntity() {}

  NotificationEntity(UUID id, UUID recipientMemberId, String type, String title, String body) {
    this.id = id;
    this.recipientMemberId = recipientMemberId;
    this.type = type;
    this.title = title;
    this.body = body;
    this.createdAt = Instant.now();
  }

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

  @Nullable
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
