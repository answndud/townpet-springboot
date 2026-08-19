package com.townpet.discovery;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class SearchEventCleanupTest {
  @Test
  void deletesEventsOlderThanConfiguredRetention() {
    SearchEventRepository events = mock(SearchEventRepository.class);
    SearchEventCleanup cleanup = new SearchEventCleanup(events, Duration.ofDays(30));

    cleanup.deleteExpired();

    verify(events).deleteByCreatedAtBefore(org.mockito.ArgumentMatchers.any(Instant.class));
  }
}
