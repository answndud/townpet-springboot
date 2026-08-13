package com.townpet.operations;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class PublicIngressRateLimiterTest {
  @Test
  void capsAnonymousIngressPerApplicationWindow() {
    PublicIngressRateLimiter limiter = new PublicIngressRateLimiter();

    for (int index = 0; index < 600; index++) {
      limiter.requireCapacity();
    }

    assertThrows(ResponseStatusException.class, limiter::requireCapacity);
  }
}
