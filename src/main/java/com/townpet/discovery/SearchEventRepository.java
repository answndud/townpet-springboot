package com.townpet.discovery;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SearchEventRepository extends JpaRepository<SearchEventEntity, UUID> {
  Optional<SearchEventEntity> findByClientEventId(UUID clientEventId);
}
