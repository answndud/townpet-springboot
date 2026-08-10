package com.townpet.relationship;

import com.townpet.relationship.api.BlockDirectory;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
class BlockDirectoryAdapter implements BlockDirectory {
  private final BlockRepository blocks;

  BlockDirectoryAdapter(BlockRepository blocks) {
    this.blocks = blocks;
  }

  @Override
  @Transactional(readOnly = true)
  public boolean isBlocked(UUID viewerMemberId, UUID authorMemberId) {
    return blocks
        .findByBlockerMemberIdAndBlockedMemberId(viewerMemberId, authorMemberId)
        .isPresent();
  }

  @Override
  @Transactional(readOnly = true)
  public Set<UUID> blockedAuthorIds(UUID viewerMemberId) {
    return Set.copyOf(blocks.findBlockedMemberIdsByBlockerMemberId(viewerMemberId));
  }
}
