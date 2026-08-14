package com.townpet.publication;

import com.townpet.catalog.api.AnimalCommunityTagger;
import com.townpet.catalog.api.AnimalInterestCatalog;
import com.townpet.member.api.MemberDirectory;
import com.townpet.publication.api.GuestDirectory;
import com.townpet.publication.api.PublicationModeration;
import com.townpet.relationship.api.BlockDirectory;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
  private final AnimalCommunityTagger communityTags;

  PublicationService(
      PublicationRepository publications,
      MemberDirectory members,
      BlockDirectory blocks,
      GuestDirectory guests,
      AnimalCommunityTagger communityTags) {
    this.publications = publications;
    this.members = members;
    this.blocks = blocks;
    this.guests = guests;
    this.communityTags = communityTags;
  }

  @Transactional
  PublicationEntity create(
      UUID memberId,
      PublicationType type,
      @Nullable String animalInterestCode,
      String title,
      String body,
      @Nullable Collection<String> animalCommunityCodes) {
    if (members.findPublicationContext(memberId).isEmpty()) {
      throw new PublicationPolicyException("Member does not exist");
    }
    List<String> normalizedCommunityCodes = normalizeAnimalCommunityCodes(animalCommunityCodes);
    String normalizedAnimalInterestCode = normalizeAnimalInterestCode(animalInterestCode);
    if (animalCommunityCodes != null) {
      if (normalizedAnimalInterestCode != null
          && !normalizedCommunityCodes.contains(normalizedAnimalInterestCode)) {
        throw new PublicationPolicyException("Animal community codes are inconsistent");
      }
      if (normalizedAnimalInterestCode == null && !normalizedCommunityCodes.isEmpty()) {
        normalizedAnimalInterestCode = normalizedCommunityCodes.getFirst();
      }
    }
    PublicationEntity publication =
        publications.save(
            new PublicationEntity(
                memberId,
                type == null ? PublicationType.FREE_BOARD : type,
                normalizedAnimalInterestCode,
                title.trim(),
                body.trim()));
    communityTags.replace(
        "PUBLICATION",
        publication.getId(),
        animalCommunityCodes == null
            ? (normalizedAnimalInterestCode == null
                ? Set.of()
                : Set.of(normalizedAnimalInterestCode))
            : normalizedCommunityCodes);
    return publication;
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
  PublicationEntity editGuest(
      UUID guestPublicId,
      String password,
      UUID publicationId,
      long expectedVersion,
      String title,
      String body) {
    GuestDirectory.GuestIdentity guest = guests.authenticate(guestPublicId, password);
    PublicationEntity publication =
        publications
            .findByIdAndLifecycle(publicationId, PublicationLifecycle.ACTIVE)
            .orElseThrow(PublicationNotFoundException::new);
    if (!guest.internalId().equals(publication.getGuestAuthorId()))
      throw new PublicationOwnershipException();
    requireCurrentVersion(publication, expectedVersion);
    publication.edit(null, title.trim(), body.trim(), java.time.Instant.now());
    return publications.saveAndFlush(publication);
  }

  @Transactional
  void deleteGuest(UUID guestPublicId, String password, UUID publicationId, long expectedVersion) {
    GuestDirectory.GuestIdentity guest = guests.authenticate(guestPublicId, password);
    PublicationEntity publication =
        publications
            .findByIdAndLifecycle(publicationId, PublicationLifecycle.ACTIVE)
            .orElseThrow(PublicationNotFoundException::new);
    if (!guest.internalId().equals(publication.getGuestAuthorId()))
      throw new PublicationOwnershipException();
    requireCurrentVersion(publication, expectedVersion);
    publication.delete(java.time.Instant.now());
    publications.saveAndFlush(publication);
  }

  @Transactional(readOnly = true)
  List<PublicationEntity> mine(UUID memberId) {
    return publications.findTop100ByAuthorMemberIdAndLifecycleOrderByCreatedAtDescIdDesc(
        memberId, PublicationLifecycle.ACTIVE);
  }

  @Transactional(readOnly = true)
  List<String> animalCommunityCodes(UUID publicationId) {
    return communityTags.find("PUBLICATION", publicationId);
  }

  @Transactional(readOnly = true)
  Map<UUID, List<String>> animalCommunityCodes(Collection<UUID> publicationIds) {
    return communityTags.findAll("PUBLICATION", publicationIds);
  }

  @Override
  @Transactional
  public int setAuthorContentVisibility(UUID authorMemberId, boolean visible) {
    PublicationLifecycle from = visible ? PublicationLifecycle.HIDDEN : PublicationLifecycle.ACTIVE;
    PublicationLifecycle to = visible ? PublicationLifecycle.ACTIVE : PublicationLifecycle.HIDDEN;
    return publications.updateLifecycleByAuthor(authorMemberId, from, to, java.time.Instant.now());
  }

  @Transactional
  PublicationEntity edit(
      UUID memberId,
      UUID publicationId,
      long expectedVersion,
      @Nullable PublicationType type,
      @Nullable String animalInterestCode,
      String title,
      String body,
      @Nullable Collection<String> animalCommunityCodes) {
    PublicationEntity publication = ownedActivePublication(memberId, publicationId);
    requireCurrentVersion(publication, expectedVersion);
    if (members.findPublicationContext(memberId).isEmpty()) {
      throw new PublicationPolicyException("Member does not exist");
    }
    boolean replacesCommunityCodes = animalCommunityCodes != null;
    List<String> normalizedCommunityCodes = normalizeAnimalCommunityCodes(animalCommunityCodes);
    String normalizedAnimalInterestCode = normalizeAnimalInterestCode(animalInterestCode);
    if (replacesCommunityCodes) {
      if (normalizedAnimalInterestCode != null
          && !normalizedCommunityCodes.contains(normalizedAnimalInterestCode)) {
        throw new PublicationPolicyException("Animal community codes are inconsistent");
      }
      if (normalizedAnimalInterestCode == null && !normalizedCommunityCodes.isEmpty()) {
        normalizedAnimalInterestCode = normalizedCommunityCodes.getFirst();
      }
    }
    String resolvedAnimalInterestCode =
        replacesCommunityCodes
            ? normalizedAnimalInterestCode
            : (animalInterestCode == null
                ? publication.getAnimalInterestCode()
                : normalizedAnimalInterestCode);
    publication.edit(
        type == null ? publication.getType() : type,
        resolvedAnimalInterestCode,
        title.trim(),
        body.trim(),
        java.time.Instant.now());
    PublicationEntity saved = publications.saveAndFlush(publication);
    if (replacesCommunityCodes) {
      communityTags.replace("PUBLICATION", saved.getId(), normalizedCommunityCodes);
    } else if (animalInterestCode != null) {
      communityTags.replace("PUBLICATION", saved.getId(), Set.of(animalInterestCode));
    }
    return saved;
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
  private static String normalizeAnimalInterestCode(@Nullable String code) {
    if (code == null || code.isBlank()) return null;
    String normalized = code.trim().toUpperCase(java.util.Locale.ROOT);
    if (!AnimalInterestCatalog.codes().contains(normalized)) {
      throw new PublicationPolicyException("Invalid animal interest code");
    }
    return normalized;
  }

  private static List<String> normalizeAnimalCommunityCodes(
      @Nullable Collection<String> animalCommunityCodes) {
    if (animalCommunityCodes == null) return List.of();
    if (animalCommunityCodes.stream().anyMatch(code -> code == null || code.isBlank())) {
      throw new PublicationPolicyException("Invalid animal community code");
    }
    List<String> normalized =
        animalCommunityCodes.stream()
            .map(code -> code.trim().toUpperCase(Locale.ROOT))
            .distinct()
            .toList();
    if (!AnimalInterestCatalog.codes().containsAll(normalized)) {
      throw new PublicationPolicyException("Invalid animal community code");
    }
    return normalized;
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
