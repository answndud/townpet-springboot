package com.townpet.member;

import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/users/{memberId}/profile-summary")
class LegacyMemberProfileController {
  private final MemberRepository members;
  private final MemberProfileRepository profiles;
  private final MemberPetRepository pets;

  LegacyMemberProfileController(
      MemberRepository members, MemberProfileRepository profiles, MemberPetRepository pets) {
    this.members = members;
    this.profiles = profiles;
    this.pets = pets;
  }

  @GetMapping
  ProfileSummary get(@PathVariable UUID memberId) {
    MemberEntity member =
        members
            .findById(memberId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    MemberProfileEntity profile = profiles.findByMemberId(memberId).orElse(null);
    List<PetSummary> petItems =
        (profile == null || profile.isShowPublicPets())
            ? pets.findAllByMemberIdOrderByCreatedAtAsc(memberId).stream()
                .map(pet -> new PetSummary(pet.getName(), pet.getSpecies()))
                .toList()
            : List.of();
    return new ProfileSummary(
        member.getId(),
        member.getNickname(),
        profile == null ? null : profile.getBio(),
        profile == null ? null : profile.getNeighborhoodId(),
        petItems);
  }

  record ProfileSummary(
      UUID id,
      String nickname,
      @Nullable String bio,
      @Nullable UUID neighborhoodId,
      List<PetSummary> pets) {}

  record PetSummary(String name, String species) {}
}
