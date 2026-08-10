package com.townpet.engagement;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/publications/{publicationId}/reaction")
class ReactionController {
  private final ReactionService reactions;

  ReactionController(ReactionService reactions) {
    this.reactions = reactions;
  }

  @GetMapping
  ReactionResponse get(
      @PathVariable UUID publicationId, @AuthenticationPrincipal @Nullable UserDetails principal) {
    try {
      return toResponse(reactions.state(publicationId, memberId(principal)));
    } catch (ReactionPublicationNotFoundException exception) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }
  }

  @PutMapping
  ReactionResponse set(
      @AuthenticationPrincipal UserDetails principal,
      @PathVariable UUID publicationId,
      @Valid @RequestBody SetReactionRequest request) {
    try {
      return toResponse(
          reactions.set(authenticatedMemberId(principal), publicationId, request.active()));
    } catch (ReactionPublicationNotFoundException exception) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }
  }

  @Nullable
  private static UUID memberId(@Nullable UserDetails principal) {
    if (principal == null) return null;
    try {
      return UUID.fromString(principal.getUsername());
    } catch (IllegalArgumentException exception) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid principal");
    }
  }

  private static UUID authenticatedMemberId(UserDetails principal) {
    UUID memberId = memberId(principal);
    if (memberId == null) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
    }
    return memberId;
  }

  private static ReactionResponse toResponse(ReactionService.ReactionState state) {
    return new ReactionResponse(state.active(), state.count());
  }

  record SetReactionRequest(@NotNull Boolean active) {}

  record ReactionResponse(boolean active, long count) {}
}
