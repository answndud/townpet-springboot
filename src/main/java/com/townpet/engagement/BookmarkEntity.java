package com.townpet.engagement;

import com.townpet.common.UuidV7;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "engagement_bookmark",
    uniqueConstraints = @UniqueConstraint(columnNames = {"publication_id", "member_id"}))
class BookmarkEntity {
  @Id private UUID id;

  @Column(nullable = false)
  private UUID publicationId;

  @Column(nullable = false)
  private UUID memberId;

  @Column(nullable = false)
  private Instant createdAt;

  protected BookmarkEntity() {}

  UUID getPublicationId() {
    return publicationId;
  }

  UUID getMemberId() {
    return memberId;
  }

  Instant getCreatedAt() {
    return createdAt;
  }

  BookmarkEntity(UUID publicationId, UUID memberId) {
    this.id = UuidV7.randomUuid();
    this.publicationId = publicationId;
    this.memberId = memberId;
    this.createdAt = Instant.now();
  }
}
