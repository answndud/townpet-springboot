package com.townpet.publication;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
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
  ResponseEntity<PublicationResponse> create(
      @AuthenticationPrincipal UserDetails principal,
      @Valid @RequestBody CreatePublicationRequest request) {
    UUID memberId = memberId(principal);
    try {
      PublicationEntity publication =
          publications.create(
              memberId, request.scope(), request.neighborhoodId(), request.title(), request.body());
      return ResponseEntity.created(URI.create("/api/v1/publications/" + publication.getId()))
          .body(toResponse(publication));
    } catch (PublicationPolicyException exception) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage());
    }
  }

  @GetMapping("/{publicationId}")
  PublicationResponse get(
      @PathVariable UUID publicationId, @AuthenticationPrincipal @Nullable UserDetails principal) {
    return publications
        .findVisible(publicationId, viewerMemberId(principal))
        .map(PublicationController::toResponse)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
  }

  @GetMapping("/mine")
  List<PublicationResponse> mine(@AuthenticationPrincipal UserDetails principal) {
    return publications.mine(memberId(principal)).stream()
        .map(PublicationController::toResponse)
        .toList();
  }

  @PutMapping("/{publicationId}")
  PublicationResponse edit(
      @AuthenticationPrincipal UserDetails principal,
      @PathVariable UUID publicationId,
      @Valid @RequestBody EditPublicationRequest request) {
    try {
      return toResponse(
          publications.edit(
              memberId(principal),
              publicationId,
              request.version(),
              request.scope(),
              request.neighborhoodId(),
              request.title(),
              request.body()));
    } catch (PublicationPolicyException exception) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage());
    } catch (PublicationNotFoundException exception) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    } catch (PublicationOwnershipException exception) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "Only the author can edit this publication");
    } catch (PublicationVersionConflictException
        | ObjectOptimisticLockingFailureException exception) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Publication has changed");
    }
  }

  @DeleteMapping("/{publicationId}")
  ResponseEntity<Void> delete(
      @AuthenticationPrincipal UserDetails principal,
      @PathVariable UUID publicationId,
      @Valid @RequestBody DeletePublicationRequest request) {
    try {
      PublicationEntity publication =
          publications.delete(memberId(principal), publicationId, request.version());
      return ResponseEntity.noContent().eTag("\"" + publication.getVersion() + "\"").build();
    } catch (PublicationNotFoundException exception) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    } catch (PublicationOwnershipException exception) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "Only the author can delete this publication");
    } catch (PublicationVersionConflictException
        | ObjectOptimisticLockingFailureException exception) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Publication has changed");
    }
  }

  @PostMapping("/{publicationId}/restore")
  PublicationResponse restore(
      @AuthenticationPrincipal UserDetails principal,
      @PathVariable UUID publicationId,
      @Valid @RequestBody RestorePublicationRequest request) {
    try {
      return toResponse(
          publications.restore(memberId(principal), publicationId, request.version()));
    } catch (PublicationNotFoundException exception) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    } catch (PublicationOwnershipException exception) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "Only the author can restore this publication");
    } catch (PublicationVersionConflictException
        | ObjectOptimisticLockingFailureException exception) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Publication has changed");
    }
  }

  @Nullable
  private static UUID viewerMemberId(@Nullable UserDetails principal) {
    if (principal == null) return null;
    try {
      return UUID.fromString(principal.getUsername());
    } catch (IllegalArgumentException exception) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid principal");
    }
  }

  private static UUID memberId(UserDetails principal) {
    UUID memberId = viewerMemberId(principal);
    if (memberId == null) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
    }
    return memberId;
  }

  private static PublicationResponse toResponse(PublicationEntity publication) {
    return new PublicationResponse(
        publication.getId(),
        publication.getType(),
        publication.getScope(),
        publication.getAuthorMemberId(),
        publication.getNeighborhoodId(),
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
      @NotNull PublicationScope scope,
      @Nullable UUID neighborhoodId) {}

  record EditPublicationRequest(
      @NotBlank @Size(max = 120) String title,
      @NotBlank @Size(max = 20000) String body,
      @NotNull PublicationScope scope,
      @Nullable UUID neighborhoodId,
      @NotNull @Min(0) Long version) {}

  record DeletePublicationRequest(@NotNull @Min(0) Long version) {}

  record RestorePublicationRequest(@NotNull @Min(0) Long version) {}

  record PublicationResponse(
      UUID id,
      PublicationType type,
      PublicationScope scope,
      @Nullable UUID authorId,
      @Nullable UUID neighborhoodId,
      String title,
      String body,
      PublicationLifecycle lifecycle,
      Instant createdAt,
      Instant updatedAt,
      long version) {}
}
