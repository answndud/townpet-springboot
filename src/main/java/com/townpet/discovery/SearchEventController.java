package com.townpet.discovery;

import com.townpet.common.UuidV7;
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
class SearchEventController {
  private final SearchEventRepository events;

  SearchEventController(SearchEventRepository events) {
    this.events = events;
  }

  @PostMapping("/api/search/log")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void record(@Valid @RequestBody SearchLogRequest request) {
    try {
      events.save(
          new SearchEventEntity(
              UuidV7.randomUuid(),
              hash(request.query()),
              request.route(),
              request.clientEventId()));
    } catch (DataIntegrityViolationException exception) {
      if (request.clientEventId() == null
          || events.findByClientEventId(request.clientEventId()).isEmpty()) {
        throw exception;
      }
      // A replayed client event is already recorded; keep the endpoint idempotent.
    }
  }

  private static String hash(String value) {
    try {
      byte[] digest =
          MessageDigest.getInstance("SHA-256")
              .digest(value.trim().getBytes(StandardCharsets.UTF_8));
      StringBuilder hex = new StringBuilder(64);
      for (byte item : digest) hex.append(String.format("%02x", item));
      return hex.toString();
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException(exception);
    }
  }

  record SearchLogRequest(
      @NotBlank @Size(max = 200) String query,
      @NotBlank @Size(max = 200) String route,
      @Nullable UUID clientEventId) {}
}
