package com.townpet.relationship;

import com.townpet.common.UuidV7;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "relationship_follow",
    uniqueConstraints =
        @UniqueConstraint(columnNames = {"follower_member_id", "followed_member_id"}))
class FollowEntity {
  @Id private UUID id;

  @Column(nullable = false)
  private UUID followerMemberId;

  @Column(nullable = false)
  private UUID followedMemberId;

  @Column(nullable = false)
  private Instant createdAt;

  protected FollowEntity() {}

  FollowEntity(UUID followerMemberId, UUID followedMemberId) {
    this.id = UuidV7.randomUuid();
    this.followerMemberId = followerMemberId;
    this.followedMemberId = followedMemberId;
    this.createdAt = Instant.now();
  }
}
