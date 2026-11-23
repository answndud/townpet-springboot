package com.townpet.engagement;

import com.townpet.common.CurrentMember;
import com.townpet.common.MemberOnly;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping({
  "/api/v1/publications/{publicationId}/reaction",
  "/api/posts/{publicationId}/reaction"
})
class ReactionController {
  private final ReactionService reactions;

  ReactionController(ReactionService reactions) {
    this.reactions = reactions;
  }

  @GetMapping
  ReactionResponse get(
      @PathVariable UUID publicationId, @CurrentMember(required = false) @Nullable UUID currentMemberId) {
    try {
      return toResponse(reactions.state(publicationId, currentMemberId));
    } catch (ReactionPublicationNotFoundException exception) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }
  }

  @PutMapping
  @MemberOnly
  ReactionResponse set(
      @CurrentMember UUID currentMemberId,
      @PathVariable UUID publicationId,
      @Valid @RequestBody SetReactionRequest request) {
    try {
      return toResponse(
          reactions.set(currentMemberId, publicationId, request.active()));
    } catch (ReactionPublicationNotFoundException exception) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }
  }

  private static ReactionResponse toResponse(ReactionService.ReactionState state) {
    return new ReactionResponse(state.active(), state.count());
  }

  record SetReactionRequest(@NotNull Boolean active) {}

  record ReactionResponse(boolean active, long count) {}
}
