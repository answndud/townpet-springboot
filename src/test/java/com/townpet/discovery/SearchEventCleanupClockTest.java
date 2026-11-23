package com.townpet.discovery;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class SearchEventCleanupClockTest {
  @Test
  void deletesEventsCreatedBeforeExactlyNowMinusRetention() {
    Instant now = Instant.parse("2026-05-15T00:00:00Z");
    SearchEventRepository events = mock(SearchEventRepository.class);
    SearchEventCleanup cleanup =
        new SearchEventCleanup(events, Duration.ofDays(30), Clock.fixed(now, ZoneOffset.UTC));

    cleanup.deleteExpired();

    verify(events).deleteByCreatedAtBefore(Instant.parse("2026-04-15T00:00:00Z"));
  }
}
