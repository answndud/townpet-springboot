package com.townpet.engagement;

import com.townpet.common.UuidV7;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "engagement_comment")
class CommentEntity {
  @Id private UUID id;

  @Column(nullable = false)
  private UUID publicationId;

  @Column(nullable = false)
  private UUID authorMemberId;

  @Column(nullable = false, length = 5000)
  private String body;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private CommentLifecycle lifecycle;

  @Column(nullable = false)
  private Instant createdAt;

  @Column(nullable = false)
  private Instant updatedAt;

  @Version private long version;

  protected CommentEntity() {}

  CommentEntity(UUID publicationId, UUID authorMemberId, String body) {
    this.id = UuidV7.randomUuid();
    this.publicationId = publicationId;
    this.authorMemberId = authorMemberId;
    this.body = body;
    this.lifecycle = CommentLifecycle.ACTIVE;
    this.createdAt = Instant.now();
    this.updatedAt = createdAt;
  }

  void delete(Instant changedAt) {
    this.lifecycle = CommentLifecycle.DELETED;
    this.updatedAt = changedAt;
  }

  void edit(String nextBody, Instant changedAt) {
    this.body = nextBody.trim();
    this.updatedAt = changedAt;
  }

  UUID getId() {
    return id;
  }

  UUID getPublicationId() {
    return publicationId;
  }

  UUID getAuthorMemberId() {
    return authorMemberId;
  }

  String getBody() {
    return body;
  }

  CommentLifecycle getLifecycle() {
    return lifecycle;
  }

  Instant getCreatedAt() {
    return createdAt;
  }

  Instant getUpdatedAt() {
    return updatedAt;
  }

  long getVersion() {
    return version;
  }
}
