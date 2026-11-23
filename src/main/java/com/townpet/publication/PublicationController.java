package com.townpet.publication;

import com.townpet.catalog.api.ValidAnimalCommunityCodes;
import com.townpet.common.CurrentMember;
import com.townpet.common.MemberOnly;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/publications")
class PublicationController {
  private final PublicationService publications;

  PublicationController(PublicationService publications) {
    this.publications = publications;
  }

  @PostMapping
  @MemberOnly
  ResponseEntity<PublicationResponse> create(
      @CurrentMember UUID currentMemberId,
      @Valid @RequestBody CreatePublicationRequest request) {
    UUID memberId = currentMemberId;
    PublicationEntity publication =
        publications.create(
            memberId,
            request.type() == null ? PublicationType.FREE_BOARD : request.type(),
            request.animalInterestCode(),
            request.title(),
            request.body(),
            request.animalCommunityCodes());
    return ResponseEntity.created(URI.create("/api/v1/publications/" + publication.getId()))
        .body(toResponse(publication));
  }

  @GetMapping("/{publicationId}")
  PublicationResponse get(
      @PathVariable UUID publicationId, @CurrentMember(required = false) @Nullable UUID currentMemberId) {
    return publications
        .findVisible(publicationId, currentMemberId)
        .map(this::toResponse)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
  }

  @GetMapping("/mine")
  @MemberOnly
  List<PublicationResponse> mine(@CurrentMember UUID currentMemberId) {
    List<PublicationEntity> mine = publications.mine(currentMemberId);
    Map<UUID, List<String>> tags =
        publications.animalCommunityCodes(mine.stream().map(PublicationEntity::getId).toList());
    return mine.stream()
        .map(
            publication ->
                toResponse(publication, tags.getOrDefault(publication.getId(), List.of())))
        .toList();
  }

  @PutMapping("/{publicationId}")
  @MemberOnly
  PublicationResponse edit(
      @CurrentMember UUID currentMemberId,
      @PathVariable UUID publicationId,
      @Valid @RequestBody EditPublicationRequest request) {
    try {
      return toResponse(
          publications.edit(
              currentMemberId,
              publicationId,
              request.version(),
              request.type(),
              request.animalInterestCode(),
              request.title(),
              request.body(),
              request.animalCommunityCodes()));
    } catch (ObjectOptimisticLockingFailureException exception) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Publication has changed");
    }
  }

  @DeleteMapping("/{publicationId}")
  @MemberOnly
  ResponseEntity<Void> delete(
      @CurrentMember UUID currentMemberId,
      @PathVariable UUID publicationId,
      @Valid @RequestBody DeletePublicationRequest request) {
    try {
      PublicationEntity publication =
          publications.delete(currentMemberId, publicationId, request.version());
      return ResponseEntity.noContent().eTag("\"" + publication.getVersion() + "\"").build();
    } catch (ObjectOptimisticLockingFailureException exception) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Publication has changed");
    }
  }

  @PostMapping("/{publicationId}/restore")
  @MemberOnly
  PublicationResponse restore(
      @CurrentMember UUID currentMemberId,
      @PathVariable UUID publicationId,
      @Valid @RequestBody RestorePublicationRequest request) {
    try {
      return toResponse(
          publications.restore(currentMemberId, publicationId, request.version()));
    } catch (ObjectOptimisticLockingFailureException exception) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Publication has changed");
    }
  }

  private PublicationResponse toResponse(PublicationEntity publication) {
    return toResponse(publication, publications.animalCommunityCodes(publication.getId()));
  }

  private static PublicationResponse toResponse(
      PublicationEntity publication, List<String> animalCommunityCodes) {
    return new PublicationResponse(
        publication.getId(),
        publication.getType(),
        publication.getAuthorMemberId(),
        publication.getAnimalInterestCode(),
        animalCommunityCodes,
        publication.getTitle(),
        publication.getBody(),
        publication.getLifecycle(),
        publication.getCreatedAt(),
        publication.getUpdatedAt(),
        publication.getVersion());
  }

  record CreatePublicationRequest(
      @NotBlank @Size(max = 120) String title,
      @NotBlank @Size(max = 20000) String body,
      @Nullable PublicationType type,
      @Nullable @Size(max = 40) String animalInterestCode,
      @Nullable @Size(max = 12) @ValidAnimalCommunityCodes
          Collection<@Size(max = 40) String> animalCommunityCodes) {}

  record EditPublicationRequest(
      @NotBlank @Size(max = 120) String title,
      @NotBlank @Size(max = 20000) String body,
      @Nullable PublicationType type,
      @Nullable @Size(max = 40) String animalInterestCode,
      @Nullable @Size(max = 12) @ValidAnimalCommunityCodes
          Collection<@Size(max = 40) String> animalCommunityCodes,
      @NotNull @Min(0) Long version) {}

  record DeletePublicationRequest(@NotNull @Min(0) Long version) {}

  record RestorePublicationRequest(@NotNull @Min(0) Long version) {}

  record PublicationResponse(
      UUID id,
      PublicationType type,
      @Nullable UUID authorId,
      @Nullable String animalInterestCode,
      List<String> animalCommunityCodes,
      String title,
      String body,
      PublicationLifecycle lifecycle,
      Instant createdAt,
      Instant updatedAt,
      long version) {
    PublicationResponse(
        UUID id,
        PublicationType type,
        @Nullable UUID authorId,
        String title,
        String body,
        PublicationLifecycle lifecycle,
        Instant createdAt,
        Instant updatedAt,
        long version) {
      this(
          id,
          type,
          authorId,
          null,
          List.of(),
          title,
          body,
          lifecycle,
          createdAt,
          updatedAt,
          version);
    }
  }
}
