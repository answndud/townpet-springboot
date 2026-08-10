package com.townpet.publication;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface PublicationRepository extends JpaRepository<PublicationEntity, UUID> {
  Optional<PublicationEntity> findByIdAndLifecycle(UUID id, PublicationLifecycle lifecycle);

  boolean existsByIdAndLifecycle(UUID id, PublicationLifecycle lifecycle);
}
