package com.townpet.member;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "member_account")
public class MemberEntity {
  @Id private UUID id;

  @Column(nullable = false, unique = true, length = 40)
  private String nickname;

  @Column(nullable = false)
  private Instant createdAt;

  protected MemberEntity() {}

  /** Creates a member stamped with the current time; prefer the overload taking the time. */
  public MemberEntity(UUID id, String nickname) {
    this(id, nickname, Instant.now());
  }

  public MemberEntity(UUID id, String nickname, Instant now) {
    this.id = id;
    this.nickname = nickname;
    this.createdAt = now;
  }

  public UUID getId() {
    return id;
  }

  public String getNickname() {
    return nickname;
  }
}
