package com.townpet.gathering;

import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

interface GatheringRepository extends JpaRepository<GatheringEntity, UUID> {
  List<GatheringEntity> findTop100ByStatusOrderByStartsAtAscIdAsc(GatheringStatus status);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select g from GatheringEntity g where g.id = :id")
  Optional<GatheringEntity> findForUpdate(@Param("id") UUID id);
}
