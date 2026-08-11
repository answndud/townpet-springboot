package com.townpet.operations;

import com.townpet.common.UuidV7;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "moderation_action")
public class ModerationActionEntity {
  @Id private UUID id;

  @Column(nullable = false)
  private UUID actorMemberId;

  private UUID targetMemberId;

  @Column(nullable = false, length = 30)
  private String targetType;

  private UUID targetId;

  @Column(nullable = false, length = 40)
  private String action;

  @Column(length = 500)
  private String reason;

  @Column(nullable = false)
  private Instant createdAt;

  protected ModerationActionEntity() {}

  public ModerationActionEntity(
      UUID actor, UUID targetMember, String type, UUID target, String action, String reason) {
    this.id = UuidV7.randomUuid();
    this.actorMemberId = actor;
    this.targetMemberId = targetMember;
    this.targetType = type;
    this.targetId = target;
    this.action = action;
    this.reason = reason;
    this.createdAt = Instant.now();
  }
}
