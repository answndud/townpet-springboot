package com.townpet.media.api;

import java.time.Instant;

/** Operational media lifecycle actions exposed to the operations module. */
public interface MediaOperations {
  CleanupReport inspectExpiredUploads(Instant now);

  CleanupReport cleanupExpiredUploads(Instant now);

  record CleanupReport(
      int candidateCount, long candidateBytes, int deletedCount, Instant observedAt) {}
}
