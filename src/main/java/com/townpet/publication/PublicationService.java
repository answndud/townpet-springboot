package com.townpet.publication;

import com.townpet.member.api.MemberDirectory;
import com.townpet.member.api.MemberDirectory.MemberPublicationContext;
import com.townpet.publication.api.GuestDirectory;
import com.townpet.publication.api.PublicationModeration;
import com.townpet.relationship.api.BlockDirectory;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class PublicationService implements PublicationModeration {
  private final PublicationRepository publications;
  private final MemberDirectory members;
  private final BlockDirectory blocks;
  private final GuestDirectory guests;

  PublicationService(
      PublicationRepository publications, MemberDirectory members, BlockDirectory blocks, GuestDirectory guests) {
    this.publications = publications;
    this.members = members;
    this.blocks = blocks;
    this.guests = guests;
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
  Optional<PublicationEntity> findVisible(UUID publicationId, @Nullable UUID viewerMemberId) {
    return publications
        .findByIdAndLifecycle(publicationId, PublicationLifecycle.ACTIVE)
        .filter(
            publication ->
                viewerMemberId == null
                    || publication.getAuthorMemberId() == null
                    || !blocks.isBlocked(viewerMemberId, publication.getAuthorMemberId()));
  }

  @Transactional
  PublicationEntity createGuest(UUID guestPublicId, String password, String title, String body) {
    GuestDirectory.GuestIdentity guest = guests.authenticate(guestPublicId, password);
    return publications.save(PublicationEntity.forGuest(guest.internalId(), title, body));
  }

  @Transactional
  PublicationEntity editGuest(UUID guestPublicId, String password, UUID publicationId, long expectedVersion, String title, String body) {
    GuestDirectory.GuestIdentity guest = guests.authenticate(guestPublicId, password);
    PublicationEntity publication = publications.findByIdAndLifecycle(publicationId, PublicationLifecycle.ACTIVE)
        .orElseThrow(PublicationNotFoundException::new);
    if (!guest.internalId().equals(publication.getGuestAuthorId())) throw new PublicationOwnershipException();
    requireCurrentVersion(publication, expectedVersion);
    publication.edit(PublicationScope.GLOBAL, null, title.trim(), body.trim(), java.time.Instant.now());
    return publications.saveAndFlush(publication);
  }

  @Transactional
  void deleteGuest(UUID guestPublicId, String password, UUID publicationId, long expectedVersion) {
    GuestDirectory.GuestIdentity guest = guests.authenticate(guestPublicId, password);
    PublicationEntity publication = publications.findByIdAndLifecycle(publicationId, PublicationLifecycle.ACTIVE)
        .orElseThrow(PublicationNotFoundException::new);
    if (!guest.internalId().equals(publication.getGuestAuthorId())) throw new PublicationOwnershipException();
    requireCurrentVersion(publication, expectedVersion);
    publication.delete(java.time.Instant.now());
    publications.saveAndFlush(publication);
  }

  @Transactional(readOnly = true)
  List<PublicationEntity> mine(UUID memberId) {
    return publications.findByAuthorMemberIdAndLifecycleOrderByCreatedAtDesc(
        memberId, PublicationLifecycle.ACTIVE);
  }

  @Override
  @Transactional
  public int setAuthorContentVisibility(UUID authorMemberId, boolean visible) {
    List<PublicationEntity> owned = publications.findByAuthorMemberId(authorMemberId);
    java.time.Instant changedAt = java.time.Instant.now();
    owned.forEach(
        publication -> {
          if (visible && publication.getLifecycle() == PublicationLifecycle.HIDDEN) {
            publication.makeVisible(changedAt);
          } else if (!visible && publication.getLifecycle() == PublicationLifecycle.ACTIVE) {
            publication.hide(changedAt);
          }
        });
    publications.saveAll(owned);
    return owned.size();
  }

  @Transactional
  PublicationEntity edit(
      UUID memberId,
      UUID publicationId,
      long expectedVersion,
      PublicationScope scope,
      @Nullable UUID neighborhoodId,
      String title,
      String body) {
    PublicationEntity publication = ownedActivePublication(memberId, publicationId);
    requireCurrentVersion(publication, expectedVersion);
    MemberPublicationContext member =
        members
            .findPublicationContext(memberId)
            .orElseThrow(() -> new PublicationPolicyException("Member does not exist"));
    UUID resolvedNeighborhoodId = resolveNeighborhood(member, scope, neighborhoodId);
    publication.edit(
        scope, resolvedNeighborhoodId, title.trim(), body.trim(), java.time.Instant.now());
    return publications.saveAndFlush(publication);
  }

  @Transactional
  PublicationEntity delete(UUID memberId, UUID publicationId, long expectedVersion) {
    PublicationEntity publication = ownedActivePublication(memberId, publicationId);
    requireCurrentVersion(publication, expectedVersion);
    publication.delete(java.time.Instant.now());
    return publications.saveAndFlush(publication);
  }

  @Transactional
  PublicationEntity restore(UUID memberId, UUID publicationId, long expectedVersion) {
    PublicationEntity publication =
        publications
            .findByIdAndLifecycle(publicationId, PublicationLifecycle.DELETED)
            .orElseThrow(PublicationNotFoundException::new);
    if (!memberId.equals(publication.getAuthorMemberId())) {
      throw new PublicationOwnershipException();
    }
    requireCurrentVersion(publication, expectedVersion);
    publication.restore(java.time.Instant.now());
    return publications.saveAndFlush(publication);
  }

  private PublicationEntity ownedActivePublication(UUID memberId, UUID publicationId) {
    PublicationEntity publication =
        publications
            .findByIdAndLifecycle(publicationId, PublicationLifecycle.ACTIVE)
            .orElseThrow(PublicationNotFoundException::new);
    if (!memberId.equals(publication.getAuthorMemberId())) {
      throw new PublicationOwnershipException();
    }
    return publication;
  }

  private static void requireCurrentVersion(PublicationEntity publication, long expectedVersion) {
    if (publication.getVersion() != expectedVersion) {
      throw new PublicationVersionConflictException();
    }
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

final class PublicationNotFoundException extends RuntimeException {}

final class PublicationOwnershipException extends RuntimeException {}

final class PublicationVersionConflictException extends RuntimeException {}
