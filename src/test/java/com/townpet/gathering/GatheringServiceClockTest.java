package com.townpet.gathering;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class GatheringServiceClockTest {
  private static final Instant NOW = Instant.parse("2026-06-01T09:00:00Z");
  private static final UUID ID = UUID.fromString("00000000-0000-4000-8000-000000000401");
  private static final UUID HOST = UUID.fromString("00000000-0000-4000-8000-000000000201");
  private static final UUID MEMBER = UUID.fromString("00000000-0000-4000-8000-000000000202");

  private final GatheringRepository gatherings = mock(GatheringRepository.class);
  private final GatheringParticipantRepository participants =
      mock(GatheringParticipantRepository.class);
  private final GatheringService service =
      new GatheringService(gatherings, participants, Clock.fixed(NOW, ZoneOffset.UTC));

  @Test
  void rejectsJoiningAtTheStartInstantAndAllowsOneSecondBefore() {
    when(gatherings.findForUpdate(ID)).thenReturn(Optional.of(gatheringStartingAt(NOW)));
    assertThatThrownBy(() -> service.join(ID, MEMBER))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("already started");

    when(gatherings.findForUpdate(ID))
        .thenReturn(Optional.of(gatheringStartingAt(NOW.plusSeconds(1))));
    when(participants.findByGatheringIdAndMemberId(ID, MEMBER)).thenReturn(Optional.empty());
    when(participants.countByGatheringId(ID)).thenReturn(0L);
    when(participants.save(any(GatheringParticipantEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    assertThatCode(() -> service.join(ID, MEMBER)).doesNotThrowAnyException();
  }

  private static GatheringEntity gatheringStartingAt(Instant startsAt) {
    return new GatheringEntity(ID, HOST, "산책", "설명", "마포", startsAt, 8, NOW.minusSeconds(60));
  }
}
