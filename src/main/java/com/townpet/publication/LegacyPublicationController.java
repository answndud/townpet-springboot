package com.townpet.publication;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
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
      @PathVariable UUID publicationId, @AuthenticationPrincipal @Nullable UserDetails principal) {
    return publications
        .findVisible(publicationId, memberId(principal))
        .map(LegacyPublicationController::response)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
  }

  @Nullable
  private static UUID memberId(@Nullable UserDetails principal) {
    if (principal == null) return null;
    try {
      return UUID.fromString(principal.getUsername());
    } catch (IllegalArgumentException exception) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
    }
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
