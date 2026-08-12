package com.townpet.engagement;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface ReactionRepository extends JpaRepository<ReactionEntity, UUID> {
  Optional<ReactionEntity> findByPublicationIdAndAuthorMemberIdAndType(
      UUID publicationId, UUID authorMemberId, ReactionType type);

  long countByPublicationIdAndType(UUID publicationId, ReactionType type);

  List<ReactionEntity> findByAuthorMemberIdOrderByCreatedAtDesc(UUID authorMemberId);
}
