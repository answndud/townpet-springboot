package com.townpet.engagement.api;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.lang.Nullable;

/** Operator-only comment search and lifecycle changes implemented by the engagement module. */
public interface CommentModeration {
  List<CommentView> search(
      @Nullable UUID publicationId,
      @Nullable String authorType,
      @Nullable String lifecycle,
      @Nullable Instant createdAfter,
      @Nullable Instant createdBefore,
      int limit);

  Optional<CommentView> delete(UUID commentId);

  Optional<CommentView> restore(UUID commentId);

  boolean isActive(UUID commentId);

  record CommentView(
      UUID id,
      UUID publicationId,
      @Nullable UUID parentCommentId,
      String authorType,
      String lifecycle,
      String body,
      Instant createdAt,
      Instant updatedAt,
      long version) {}
}
