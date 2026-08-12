package com.townpet.care;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

interface CareRequestRepository extends JpaRepository<CareRequestEntity, UUID> {
  List<CareRequestEntity> findByStatusOrderByStartsAtAscIdAsc(CareRequestStatus status);

  Optional<CareRequestEntity> findByIdAndStatus(UUID id, CareRequestStatus status);
}
