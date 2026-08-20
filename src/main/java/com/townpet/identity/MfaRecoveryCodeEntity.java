package com.townpet.identity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.springframework.lang.Nullable;

@Entity
@Table(name = "identity_mfa_recovery_code")
class MfaRecoveryCodeEntity {
  @Id private UUID id;

  @Column(nullable = false)
  private UUID memberId;

  @Column(nullable = false, length = 100)
  private String codeHash;

  @Nullable private Instant usedAt;

  @Column(nullable = false)
  private Instant createdAt;

  protected MfaRecoveryCodeEntity() {}

  MfaRecoveryCodeEntity(UUID memberId, String codeHash) {
    this.id = UUID.randomUUID();
    this.memberId = memberId;
    this.codeHash = codeHash;
    this.createdAt = Instant.now();
  }

  UUID getId() {
    return id;
  }

  String getCodeHash() {
    return codeHash;
  }

  @Nullable
  Instant getUsedAt() {
    return usedAt;
  }

  void markUsed(Instant now) {
    usedAt = now;
  }
}
