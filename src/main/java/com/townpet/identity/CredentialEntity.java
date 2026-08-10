package com.townpet.identity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "identity_credential")
public class CredentialEntity {
  @Id private UUID id;

  @Column(name = "member_id", nullable = false)
  private UUID memberId;

  @Column(nullable = false, unique = true, length = 320)
  private String email;

  @Column(name = "password_hash", nullable = false, length = 100)
  private String passwordHash;

  @Column(nullable = false)
  private boolean enabled = true;

  protected CredentialEntity() {}

  public CredentialEntity(UUID memberId, String email, String passwordHash) {
    this.id = UUID.randomUUID();
    this.memberId = memberId;
    this.email = email;
    this.passwordHash = passwordHash;
  }

  public UUID getMemberId() {
    return memberId;
  }

  public String getEmail() {
    return email;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public boolean isEnabled() {
    return enabled;
  }
}
