package com.townpet.member.api;

import com.townpet.member.MemberPetRepository;
import com.townpet.member.MemberProfileEntity;
import com.townpet.member.MemberProfileRepository;
import com.townpet.member.MemberRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

@Component
public class MemberDirectory {
  private final MemberRepository members;
  private final MemberProfileRepository profiles;
  private final MemberPetRepository pets;

  MemberDirectory(
      MemberRepository members, MemberProfileRepository profiles, MemberPetRepository pets) {
    this.members = members;
    this.profiles = profiles;
    this.pets = pets;
  }

  public Optional<MemberPublicationContext> findPublicationContext(UUID memberId) {
    return members
        .findById(memberId)
        .map(
            member ->
                new MemberPublicationContext(
                    profiles
                        .findByMemberId(memberId)
                        .map(profile -> profile.getNeighborhoodId())
                        .orElse(null)));
  }

  public boolean isPublicPosts(UUID memberId) {
    return profiles
        .findByMemberId(memberId)
        .map(MemberProfileEntity::isShowPublicPosts)
        .orElse(true);
  }

  public boolean isPublicComments(UUID memberId) {
    return profiles
        .findByMemberId(memberId)
        .map(MemberProfileEntity::isShowPublicComments)
        .orElse(true);
  }

  public Optional<PublicProfile> findPublicProfile(UUID memberId) {
    return members
        .findById(memberId)
        .map(
            member -> {
              MemberProfileEntity profile = profiles.findByMemberId(memberId).orElse(null);
              List<Pet> memberPets =
                  pets.findAllByMemberIdOrderByCreatedAtAsc(memberId).stream()
                      .map(pet -> new Pet(pet.getId(), pet.getName(), pet.getSpecies()))
                      .toList();
              return new PublicProfile(
                  member.getId(),
                  member.getNickname(),
                  profile == null ? null : profile.getBio(),
                  profile == null ? null : profile.getNeighborhoodId(),
                  profile == null || profile.isShowPublicPosts(),
                  profile == null || profile.isShowPublicComments(),
                  profile == null || profile.isShowPublicPets(),
                  memberPets);
            });
  }

  public record MemberPublicationContext(@Nullable UUID neighborhoodId) {}

  public record PublicProfile(
      UUID id,
      String nickname,
      @Nullable String bio,
      @Nullable UUID neighborhoodId,
      boolean showPublicPosts,
      boolean showPublicComments,
      boolean showPublicPets,
      List<Pet> pets) {}

  public record Pet(UUID id, String name, String species) {}
}
