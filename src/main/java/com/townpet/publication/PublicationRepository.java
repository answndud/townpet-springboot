package com.townpet.publication;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface PublicationRepository extends JpaRepository<PublicationEntity, UUID> {
  Optional<PublicationEntity> findByIdAndLifecycle(UUID id, PublicationLifecycle lifecycle);

  boolean existsByIdAndLifecycle(UUID id, PublicationLifecycle lifecycle);

  List<PublicationEntity> findTop100ByAuthorMemberIdAndLifecycleOrderByCreatedAtDescIdDesc(
      UUID authorMemberId, PublicationLifecycle lifecycle);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      "update PublicationEntity p set p.lifecycle = :toLifecycle, p.updatedAt = :changedAt, p.version = p.version + 1 "
          + "where p.authorMemberId = :authorMemberId and p.lifecycle = :fromLifecycle")
  int updateLifecycleByAuthor(
      @Param("authorMemberId") UUID authorMemberId,
      @Param("fromLifecycle") PublicationLifecycle fromLifecycle,
      @Param("toLifecycle") PublicationLifecycle toLifecycle,
      @Param("changedAt") Instant changedAt);
}
