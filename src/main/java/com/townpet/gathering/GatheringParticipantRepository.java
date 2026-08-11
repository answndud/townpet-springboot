package com.townpet.gathering;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

interface GatheringParticipantRepository extends JpaRepository<GatheringParticipantEntity, UUID> {
  List<GatheringParticipantEntity> findAllByGatheringId(UUID gatheringId);

  Optional<GatheringParticipantEntity> findByGatheringIdAndMemberId(
      UUID gatheringId, UUID memberId);
}
