package com.townpet.engagement;

import com.townpet.publication.api.PublicationAccess;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class CommentService {
  private final CommentRepository comments;
  private final PublicationAccess publications;

  CommentService(CommentRepository comments, PublicationAccess publications) {
    this.comments = comments;
    this.publications = publications;
  }

  @Transactional(readOnly = true)
  List<CommentEntity> list(UUID publicationId) {
    requireActivePublication(publicationId);
    return comments.findByPublicationIdAndLifecycleOrderByCreatedAtAscIdAsc(
        publicationId, CommentLifecycle.ACTIVE);
  }

  @Transactional
  CommentEntity create(UUID memberId, UUID publicationId, String body) {
    requireActivePublication(publicationId);
    return comments.save(new CommentEntity(publicationId, memberId, body.trim()));
  }

  @Transactional
  void delete(UUID memberId, UUID publicationId, UUID commentId, long expectedVersion) {
    requireActivePublication(publicationId);
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

  private void requireActivePublication(UUID publicationId) {
    if (!publications.existsActive(publicationId)) {
      throw new CommentPublicationNotFoundException();
    }
  }
}

final class CommentPublicationNotFoundException extends RuntimeException {}

final class CommentNotFoundException extends RuntimeException {}

final class CommentOwnershipException extends RuntimeException {}

final class CommentVersionConflictException extends RuntimeException {}
