package com.townpet.publication;

import com.townpet.common.CurrentMember;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/posts/{publicationId}")
class LegacyPublicationController {
  private final PublicationService publications;

  LegacyPublicationController(PublicationService publications) {
    this.publications = publications;
  }

  @GetMapping({"/detail", "/content"})
  PublicationController.PublicationResponse get(
      @PathVariable UUID publicationId, @CurrentMember(required = false) @Nullable UUID currentMemberId) {
    return publications
        .findVisible(publicationId, currentMemberId)
        .map(LegacyPublicationController::response)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
  }

  private static PublicationController.PublicationResponse response(PublicationEntity p) {
    return new PublicationController.PublicationResponse(
        p.getId(),
        p.getType(),
        p.getAuthorMemberId(),
        p.getTitle(),
        p.getBody(),
        p.getLifecycle(),
        p.getCreatedAt(),
        p.getUpdatedAt(),
        p.getVersion());
  }
}
