package com.townpet.care;

import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

interface CareRequestRepository extends JpaRepository<CareRequestEntity, UUID> {
  List<CareRequestEntity> findTop100ByStatusOrderByStartsAtAscIdAsc(CareRequestStatus status);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<CareRequestEntity> findForUpdateByIdAndStatus(UUID id, CareRequestStatus status);

  Optional<CareRequestEntity> findByIdAndStatus(UUID id, CareRequestStatus status);
}
