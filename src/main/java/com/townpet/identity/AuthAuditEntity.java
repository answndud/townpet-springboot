package com.townpet.identity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "identity_auth_audit")
public class AuthAuditEntity {
  @Id private UUID id;
  private UUID memberId;

  @Column(nullable = false, length = 40)
  private String action;

  @Column(nullable = false)
  private Instant createdAt;

  protected AuthAuditEntity() {}

  public AuthAuditEntity(UUID memberId, String action) {
    this.id = UUID.randomUUID();
    this.memberId = memberId;
    this.action = action;
    this.createdAt = Instant.now();
  }

  public UUID getMemberId() {
    return memberId;
  }
}
