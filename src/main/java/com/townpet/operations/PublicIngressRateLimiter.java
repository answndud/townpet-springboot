package com.townpet.operations;

import com.townpet.common.ClientAddress;
import com.townpet.common.RequestRateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import org.springframework.stereotype.Component;

/** Bounds anonymous telemetry ingress when the portfolio runs as one application instance. */
@Component
final class PublicIngressRateLimiter {
  private final RequestRateLimiter limiter;
  private final ClientAddress clientAddress;

  PublicIngressRateLimiter(RequestRateLimiter limiter, ClientAddress clientAddress) {
    this.limiter = limiter;
    this.clientAddress = clientAddress;
  }

  void requireCapacity(HttpServletRequest request) {
    limiter.requireCapacity(
        "public-telemetry", clientAddress.resolve(request), 600, Duration.ofMinutes(1));
  }
}
