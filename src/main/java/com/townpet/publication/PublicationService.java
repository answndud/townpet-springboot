package com.townpet.publication;

import com.townpet.member.api.MemberDirectory;
import com.townpet.member.api.MemberDirectory.MemberPublicationContext;
import java.util.Optional;
import java.util.UUID;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class PublicationService {
  private final PublicationRepository publications;
  private final MemberDirectory members;

  PublicationService(PublicationRepository publications, MemberDirectory members) {
    this.publications = publications;
    this.members = members;
  }

  @Transactional
  PublicationEntity create(
      UUID memberId,
      PublicationScope scope,
      @Nullable UUID neighborhoodId,
      String title,
      String body) {
    MemberPublicationContext member =
        members
            .findPublicationContext(memberId)
            .orElseThrow(() -> new PublicationPolicyException("Member does not exist"));
    UUID resolvedNeighborhoodId = resolveNeighborhood(member, scope, neighborhoodId);
    return publications.save(
        new PublicationEntity(memberId, scope, resolvedNeighborhoodId, title.trim(), body.trim()));
  }

  @Transactional(readOnly = true)
  Optional<PublicationEntity> findVisible(UUID publicationId) {
    return publications.findByIdAndLifecycle(publicationId, PublicationLifecycle.ACTIVE);
  }

  @Nullable
  private static UUID resolveNeighborhood(
      MemberPublicationContext member,
      PublicationScope scope,
      @Nullable UUID requestedNeighborhoodId) {
    if (scope == PublicationScope.GLOBAL) {
      if (requestedNeighborhoodId != null) {
        throw new PublicationPolicyException("GLOBAL publication cannot select a neighborhood");
      }
      return null;
    }
    if (requestedNeighborhoodId == null
        || !requestedNeighborhoodId.equals(member.neighborhoodId())) {
      throw new PublicationPolicyException("LOCAL publication requires the member neighborhood");
    }
    return requestedNeighborhoodId;
  }
}

final class PublicationPolicyException extends RuntimeException {
  PublicationPolicyException(String message) {
    super(message);
  }
}
