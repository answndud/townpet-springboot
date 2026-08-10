package com.townpet.engagement;

import com.townpet.publication.api.PublicationAccess;
import java.util.UUID;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class ReactionService {
  private static final ReactionType TYPE = ReactionType.LIKE;

  private final ReactionRepository reactions;
  private final PublicationAccess publications;

  ReactionService(ReactionRepository reactions, PublicationAccess publications) {
    this.reactions = reactions;
    this.publications = publications;
  }

  @Transactional(readOnly = true)
  ReactionState state(UUID publicationId, @Nullable UUID memberId) {
    requireActivePublication(publicationId);
    boolean active =
        memberId != null
            && reactions
                .findByPublicationIdAndAuthorMemberIdAndType(publicationId, memberId, TYPE)
                .isPresent();
    return new ReactionState(active, reactions.countByPublicationIdAndType(publicationId, TYPE));
  }

  @Transactional
  ReactionState set(UUID memberId, UUID publicationId, boolean active) {
    requireActivePublication(publicationId);
    var existing =
        reactions.findByPublicationIdAndAuthorMemberIdAndType(publicationId, memberId, TYPE);
    if (active && existing.isEmpty()) {
      reactions.save(new ReactionEntity(publicationId, memberId, TYPE));
    } else if (!active) {
      existing.ifPresent(reactions::delete);
    }
    return new ReactionState(active, reactions.countByPublicationIdAndType(publicationId, TYPE));
  }

  private void requireActivePublication(UUID publicationId) {
    if (!publications.existsActive(publicationId)) {
      throw new ReactionPublicationNotFoundException();
    }
  }

  record ReactionState(boolean active, long count) {}
}

final class ReactionPublicationNotFoundException extends RuntimeException {}
