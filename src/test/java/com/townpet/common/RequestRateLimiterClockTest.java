package com.townpet.common;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class RequestRateLimiterClockTest {
  private static final String KEY = "k".repeat(32);
  private static final Duration WINDOW = Duration.ofMinutes(1);

  @Test
  void opensANewWindowExactlyWhenTheWindowDurationHasPassed() {
    MutableClock clock = new MutableClock(Instant.parse("2026-08-01T00:00:00Z"));
    RequestRateLimiter limiter =
        new RequestRateLimiter(null, new SimpleMeterRegistry(), KEY, clock);

    limiter.requireCapacity("login", "client", 2, WINDOW);
    limiter.requireCapacity("login", "client", 2, WINDOW);
    assertThrows(
        ResponseStatusException.class, () -> limiter.requireCapacity("login", "client", 2, WINDOW));

    clock.advance(Duration.ofSeconds(59));
    assertThrows(
        ResponseStatusException.class, () -> limiter.requireCapacity("login", "client", 2, WINDOW));

    clock.advance(Duration.ofSeconds(1));
    assertDoesNotThrow(() -> limiter.requireCapacity("login", "client", 2, WINDOW));
  }

  private static final class MutableClock extends Clock {
    private Instant now;

    MutableClock(Instant start) {
      this.now = start;
    }

    void advance(Duration duration) {
      now = now.plus(duration);
    }

    @Override
    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
      return this;
    }

    @Override
    public Instant instant() {
      return now;
    }
  }
}
