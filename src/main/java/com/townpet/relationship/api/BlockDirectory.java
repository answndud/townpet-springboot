package com.townpet.relationship.api;

import java.util.Set;
import java.util.UUID;

public interface BlockDirectory {
  boolean isBlocked(UUID viewerMemberId, UUID authorMemberId);

  Set<UUID> blockedAuthorIds(UUID viewerMemberId);
}
