package com.townpet.member;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/members")
public class MemberController {
  private final MemberRepository members;
  private final MemberProfileRepository profiles;
  private final MemberPetRepository pets;

  public MemberController(
      MemberRepository members, MemberProfileRepository profiles, MemberPetRepository pets) {
    this.members = members;
    this.profiles = profiles;
    this.pets = pets;
  }

  @GetMapping("/me")
  MemberResponse getCurrentMember(@AuthenticationPrincipal UserDetails principal) {
    if (principal == null) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
    }
    MemberEntity member = findMember(principal);
    MemberProfileEntity profile = profiles.findByMemberId(member.getId()).orElse(null);
    return toResponse(member, profile, pets.findAllByMemberIdOrderByCreatedAtAsc(member.getId()));
  }

  @GetMapping("/{memberId}")
  MemberResponse getMember(@org.springframework.web.bind.annotation.PathVariable UUID memberId) {
    MemberEntity member =
        members
            .findById(memberId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    MemberProfileEntity profile = profiles.findByMemberId(member.getId()).orElse(null);
    return toResponse(member, profile, pets.findAllByMemberIdOrderByCreatedAtAsc(member.getId()));
  }

  @PutMapping("/me/onboarding")
  @Transactional
  MemberResponse updateOnboarding(
      @AuthenticationPrincipal UserDetails principal,
      @Valid @RequestBody OnboardingRequest request) {
    MemberEntity member = findMember(principal);
    if (request.neighborhoodId() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "neighborhoodId is required");
    }
    MemberProfileEntity profile =
        profiles
            .findByMemberId(member.getId())
            .map(
                existing -> {
                  existing.update(request.bio(), request.neighborhoodId());
                  return existing;
                })
            .orElseGet(
                () ->
                    new MemberProfileEntity(
                        member.getId(), request.bio(), request.neighborhoodId()));
    profiles.save(profile);
    pets.deleteAllByMemberId(member.getId());
    List<MemberPetEntity> savedPets =
        request.pets().stream()
            .map(
                pet ->
                    new MemberPetEntity(
                        UUID.randomUUID(), member.getId(), pet.name(), pet.species()))
            .toList();
    pets.saveAll(savedPets);
    return toResponse(member, profile, savedPets);
  }

  private MemberEntity findMember(UserDetails principal) {
    UUID memberId;
    try {
      memberId = UUID.fromString(principal.getUsername());
    } catch (IllegalArgumentException exception) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid principal");
    }
    return members
        .findById(memberId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
  }

  private static MemberResponse toResponse(
      MemberEntity member, @Nullable MemberProfileEntity profile, List<MemberPetEntity> pets) {
    return new MemberResponse(
        member.getId(),
        member.getNickname(),
        profile == null ? null : profile.getBio(),
        profile == null ? null : profile.getNeighborhoodId(),
        pets.stream()
            .map(pet -> new PetResponse(pet.getId(), pet.getName(), pet.getSpecies()))
            .toList());
  }

  record OnboardingRequest(
      @Size(max = 500) String bio,
      UUID neighborhoodId,
      @Size(max = 10) List<@Valid PetRequest> pets) {
    OnboardingRequest {
      pets = pets == null ? List.of() : List.copyOf(pets);
    }
  }

  record PetRequest(
      @NotBlank @Size(max = 40) String name, @NotBlank @Size(max = 20) String species) {}

  record MemberResponse(
      UUID id,
      String nickname,
      @Nullable String bio,
      @Nullable UUID neighborhoodId,
      List<PetResponse> pets) {}

  record PetResponse(UUID id, String name, String species) {}
}
