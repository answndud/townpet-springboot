package com.townpet.identity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.springframework.lang.Nullable;

@Entity
@Table(name = "identity_mfa_factor")
class MfaFactorEntity {
  @Id
  @Column(name = "member_id")
  private UUID memberId;

  @Column(nullable = false, length = 512)
  private String secretCiphertext;

  @Column(nullable = false)
  private Instant enrollmentExpiresAt;

  @Nullable private Instant enabledAt;

  @Column(nullable = false)
  private Instant createdAt;

  @Column(nullable = false)
  private Instant updatedAt;

  protected MfaFactorEntity() {}

  MfaFactorEntity(UUID memberId, String secretCiphertext, Instant enrollmentExpiresAt) {
    this.memberId = memberId;
    this.secretCiphertext = secretCiphertext;
    this.enrollmentExpiresAt = enrollmentExpiresAt;
    this.createdAt = Instant.now();
    this.updatedAt = this.createdAt;
  }

  UUID getMemberId() {
    return memberId;
  }

  String getSecretCiphertext() {
    return secretCiphertext;
  }

  Instant getEnrollmentExpiresAt() {
    return enrollmentExpiresAt;
  }

  @Nullable
  Instant getEnabledAt() {
    return enabledAt;
  }

  void enable(Instant now) {
    enabledAt = now;
    updatedAt = now;
  }

  void replaceEnrollment(String secretCiphertext, Instant expiresAt, Instant now) {
    this.secretCiphertext = secretCiphertext;
    this.enrollmentExpiresAt = expiresAt;
    this.enabledAt = null;
    this.updatedAt = now;
  }
}
