package com.townpet.identity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "email_verification_token")
public class EmailVerificationTokenEntity {
  @Id private UUID id;

  @Column(nullable = false)
  private UUID memberId;

  @Column(nullable = false, unique = true, length = 64)
  private String tokenHash;

  @Column(nullable = false)
  private Instant expiresAt;

  @Column(nullable = false)
  private Instant createdAt;

  @Version private long version;

  protected EmailVerificationTokenEntity() {}

  public EmailVerificationTokenEntity(UUID memberId, String tokenHash, Instant expiresAt) {
    this(memberId, tokenHash, Instant.now(), expiresAt);
  }

  EmailVerificationTokenEntity(
      UUID memberId, String tokenHash, Instant createdAt, Instant expiresAt) {
    this.id = UUID.randomUUID();
    this.memberId = memberId;
    this.tokenHash = tokenHash;
    this.createdAt = createdAt;
    this.expiresAt = expiresAt;
  }

  public UUID getMemberId() {
    return memberId;
  }

  public String getTokenHash() {
    return tokenHash;
  }
}
