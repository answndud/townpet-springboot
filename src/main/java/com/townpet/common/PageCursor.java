package com.townpet.common;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

/**
 * Keyset position {@code (createdAt, id)} of the last row of a page, shared by list endpoints.
 *
 * <p>The encoding matches the feed cursors: URL-safe Base64 of {@code <ISO instant>|<uuid>}.
 */
public record PageCursor(Instant createdAt, UUID id) {
  public static final int MAX_LIMIT = 500;

  public String encode() {
    return Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString((createdAt + "|" + id).getBytes(StandardCharsets.UTF_8));
  }

  public static PageCursor decode(String encoded) {
    try {
      String value = new String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8);
      int separator = value.indexOf('|');
      if (separator <= 0 || separator != value.lastIndexOf('|')) {
        throw new IllegalArgumentException("malformed cursor");
      }
      return new PageCursor(
          Instant.parse(value.substring(0, separator)),
          UUID.fromString(value.substring(separator + 1)));
    } catch (RuntimeException exception) {
      throw new InvalidPageRequestException("Invalid cursor");
    }
  }

  /** Validates a requested page size. */
  public static int requireLimit(int limit, int max) {
    if (limit < 1 || limit > max) {
      throw new InvalidPageRequestException("Page size must be between 1 and " + max);
    }
    return limit;
  }
}
