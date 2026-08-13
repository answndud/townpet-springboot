package com.townpet.operations;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.townpet.common.RequestRateLimiter;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class PublicIngressRateLimiterTest {
  @Test
  void capsAnonymousIngressPerApplicationWindow() {
    RequestRateLimiter limiter = new RequestRateLimiter();

    for (int index = 0; index < 600; index++) {
      limiter.requireCapacity("public-telemetry", "127.0.0.1", 600, Duration.ofMinutes(1));
    }

    assertThrows(
        ResponseStatusException.class,
        () -> limiter.requireCapacity("public-telemetry", "127.0.0.1", 600, Duration.ofMinutes(1)));
  }
}
