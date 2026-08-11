package com.townpet.gathering;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "gathering_participant",
    uniqueConstraints = @UniqueConstraint(columnNames = {"gatheringId", "memberId"}))
class GatheringParticipantEntity {
  @Id private UUID id;

  @Column(nullable = false)
  private UUID gatheringId;

  @Column(nullable = false)
  private UUID memberId;

  @Column(nullable = false)
  private Instant joinedAt;

  protected GatheringParticipantEntity() {}

  GatheringParticipantEntity(UUID id, UUID gatheringId, UUID memberId) {
    this.id = id;
    this.gatheringId = gatheringId;
    this.memberId = memberId;
    this.joinedAt = Instant.now();
  }

  UUID getMemberId() {
    return memberId;
  }
}
