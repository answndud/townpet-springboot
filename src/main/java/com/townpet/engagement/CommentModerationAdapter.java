package com.townpet.engagement;

import com.townpet.engagement.api.CommentModeration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.ArrayList;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
class CommentModerationAdapter implements CommentModeration {
  private final CommentRepository comments;
  private final EntityManager entityManager;

  CommentModerationAdapter(CommentRepository comments, EntityManager entityManager) {
    this.comments = comments;
    this.entityManager = entityManager;
  }

  @Override
  @Transactional(readOnly = true)
  public List<CommentView> search(
      @Nullable UUID publicationId,
      @Nullable String authorType,
      @Nullable String lifecycle,
      @Nullable Instant createdAfter,
      @Nullable Instant createdBefore,
      int limit) {
    CommentLifecycle state = lifecycle == null ? null : CommentLifecycle.valueOf(lifecycle);
    var builder = entityManager.getCriteriaBuilder();
    var query = builder.createQuery(CommentEntity.class);
    Root<CommentEntity> comment = query.from(CommentEntity.class);
    var predicates = new ArrayList<Predicate>();
    if (publicationId != null) {
      predicates.add(builder.equal(comment.get("publicationId"), publicationId));
    }
    if ("GUEST".equals(authorType)) {
      predicates.add(comment.get("guestAuthorId").isNotNull());
    } else if ("MEMBER".equals(authorType)) {
      predicates.add(comment.get("authorMemberId").isNotNull());
    }
    if (state != null) {
      predicates.add(builder.equal(comment.get("lifecycle"), state));
    }
    if (createdAfter != null) {
      predicates.add(builder.greaterThanOrEqualTo(comment.get("createdAt"), createdAfter));
    }
    if (createdBefore != null) {
      predicates.add(builder.lessThan(comment.get("createdAt"), createdBefore));
    }
    query
        .select(comment)
        .where(predicates.toArray(Predicate[]::new))
        .orderBy(
            builder.desc(comment.get("createdAt")), builder.desc(comment.get("id")));
    return entityManager
        .createQuery(query)
        .setMaxResults(Math.clamp(limit, 1, 100))
        .getResultList()
        .stream()
        .map(CommentModerationAdapter::view)
        .toList();
  }

  @Override
  @Transactional
  public Optional<CommentView> delete(UUID commentId) {
    return comments
        .findById(commentId)
        .filter(comment -> comment.getLifecycle() == CommentLifecycle.ACTIVE)
        .map(
            comment -> {
              comment.deleteByModerator(Instant.now());
              return view(comments.saveAndFlush(comment));
            });
  }

  @Override
  @Transactional
  public Optional<CommentView> restore(UUID commentId) {
    return comments
        .findById(commentId)
        .filter(comment -> comment.getLifecycle() == CommentLifecycle.MODERATOR_DELETED)
        .map(
            comment -> {
              comment.restoreByModerator(Instant.now());
              return view(comments.saveAndFlush(comment));
            });
  }

  @Override
  @Transactional(readOnly = true)
  public boolean isActive(UUID commentId) {
    return comments.findByIdAndLifecycle(commentId, CommentLifecycle.ACTIVE).isPresent();
  }

  private static CommentView view(CommentEntity comment) {
    return new CommentView(
        comment.getId(),
        comment.getPublicationId(),
        comment.getParentCommentId(),
        comment.getGuestAuthorId() == null ? "MEMBER" : "GUEST",
        comment.getLifecycle().name(),
        comment.getBody(),
        comment.getCreatedAt(),
        comment.getUpdatedAt(),
        comment.getVersion());
  }
}
