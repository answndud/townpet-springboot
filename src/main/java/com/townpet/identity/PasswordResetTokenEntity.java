package com.townpet.identity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import org.springframework.lang.Nullable;

@Entity
@Table(name = "password_reset_token")
public class PasswordResetTokenEntity {
  @Id private UUID id;

  @Column(nullable = false)
  private UUID memberId;

  @Column(nullable = false, unique = true, length = 64)
  private String tokenHash;

  @Column(nullable = false)
  private Instant expiresAt;

  @Nullable private Instant usedAt;

  @Column(nullable = false)
  private Instant createdAt;

  @Version private long version;

  protected PasswordResetTokenEntity() {}

  public PasswordResetTokenEntity(UUID memberId, String tokenHash, Instant expiresAt) {
    this(memberId, tokenHash, Instant.now(), expiresAt);
  }

  PasswordResetTokenEntity(UUID memberId, String tokenHash, Instant createdAt, Instant expiresAt) {
    this.id = UUID.randomUUID();
    this.memberId = memberId;
    this.tokenHash = tokenHash;
    this.expiresAt = expiresAt;
    this.createdAt = createdAt;
  }

  public UUID getMemberId() {
    return memberId;
  }

  public String getTokenHash() {
    return tokenHash;
  }

  @Nullable
  public Instant getUsedAt() {
    return usedAt;
  }

  public void markUsed(Instant usedAt) {
    this.usedAt = usedAt;
  }
}
