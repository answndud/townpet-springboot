package com.townpet.relationship;

import com.townpet.common.MemberOnly;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import org.springframework.http.HttpStatus;
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
@RequestMapping({
  "/api/v1/members/{targetMemberId}/relationship",
  "/api/users/{targetMemberId}/relation"
})
class RelationshipController {
  private final RelationshipService relationships;

  RelationshipController(RelationshipService relationships) {
    this.relationships = relationships;
  }

  @GetMapping
  @MemberOnly
  RelationshipResponse get(
      @AuthenticationPrincipal UserDetails principal, @PathVariable UUID targetMemberId) {
    try {
      return toResponse(relationships.state(memberId(principal), targetMemberId));
    } catch (RelationshipTargetNotFoundException exception) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }
  }

  @PutMapping
  @MemberOnly
  RelationshipResponse set(
      @AuthenticationPrincipal UserDetails principal,
      @PathVariable UUID targetMemberId,
      @Valid @RequestBody SetRelationshipRequest request) {
    try {
      return toResponse(
          relationships.set(
              memberId(principal), targetMemberId, request.following(), request.blocking()));
    } catch (RelationshipTargetNotFoundException exception) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    } catch (RelationshipSelfTargetException exception) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot target yourself");
    }
  }

  private static UUID memberId(UserDetails principal) {
    try {
      return UUID.fromString(principal.getUsername());
    } catch (IllegalArgumentException exception) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid principal");
    }
  }

  private static RelationshipResponse toResponse(RelationshipService.RelationshipState state) {
    return new RelationshipResponse(state.following(), state.blocking());
  }

  record SetRelationshipRequest(@NotNull Boolean following, @NotNull Boolean blocking) {}

  record RelationshipResponse(boolean following, boolean blocking) {}
}
