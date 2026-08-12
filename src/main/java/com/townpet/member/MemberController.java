package com.townpet.member;

import com.townpet.common.MemberOnly;
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
    return toResponse(
        principal, member, profile, pets.findAllByMemberIdOrderByCreatedAtAsc(member.getId()));
  }

  @PutMapping("/me/onboarding")
  @MemberOnly
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
    return toResponse(principal, member, profile, savedPets);
  }

  @PutMapping("/me/profile")
  @MemberOnly
  @Transactional
  MemberResponse updateProfile(
      @AuthenticationPrincipal UserDetails principal,
      @Valid @RequestBody ProfileUpdateRequest request) {
    MemberEntity member = findMember(principal);
    MemberProfileEntity profile =
        profiles
            .findByMemberId(member.getId())
            .map(
                existing -> {
                  existing.updateVisibility(
                      request.showPublicPosts(),
                      request.showPublicComments(),
                      request.showPublicPets(),
                      request.showPublicReactions());
                  if (request.bio() != null)
                    existing.update(request.bio(), existing.getNeighborhoodId());
                  return existing;
                })
            .orElseGet(
                () -> {
                  MemberProfileEntity created =
                      new MemberProfileEntity(member.getId(), request.bio(), null);
                  created.updateVisibility(
                      request.showPublicPosts(),
                      request.showPublicComments(),
                      request.showPublicPets(),
                      request.showPublicReactions());
                  return created;
                });
    profiles.save(profile);
    return toResponse(
        principal, member, profile, pets.findAllByMemberIdOrderByCreatedAtAsc(member.getId()));
  }

  private MemberEntity findMember(UserDetails principal) {
    UUID memberId = memberId(principal);
    return members
        .findById(memberId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
  }

  private static UUID memberId(UserDetails principal) {
    if (principal == null) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
    }
    UUID memberId;
    try {
      memberId = UUID.fromString(principal.getUsername());
    } catch (IllegalArgumentException exception) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid principal");
    }
    return memberId;
  }

  private static MemberResponse toResponse(
      UserDetails principal,
      MemberEntity member,
      @Nullable MemberProfileEntity profile,
      List<MemberPetEntity> pets) {
    return new MemberResponse(
        member.getId(),
        member.getNickname(),
        role(principal),
        profile == null ? null : profile.getBio(),
        profile == null ? null : profile.getNeighborhoodId(),
        profile == null || profile.isShowPublicPosts(),
        profile == null || profile.isShowPublicComments(),
        profile == null || profile.isShowPublicPets(),
        profile == null || profile.isShowPublicReactions(),
        pets.stream()
            .map(pet -> new PetResponse(pet.getId(), pet.getName(), pet.getSpecies()))
            .toList());
  }

  private static String role(UserDetails principal) {
    return principal.getAuthorities().stream()
        .map(org.springframework.security.core.GrantedAuthority::getAuthority)
        .filter(authority -> authority.startsWith("ROLE_"))
        .map(authority -> authority.substring("ROLE_".length()))
        .findFirst()
        .orElse("MEMBER");
  }

  record OnboardingRequest(
      @Size(max = 500) String bio,
      UUID neighborhoodId,
      @Size(max = 10) List<@Valid PetRequest> pets) {
    OnboardingRequest {
      pets = pets == null ? List.of() : List.copyOf(pets);
    }
  }

  record ProfileUpdateRequest(
      @Size(max = 500) String bio,
      Boolean showPublicPosts,
      Boolean showPublicComments,
      Boolean showPublicPets,
      Boolean showPublicReactions) {
    ProfileUpdateRequest {
      showPublicPosts = showPublicPosts == null ? true : showPublicPosts;
      showPublicComments = showPublicComments == null ? true : showPublicComments;
      showPublicPets = showPublicPets == null ? true : showPublicPets;
      showPublicReactions = showPublicReactions == null ? true : showPublicReactions;
    }
  }

  record PetRequest(
      @NotBlank @Size(max = 40) String name, @NotBlank @Size(max = 20) String species) {}

  record MemberResponse(
      UUID id,
      String nickname,
      String role,
      @Nullable String bio,
      @Nullable UUID neighborhoodId,
      boolean showPublicPosts,
      boolean showPublicComments,
      boolean showPublicPets,
      boolean showPublicReactions,
      List<PetResponse> pets) {}

  record PetResponse(UUID id, String name, String species) {}
}
