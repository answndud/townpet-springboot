package com.townpet.relationship;

import com.townpet.member.api.MemberDirectory;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class RelationshipService {
  private final FollowRepository follows;
  private final BlockRepository blocks;
  private final MemberDirectory members;

  RelationshipService(FollowRepository follows, BlockRepository blocks, MemberDirectory members) {
    this.follows = follows;
    this.blocks = blocks;
    this.members = members;
  }

  @Transactional(readOnly = true)
  RelationshipState state(UUID viewerId, UUID targetId) {
    requireTarget(targetId);
    return new RelationshipState(
        follows.findByFollowerMemberIdAndFollowedMemberId(viewerId, targetId).isPresent(),
        blocks.findByBlockerMemberIdAndBlockedMemberId(viewerId, targetId).isPresent());
  }

  @Transactional
  RelationshipState set(UUID viewerId, UUID targetId, boolean followActive, boolean blockActive) {
    requireTarget(targetId);
    if (viewerId.equals(targetId)) throw new RelationshipSelfTargetException();
    var follow = follows.findByFollowerMemberIdAndFollowedMemberId(viewerId, targetId);
    var block = blocks.findByBlockerMemberIdAndBlockedMemberId(viewerId, targetId);
    if (blockActive) {
      follow.ifPresent(follows::delete);
      if (block.isEmpty()) blocks.save(new BlockEntity(viewerId, targetId));
    } else {
      block.ifPresent(blocks::delete);
      if (followActive && follow.isEmpty()) follows.save(new FollowEntity(viewerId, targetId));
      if (!followActive) follow.ifPresent(follows::delete);
    }
    return new RelationshipState(followActive && !blockActive, blockActive);
  }

  private void requireTarget(UUID targetId) {
    if (members.findPublicationContext(targetId).isEmpty())
      throw new RelationshipTargetNotFoundException();
  }

  record RelationshipState(boolean following, boolean blocking) {}
}

final class RelationshipTargetNotFoundException extends RuntimeException {}

final class RelationshipSelfTargetException extends RuntimeException {}
