package com.townpet.identity;

import com.townpet.common.UuidV7;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.springframework.lang.Nullable;

@Entity
@Table(name = "guest_step_up_challenge")
class GuestStepUpChallengeEntity {
  @Id private UUID id;

  @Column(nullable = false)
  private UUID guestAuthorId;

  @Column(nullable = false, length = 80)
  private String scope;

  @Column(nullable = false, unique = true, length = 64)
  private String tokenHash;

  @Column(nullable = false)
  private Instant expiresAt;

  @Nullable private Instant usedAt;

  @Column(nullable = false)
  private Instant createdAt;

  protected GuestStepUpChallengeEntity() {}

  GuestStepUpChallengeEntity(
      UUID guestAuthorId, String scope, String tokenHash, Instant expiresAt) {
    this.id = UuidV7.randomUuid();
    this.guestAuthorId = guestAuthorId;
    this.scope = scope;
    this.tokenHash = tokenHash;
    this.expiresAt = expiresAt;
    this.createdAt = Instant.now();
  }

  String getScope() {
    return scope;
  }

  Instant getExpiresAt() {
    return expiresAt;
  }

  @Nullable
  Instant getUsedAt() {
    return usedAt;
  }

  void markUsed(Instant now) {
    usedAt = now;
  }
}
