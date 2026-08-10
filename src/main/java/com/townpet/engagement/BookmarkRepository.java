package com.townpet.engagement;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface BookmarkRepository extends JpaRepository<BookmarkEntity, UUID> {
  Optional<BookmarkEntity> findByPublicationIdAndMemberId(UUID publicationId, UUID memberId);
}
