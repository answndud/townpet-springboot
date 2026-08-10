package com.townpet.engagement;

import com.townpet.publication.api.PublicationAccess;
import com.townpet.relationship.api.BlockDirectory;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DataAccessException;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class CommentService {
  private final CommentRepository comments;
  private final PublicationAccess publications;
  private final BlockDirectory blocks;

  CommentService(
      CommentRepository comments, PublicationAccess publications, BlockDirectory blocks) {
    this.comments = comments;
    this.publications = publications;
    this.blocks = blocks;
  }

  @Transactional(readOnly = true)
  List<CommentEntity> list(UUID publicationId, @Nullable UUID viewerMemberId) {
    requireAccessiblePublication(publicationId, viewerMemberId);
    return comments.findByPublicationIdAndLifecycleOrderByCreatedAtAscIdAsc(
        publicationId, CommentLifecycle.ACTIVE);
  }

  @Transactional
  CommentEntity create(UUID memberId, UUID publicationId, String body) {
    requireAccessiblePublication(publicationId, memberId);
    try {
      return comments.saveAndFlush(new CommentEntity(publicationId, memberId, body.trim()));
    } catch (DataAccessException exception) {
      throw new CommentPublicationNotFoundException();
    }
  }

  @Transactional
  void delete(UUID memberId, UUID publicationId, UUID commentId, long expectedVersion) {
    requireAccessiblePublication(publicationId, memberId);
    CommentEntity comment =
        comments
            .findByIdAndLifecycle(commentId, CommentLifecycle.ACTIVE)
            .orElseThrow(CommentNotFoundException::new);
    if (!comment.getPublicationId().equals(publicationId)) {
      throw new CommentNotFoundException();
    }
    if (!comment.getAuthorMemberId().equals(memberId)) {
      throw new CommentOwnershipException();
    }
    if (comment.getVersion() != expectedVersion) {
      throw new CommentVersionConflictException();
    }
    comment.delete(Instant.now());
    comments.saveAndFlush(comment);
  }

  private void requireAccessiblePublication(UUID publicationId, @Nullable UUID viewerMemberId) {
    UUID authorId =
        publications
            .activeAuthorMemberId(publicationId)
            .orElseThrow(CommentPublicationNotFoundException::new);
    if (viewerMemberId != null && blocks.isBlocked(viewerMemberId, authorId)) {
      throw new CommentPublicationNotFoundException();
    }
  }
}

final class CommentPublicationNotFoundException extends RuntimeException {}

final class CommentNotFoundException extends RuntimeException {}

final class CommentOwnershipException extends RuntimeException {}

final class CommentVersionConflictException extends RuntimeException {}
