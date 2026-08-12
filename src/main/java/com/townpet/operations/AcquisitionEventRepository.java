package com.townpet.operations;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface AcquisitionEventRepository extends JpaRepository<AcquisitionEventEntity, UUID> {
  Optional<AcquisitionEventEntity> findByClientEventId(UUID clientEventId);
}
