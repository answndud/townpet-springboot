package com.townpet.gathering;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface GatheringParticipantRepository extends JpaRepository<GatheringParticipantEntity, UUID> {
  long countByGatheringId(UUID gatheringId);

  @Query("select p.gatheringId as gatheringId, count(p) as participantCount from GatheringParticipantEntity p where p.gatheringId in :gatheringIds group by p.gatheringId")
  List<ParticipantCount> countByGatheringIdIn(@Param("gatheringIds") Collection<UUID> gatheringIds);

  Optional<GatheringParticipantEntity> findByGatheringIdAndMemberId(
      UUID gatheringId, UUID memberId);

  interface ParticipantCount {
    UUID getGatheringId();
    long getParticipantCount();
  }
}
