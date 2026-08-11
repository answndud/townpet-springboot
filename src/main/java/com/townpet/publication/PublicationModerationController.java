package com.townpet.publication;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin/moderation/posts")
@PreAuthorize("hasRole('MODERATOR')")
class PublicationModerationController {
  private final PublicationRepository publications;
  private final JdbcTemplate jdbc;

  PublicationModerationController(PublicationRepository publications, JdbcTemplate jdbc) {
    this.publications = publications;
    this.jdbc = jdbc;
  }

  @PatchMapping("/{id}/visibility")
  @Transactional
  Response visibility(
      @PathVariable UUID id,
      @Valid @RequestBody VisibilityRequest request,
      @AuthenticationPrincipal UserDetails principal) {
    PublicationEntity publication =
        publications
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    Instant now = Instant.now();
    if (request.visible()) publication.makeVisible(now);
    else publication.hide(now);
    publications.saveAndFlush(publication);
    jdbc.update(
        "INSERT INTO moderation_action (id, actor_member_id, target_member_id, target_type, target_id, action, reason) VALUES (?, ?, ?, 'PUBLICATION', ?, ?, ?)",
        UUID.randomUUID(),
        memberId(principal),
        publication.getAuthorMemberId(),
        id,
        request.visible() ? "MAKE_VISIBLE" : "HIDE",
        request.reason());
    return new Response(id, publication.getLifecycle().name());
  }

  private static UUID memberId(UserDetails principal) {
    try {
      return UUID.fromString(principal.getUsername());
    } catch (IllegalArgumentException exception) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
    }
  }

  record VisibilityRequest(@NotNull Boolean visible, String reason) {}

  record Response(UUID id, String lifecycle) {}
}
