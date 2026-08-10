package com.townpet.relationship;

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
    name = "relationship_block",
    uniqueConstraints = @UniqueConstraint(columnNames = {"blocker_member_id", "blocked_member_id"}))
class BlockEntity {
  @Id private UUID id;

  @Column(nullable = false)
  private UUID blockerMemberId;

  @Column(nullable = false)
  private UUID blockedMemberId;

  @Column(nullable = false)
  private Instant createdAt;

  protected BlockEntity() {}

  BlockEntity(UUID blockerMemberId, UUID blockedMemberId) {
    this.id = UuidV7.randomUuid();
    this.blockerMemberId = blockerMemberId;
    this.blockedMemberId = blockedMemberId;
    this.createdAt = Instant.now();
  }
}
