package com.townpet.relationship;

import com.townpet.common.CurrentMember;
import com.townpet.common.MemberOnly;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
      @CurrentMember UUID currentMemberId, @PathVariable UUID targetMemberId) {
    return toResponse(relationships.state(currentMemberId, targetMemberId));
  }

  @PutMapping
  @MemberOnly
  RelationshipResponse set(
      @CurrentMember UUID currentMemberId,
      @PathVariable UUID targetMemberId,
      @Valid @RequestBody SetRelationshipRequest request) {
    return toResponse(
        relationships.set(
            currentMemberId, targetMemberId, request.following(), request.blocking()));
  }

  private static RelationshipResponse toResponse(RelationshipService.RelationshipState state) {
    return new RelationshipResponse(state.following(), state.blocking());
  }

  record SetRelationshipRequest(@NotNull Boolean following, @NotNull Boolean blocking) {}

  record RelationshipResponse(boolean following, boolean blocking) {}
}
