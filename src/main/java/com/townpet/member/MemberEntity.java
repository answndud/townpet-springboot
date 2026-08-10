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

  public MemberEntity(UUID id, String nickname) {
    this.id = id;
    this.nickname = nickname;
    this.createdAt = Instant.now();
  }

  public UUID getId() {
    return id;
  }

  public String getNickname() {
    return nickname;
  }
}
