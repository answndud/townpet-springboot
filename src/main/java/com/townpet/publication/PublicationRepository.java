package com.townpet.publication;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface PublicationRepository extends JpaRepository<PublicationEntity, UUID> {
  Optional<PublicationEntity> findByIdAndLifecycle(UUID id, PublicationLifecycle lifecycle);

  boolean existsByIdAndLifecycle(UUID id, PublicationLifecycle lifecycle);

  @Query(
      "select p from PublicationEntity p where p.authorMemberId = :authorMemberId"
          + " and p.lifecycle = :lifecycle order by p.createdAt desc, p.id desc")
  List<PublicationEntity> findAuthorPage(
      @Param("authorMemberId") UUID authorMemberId,
      @Param("lifecycle") PublicationLifecycle lifecycle,
      Pageable page);

  @Query(
      "select p from PublicationEntity p where p.authorMemberId = :authorMemberId"
          + " and p.lifecycle = :lifecycle"
          + " and (p.createdAt < :createdAt or (p.createdAt = :createdAt and p.id < :id))"
          + " order by p.createdAt desc, p.id desc")
  List<PublicationEntity> findAuthorPageBefore(
      @Param("authorMemberId") UUID authorMemberId,
      @Param("lifecycle") PublicationLifecycle lifecycle,
      @Param("createdAt") Instant createdAt,
      @Param("id") UUID id,
      Pageable page);

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
