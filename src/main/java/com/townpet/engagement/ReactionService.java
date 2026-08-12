package com.townpet.engagement;

import com.townpet.notification.api.NotificationEvent;
import com.townpet.publication.api.PublicationAccess;
import com.townpet.relationship.api.BlockDirectory;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class ReactionService {
  private static final ReactionType TYPE = ReactionType.LIKE;

  private final ReactionRepository reactions;
  private final PublicationAccess publications;
  private final BlockDirectory blocks;
  private final ApplicationEventPublisher events;

  ReactionService(
      ReactionRepository reactions,
      PublicationAccess publications,
      BlockDirectory blocks,
      ApplicationEventPublisher events) {
    this.reactions = reactions;
    this.publications = publications;
    this.blocks = blocks;
    this.events = events;
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
      try {
        reactions.saveAndFlush(new ReactionEntity(publicationId, memberId, TYPE));
        publications
            .activeAuthorMemberId(publicationId)
            .ifPresent(
                recipient ->
                    events.publishEvent(
                        new NotificationEvent(
                            recipient,
                            memberId,
                            "REACTION",
                            "새 공감이 도착했습니다",
                            "게시글에 새로운 공감이 등록되었습니다.")));
      } catch (DataIntegrityViolationException exception) {
        throw new ReactionPublicationNotFoundException();
      }
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
