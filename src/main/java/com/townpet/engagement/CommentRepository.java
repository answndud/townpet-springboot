package com.townpet.engagement;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface CommentRepository extends JpaRepository<CommentEntity, UUID> {
  @Query(
      "select c from CommentEntity c where c.publicationId = :publicationId"
          + " and c.lifecycle = :lifecycle order by c.createdAt asc, c.id asc")
  List<CommentEntity> findFirstPage(
      @Param("publicationId") UUID publicationId,
      @Param("lifecycle") CommentLifecycle lifecycle,
      Pageable page);

  @Query(
      "select c from CommentEntity c where c.publicationId = :publicationId"
          + " and c.lifecycle = :lifecycle"
          + " and (c.createdAt > :createdAt or (c.createdAt = :createdAt and c.id > :id))"
          + " order by c.createdAt asc, c.id asc")
  List<CommentEntity> findPageAfter(
      @Param("publicationId") UUID publicationId,
      @Param("lifecycle") CommentLifecycle lifecycle,
      @Param("createdAt") Instant createdAt,
      @Param("id") UUID id,
      Pageable page);

  Optional<CommentEntity> findByIdAndLifecycle(UUID id, CommentLifecycle lifecycle);

  List<CommentEntity> findTop100ByAuthorMemberIdAndLifecycleOrderByCreatedAtDescIdDesc(
      UUID authorMemberId, CommentLifecycle lifecycle);
}
