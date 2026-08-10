package com.townpet.engagement;

import com.townpet.publication.api.PublicationAccess;
import com.townpet.relationship.api.BlockDirectory;
import java.util.UUID;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class ReactionService {
  private static final ReactionType TYPE = ReactionType.LIKE;

  private final ReactionRepository reactions;
  private final PublicationAccess publications;
  private final BlockDirectory blocks;

  ReactionService(
      ReactionRepository reactions, PublicationAccess publications, BlockDirectory blocks) {
    this.reactions = reactions;
    this.publications = publications;
    this.blocks = blocks;
  }

  @Transactional(readOnly = true)
  ReactionState state(UUID publicationId, @Nullable UUID memberId) {
    requireAccessiblePublication(publicationId, memberId);
    boolean active =
        memberId != null
            && reactions
                .findByPublicationIdAndAuthorMemberIdAndType(publicationId, memberId, TYPE)
                .isPresent();
    return new ReactionState(active, reactions.countByPublicationIdAndType(publicationId, TYPE));
  }

  @Transactional
  ReactionState set(UUID memberId, UUID publicationId, boolean active) {
    requireAccessiblePublication(publicationId, memberId);
    var existing =
        reactions.findByPublicationIdAndAuthorMemberIdAndType(publicationId, memberId, TYPE);
    if (active && existing.isEmpty()) {
      reactions.save(new ReactionEntity(publicationId, memberId, TYPE));
    } else if (!active) {
      existing.ifPresent(reactions::delete);
    }
    return new ReactionState(active, reactions.countByPublicationIdAndType(publicationId, TYPE));
  }

  private void requireAccessiblePublication(UUID publicationId, @Nullable UUID viewerMemberId) {
    UUID authorId =
        publications
            .activeAuthorMemberId(publicationId)
            .orElseThrow(ReactionPublicationNotFoundException::new);
    if (viewerMemberId != null && blocks.isBlocked(viewerMemberId, authorId)) {
      throw new ReactionPublicationNotFoundException();
    }
  }

  record ReactionState(boolean active, long count) {}
}

final class ReactionPublicationNotFoundException extends RuntimeException {}
