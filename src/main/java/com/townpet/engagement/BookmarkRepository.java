package com.townpet.engagement;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface BookmarkRepository extends JpaRepository<BookmarkEntity, UUID> {
  Optional<BookmarkEntity> findByPublicationIdAndMemberId(UUID publicationId, UUID memberId);

  List<BookmarkEntity> findTop100ByMemberIdOrderByCreatedAtDescIdDesc(UUID memberId);
}
