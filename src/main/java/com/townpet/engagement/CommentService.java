package com.townpet.engagement;

import com.townpet.notification.api.NotificationEvent;
import com.townpet.publication.api.GuestDirectory;
import com.townpet.publication.api.PublicationAccess;
import com.townpet.relationship.api.BlockDirectory;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class CommentService {
  private final CommentRepository comments;
  private final PublicationAccess publications;
  private final BlockDirectory blocks;
  private final GuestDirectory guests;
  private final ApplicationEventPublisher events;

  CommentService(
      CommentRepository comments,
      PublicationAccess publications,
      BlockDirectory blocks,
      GuestDirectory guests,
      ApplicationEventPublisher events) {
    this.comments = comments;
    this.publications = publications;
    this.blocks = blocks;
    this.guests = guests;
    this.events = events;
  }

  @Transactional
  CommentEntity createGuest(
      UUID guestPublicId,
      String password,
      UUID publicationId,
      @Nullable UUID parentCommentId,
      String body) {
    GuestDirectory.GuestIdentity guest = guests.authenticate(guestPublicId, password);
    requireAccessiblePublication(publicationId, null);
    if (parentCommentId != null) {
      CommentEntity parent =
          comments
              .findByIdAndLifecycle(parentCommentId, CommentLifecycle.ACTIVE)
              .orElseThrow(CommentNotFoundException::new);
      if (!parent.getPublicationId().equals(publicationId)) throw new CommentNotFoundException();
    }
    CommentEntity comment =
        comments.saveAndFlush(
            CommentEntity.forGuest(publicationId, guest.internalId(), parentCommentId, body));
    publications
        .activeAuthorMemberId(publicationId)
        .ifPresent(
            recipient ->
                events.publishEvent(
                    new NotificationEvent(
                        recipient, null, "COMMENT", "새 댓글이 달렸습니다", "게시글에 새로운 댓글이 등록되었습니다.")));
    return comment;
  }

  @Transactional(readOnly = true)
  List<CommentEntity> list(UUID publicationId, @Nullable UUID viewerMemberId) {
    requireAccessiblePublication(publicationId, viewerMemberId);
    return comments.findTop500ByPublicationIdAndLifecycleOrderByCreatedAtAscIdAsc(
        publicationId, CommentLifecycle.ACTIVE);
  }

  @Transactional
  CommentEntity create(
      UUID memberId, UUID publicationId, @Nullable UUID parentCommentId, String body) {
    requireAccessiblePublication(publicationId, memberId);
    if (parentCommentId != null) {
      CommentEntity parent =
          comments
              .findByIdAndLifecycle(parentCommentId, CommentLifecycle.ACTIVE)
              .orElseThrow(CommentNotFoundException::new);
      if (!parent.getPublicationId().equals(publicationId)) throw new CommentNotFoundException();
    }
    try {
      CommentEntity comment =
          comments.saveAndFlush(
              new CommentEntity(publicationId, memberId, parentCommentId, body.trim()));
      publications
          .activeAuthorMemberId(publicationId)
          .ifPresent(
              recipient ->
                  events.publishEvent(
                      new NotificationEvent(
                          recipient, memberId, "COMMENT", "새 댓글이 달렸습니다", "게시글에 새로운 댓글이 등록되었습니다.")));
      return comment;
    } catch (DataIntegrityViolationException exception) {
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

  @Transactional
  CommentEntity editById(UUID memberId, UUID commentId, long expectedVersion, String body) {
    CommentEntity comment = activeComment(commentId);
    requireAccessiblePublication(comment.getPublicationId(), memberId);
    requireOwnershipAndVersion(comment, memberId, expectedVersion);
    comment.edit(body, Instant.now());
    return comments.saveAndFlush(comment);
  }

  @Transactional
  void deleteById(UUID memberId, UUID commentId, long expectedVersion) {
    CommentEntity comment = activeComment(commentId);
    requireAccessiblePublication(comment.getPublicationId(), memberId);
    requireOwnershipAndVersion(comment, memberId, expectedVersion);
    comment.delete(Instant.now());
    comments.saveAndFlush(comment);
  }

  private CommentEntity activeComment(UUID commentId) {
    return comments
        .findByIdAndLifecycle(commentId, CommentLifecycle.ACTIVE)
        .orElseThrow(CommentNotFoundException::new);
  }

  private static void requireOwnershipAndVersion(
      CommentEntity comment, UUID memberId, long expectedVersion) {
    if (!comment.getAuthorMemberId().equals(memberId)) throw new CommentOwnershipException();
    if (comment.getVersion() != expectedVersion) throw new CommentVersionConflictException();
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
