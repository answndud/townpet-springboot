package com.townpet.engagement;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface CommentRepository extends JpaRepository<CommentEntity, UUID> {
  List<CommentEntity> findTop500ByPublicationIdAndLifecycleOrderByCreatedAtAscIdAsc(
      UUID publicationId, CommentLifecycle lifecycle);

  Optional<CommentEntity> findByIdAndLifecycle(UUID id, CommentLifecycle lifecycle);

  List<CommentEntity> findTop100ByAuthorMemberIdAndLifecycleOrderByCreatedAtDescIdDesc(
      UUID authorMemberId, CommentLifecycle lifecycle);
}
