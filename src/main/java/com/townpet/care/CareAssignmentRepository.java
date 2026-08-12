package com.townpet.care;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

interface CareAssignmentRepository extends JpaRepository<CareAssignmentEntity, UUID> {
  Optional<CareAssignmentEntity> findByRequestId(UUID requestId);
}
