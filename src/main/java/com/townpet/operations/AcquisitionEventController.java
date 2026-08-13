package com.townpet.operations;

import com.townpet.common.UuidV7;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.*;

@RestController
class AcquisitionEventController {
  private final AcquisitionEventRepository events;
  private final PublicIngressRateLimiter rateLimiter;

  AcquisitionEventController(
      AcquisitionEventRepository events, PublicIngressRateLimiter rateLimiter) {
    this.events = events;
    this.rateLimiter = rateLimiter;
  }

  @PostMapping("/api/acquisition/events")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void record(@Valid @RequestBody EventRequest request, HttpServletRequest httpRequest) {
    rateLimiter.requireCapacity(httpRequest);
    try {
      events.save(
          new AcquisitionEventEntity(
              UuidV7.randomUuid(),
              request.eventName(),
              request.route(),
              hash(request.anonymousKey()),
              request.clientEventId()));
    } catch (DataIntegrityViolationException exception) {
      if (request.clientEventId() == null
          || events.findByClientEventId(request.clientEventId()).isEmpty()) {
        throw exception;
      }
      // A replayed client event is already recorded; keep the endpoint idempotent.
    }
  }

  private static @Nullable String hash(@Nullable String value) {
    if (value == null || value.isBlank()) return null;
    try {
      byte[] digest =
          MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
      StringBuilder result = new StringBuilder(64);
      for (byte item : digest) result.append(String.format("%02x", item));
      return result.toString();
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException(exception);
    }
  }

  record EventRequest(
      @NotBlank @Size(max = 80) String eventName,
      @NotBlank @Size(max = 200) String route,
      @Size(max = 200) String anonymousKey,
      @Nullable UUID clientEventId) {}
}
