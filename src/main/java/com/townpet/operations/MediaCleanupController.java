package com.townpet.operations;

import com.townpet.media.api.MediaOperations;
import java.time.Clock;
import java.time.Instant;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/operations/media")
@PreAuthorize("hasRole('MODERATOR')")
class MediaCleanupController {
  private final MediaOperations media;
  private final Clock clock;

  MediaCleanupController(MediaOperations media,
      Clock clock) {
    this.media = media;
    this.clock = clock;
  }

  @PostMapping("/uploads/cleanup")
  MediaOperations.CleanupReport cleanup(@RequestParam(defaultValue = "true") boolean dryRun) {
    Instant now = Instant.now(clock);
    return dryRun ? media.inspectExpiredUploads(now) : media.cleanupExpiredUploads(now);
  }
}
