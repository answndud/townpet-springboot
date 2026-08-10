package com.townpet.member;

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
@RequestMapping("/api/v1/members/me")
public class MemberController {
  private final MemberRepository members;
  private final MemberProfileRepository profiles;

  public MemberController(MemberRepository members, MemberProfileRepository profiles) {
    this.members = members;
    this.profiles = profiles;
  }

  @GetMapping
  MemberResponse getCurrentMember(@AuthenticationPrincipal UserDetails principal) {
    MemberEntity member = findMember(principal);
    MemberProfileEntity profile = profiles.findByMemberId(member.getId()).orElse(null);
    return toResponse(member, profile);
  }

  @PutMapping("/onboarding")
  @Transactional
  MemberResponse updateOnboarding(
      @AuthenticationPrincipal UserDetails principal, @RequestBody OnboardingRequest request) {
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
    return toResponse(member, profile);
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
      MemberEntity member, @Nullable MemberProfileEntity profile) {
    return new MemberResponse(
        member.getId(),
        member.getNickname(),
        profile == null ? null : profile.getBio(),
        profile == null ? null : profile.getNeighborhoodId());
  }

  record OnboardingRequest(String bio, UUID neighborhoodId) {}

  record MemberResponse(
      UUID id, String nickname, @Nullable String bio, @Nullable UUID neighborhoodId) {}
}
