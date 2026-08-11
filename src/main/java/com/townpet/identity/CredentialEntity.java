package com.townpet.identity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.springframework.lang.Nullable;

@Entity
@Table(name = "identity_credential")
public class CredentialEntity {
  @Id private UUID id;

  @Column(name = "member_id", nullable = false)
  private UUID memberId;

  @Column(nullable = false, unique = true, length = 320, columnDefinition = "citext")
  private String email;

  @Column(name = "password_hash", nullable = false, length = 100)
  private String passwordHash;

  @Column(nullable = false)
  private boolean enabled = true;

  @Column(nullable = false, length = 30)
  private String role = "MEMBER";

  @Column(nullable = false)
  private boolean lifecycleLocked;

  @Nullable private Instant emailVerifiedAt;

  protected CredentialEntity() {}

  public CredentialEntity(UUID memberId, String email, String passwordHash) {
    this(memberId, email, passwordHash, "MEMBER");
  }

  public CredentialEntity(UUID memberId, String email, String passwordHash, String role) {
    this(memberId, email, passwordHash, role, false);
  }

  public CredentialEntity(
      UUID memberId, String email, String passwordHash, String role, boolean lifecycleLocked) {
    this.id = UUID.randomUUID();
    this.memberId = memberId;
    this.email = email;
    this.passwordHash = passwordHash;
    this.role = role;
    this.lifecycleLocked = lifecycleLocked;
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

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public String getRole() {
    return role;
  }

  public boolean isLifecycleLocked() {
    return lifecycleLocked;
  }

  public void changePassword(String passwordHash) {
    if (lifecycleLocked) {
      throw new IllegalStateException("Credential lifecycle is locked");
    }
    this.passwordHash = passwordHash;
  }

  public boolean isEmailVerified() {
    return emailVerifiedAt != null;
  }

  public void verifyEmail(Instant verifiedAt) {
    if (lifecycleLocked) {
      throw new IllegalStateException("Credential lifecycle is locked");
    }
    this.emailVerifiedAt = verifiedAt;
  }
}
