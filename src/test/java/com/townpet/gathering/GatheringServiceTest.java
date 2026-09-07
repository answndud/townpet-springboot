package com.townpet.gathering;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GatheringServiceTest {
  @Test
  void listAggregatesParticipantCountsWithOneQueryForAllGatherings() {
    GatheringRepository gatherings = mock(GatheringRepository.class);
    GatheringParticipantRepository participants = mock(GatheringParticipantRepository.class);
    List<GatheringEntity> entities = java.util.stream.IntStream.range(0, 100)
        .mapToObj(i -> new GatheringEntity(UUID.randomUUID(), UUID.randomUUID(), "g" + i, "d", "l", Instant.now(), 10))
        .toList();
    when(gatherings.findTop100ByStatusOrderByStartsAtAscIdAsc(GatheringStatus.ACTIVE)).thenReturn(entities);
    when(participants.countByGatheringIdIn(anyCollection())).thenReturn(List.of());

    List<GatheringService.GatheringView> result = new GatheringService(gatherings, participants).list();

    assertThat(result).hasSize(100);
    verify(participants).countByGatheringIdIn(argThat(ids -> ids.size() == 100));
    verify(participants, never()).countByGatheringId(any());
  }
}
