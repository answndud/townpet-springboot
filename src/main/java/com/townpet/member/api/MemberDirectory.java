package com.townpet.member.api;

import com.townpet.member.MemberProfileRepository;
import com.townpet.member.MemberRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

@Component
public class MemberDirectory {
  private final MemberRepository members;
  private final MemberProfileRepository profiles;

  MemberDirectory(MemberRepository members, MemberProfileRepository profiles) {
    this.members = members;
    this.profiles = profiles;
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

  public record MemberPublicationContext(@Nullable UUID neighborhoodId) {}
}
