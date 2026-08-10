package com.townpet.relationship;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface FollowRepository extends JpaRepository<FollowEntity, UUID> {
  Optional<FollowEntity> findByFollowerMemberIdAndFollowedMemberId(
      UUID followerId, UUID followedId);
}

interface BlockRepository extends JpaRepository<BlockEntity, UUID> {
  Optional<BlockEntity> findByBlockerMemberIdAndBlockedMemberId(UUID blockerId, UUID blockedId);
}
