package com.townpet.gathering;
import java.util.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
interface GatheringRepository extends JpaRepository<GatheringEntity, UUID> {
  List<GatheringEntity> findByStatusOrderByStartsAtAsc(GatheringStatus status);
  @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select g from GatheringEntity g where g.id = :id") Optional<GatheringEntity> findForUpdate(@Param("id") UUID id);
}
