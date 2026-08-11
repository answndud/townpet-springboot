package com.townpet.identity;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface GuestAuthorRepository extends JpaRepository<GuestAuthorEntity, UUID> {
  Optional<GuestAuthorEntity> findByPublicId(UUID publicId);
}
