package com.townpet.engagement;

import com.townpet.common.UuidV7;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "engagement_reaction",
    uniqueConstraints =
        @UniqueConstraint(columnNames = {"publication_id", "author_member_id", "type"}))
class ReactionEntity {
  @Id private UUID id;

  @Column(nullable = false)
  private UUID publicationId;

  @Column(nullable = false)
  private UUID authorMemberId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private ReactionType type;

  @Column(nullable = false)
  private Instant createdAt;

  protected ReactionEntity() {}

  ReactionEntity(UUID publicationId, UUID authorMemberId, ReactionType type) {
    this.id = UuidV7.randomUuid();
    this.publicationId = publicationId;
    this.authorMemberId = authorMemberId;
    this.type = type;
    this.createdAt = Instant.now();
  }

  UUID getPublicationId() {
    return publicationId;
  }

  UUID getAuthorMemberId() {
    return authorMemberId;
  }

  Instant getCreatedAt() {
    return createdAt;
  }

  ReactionType getType() {
    return type;
  }
}
