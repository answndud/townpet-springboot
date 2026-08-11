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
@Table(name = "guest_author")
class GuestAuthorEntity {
  @Id private UUID id;

  @Column(nullable = false, unique = true)
  private UUID publicId;

  @Column(nullable = false, length = 100)
  private String managementPasswordHash;

  @Column(nullable = false)
  private int failedAttempts;

  @Nullable private Instant lockedUntil;

  @Column(nullable = false)
  private Instant createdAt;

  protected GuestAuthorEntity() {}

  GuestAuthorEntity(String passwordHash) {
    this.id = UuidV7.randomUuid();
    this.publicId = UuidV7.randomUuid();
    this.managementPasswordHash = passwordHash;
    this.createdAt = Instant.now();
  }

  UUID getId() {
    return id;
  }

  UUID getPublicId() {
    return publicId;
  }

  String getManagementPasswordHash() {
    return managementPasswordHash;
  }

  int getFailedAttempts() {
    return failedAttempts;
  }

  @Nullable
  Instant getLockedUntil() {
    return lockedUntil;
  }

  void recordFailure(@Nullable Instant until) {
    failedAttempts++;
    lockedUntil = until;
  }

  void clearFailures() {
    failedAttempts = 0;
    lockedUntil = null;
  }
}
