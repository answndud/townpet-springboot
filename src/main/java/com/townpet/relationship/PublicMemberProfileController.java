package com.townpet.relationship;

import com.townpet.common.CurrentMember;
import com.townpet.member.api.MemberDirectory;
import com.townpet.member.api.MemberDirectory.PublicProfile;
import com.townpet.relationship.api.BlockDirectory;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/members/{memberId}")
class PublicMemberProfileController {
  private final MemberDirectory members;
  private final BlockDirectory blocks;

  PublicMemberProfileController(MemberDirectory members, BlockDirectory blocks) {
    this.members = members;
    this.blocks = blocks;
  }

  @GetMapping
  Response response(@CurrentMember UUID currentMemberId, @PathVariable UUID memberId) {
    UUID viewerId = currentMemberId;
    if (!viewerId.equals(memberId)
        && (blocks.isBlocked(viewerId, memberId) || blocks.isBlocked(memberId, viewerId))) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }
    PublicProfile profile =
        members
            .findPublicProfile(memberId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    List<PetResponse> pets =
        profile.showPublicPets() || viewerId.equals(memberId)
            ? profile.pets().stream()
                .map(pet -> new PetResponse(pet.id(), pet.name(), pet.species()))
                .toList()
            : List.of();
    return new Response(
        profile.id(),
        profile.nickname(),
        profile.bio(),
        profile.neighborhoodId(),
        profile.showPublicPosts(),
        profile.showPublicComments(),
        profile.showPublicPets(),
        profile.showPublicReactions(),
        pets);
  }

  record Response(
      UUID id,
      String nickname,
      @Nullable String bio,
      @Nullable UUID neighborhoodId,
      boolean showPublicPosts,
      boolean showPublicComments,
      boolean showPublicPets,
      boolean showPublicReactions,
      List<PetResponse> pets) {}

  record PetResponse(UUID id, String name, String species) {}
}
