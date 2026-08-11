package com.townpet.operations;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface AcquisitionEventRepository extends JpaRepository<AcquisitionEventEntity, UUID> {}
