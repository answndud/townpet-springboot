package com.townpet.gathering;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

interface GatheringRepository extends JpaRepository<GatheringEntity, UUID> {
  List<GatheringEntity> findTop100ByStatusAndStartsAtAfterOrderByStartsAtAscIdAsc(
      GatheringStatus status, Instant startsAt);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select g from GatheringEntity g where g.id = :id")
  Optional<GatheringEntity> findForUpdate(@Param("id") UUID id);
}
