package com.townpet.operations;

import com.townpet.media.api.MediaOperations;
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

  MediaCleanupController(MediaOperations media) {
    this.media = media;
  }

  @PostMapping("/uploads/cleanup")
  MediaOperations.CleanupReport cleanup(@RequestParam(defaultValue = "true") boolean dryRun) {
    Instant now = Instant.now();
    return dryRun ? media.inspectExpiredUploads(now) : media.cleanupExpiredUploads(now);
  }
}
