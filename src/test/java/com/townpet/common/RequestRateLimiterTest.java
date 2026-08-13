package com.townpet.common;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class RequestRateLimiterTest {
  @Test
  void separatesBucketsAndKeys() {
    RequestRateLimiter limiter = new RequestRateLimiter();

    limiter.requireCapacity("login", "first-ip", 1, Duration.ofMinutes(1));
    limiter.requireCapacity("login", "second-ip", 1, Duration.ofMinutes(1));
    limiter.requireCapacity("guest", "first-ip", 1, Duration.ofMinutes(1));

    assertThrows(
        ResponseStatusException.class,
        () -> limiter.requireCapacity("login", "first-ip", 1, Duration.ofMinutes(1)));
  }
}
