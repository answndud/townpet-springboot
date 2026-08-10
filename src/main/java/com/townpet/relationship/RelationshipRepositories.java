package com.townpet.relationship;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface FollowRepository extends JpaRepository<FollowEntity, UUID> {
  Optional<FollowEntity> findByFollowerMemberIdAndFollowedMemberId(
      UUID followerId, UUID followedId);

  @org.springframework.data.jpa.repository.Modifying
  @org.springframework.data.jpa.repository.Query(
      value =
          "INSERT INTO relationship_follow "
              + "(id, follower_member_id, followed_member_id, created_at) "
              + "VALUES (:id, :followerId, :followedId, CURRENT_TIMESTAMP) "
              + "ON CONFLICT (follower_member_id, followed_member_id) DO NOTHING",
      nativeQuery = true)
  int insertIfAbsent(UUID id, UUID followerId, UUID followedId);
}

interface BlockRepository extends JpaRepository<BlockEntity, UUID> {
  Optional<BlockEntity> findByBlockerMemberIdAndBlockedMemberId(UUID blockerId, UUID blockedId);

  @org.springframework.data.jpa.repository.Modifying
  @org.springframework.data.jpa.repository.Query(
      value =
          "INSERT INTO relationship_block "
              + "(id, blocker_member_id, blocked_member_id, created_at) "
              + "VALUES (:id, :blockerId, :blockedId, CURRENT_TIMESTAMP) "
              + "ON CONFLICT (blocker_member_id, blocked_member_id) DO NOTHING",
      nativeQuery = true)
  int insertIfAbsent(UUID id, UUID blockerId, UUID blockedId);

  @org.springframework.data.jpa.repository.Query(
      "select b.blockedMemberId from BlockEntity b where b.blockerMemberId = :blockerId")
  List<UUID> findBlockedMemberIdsByBlockerMemberId(UUID blockerId);
}
