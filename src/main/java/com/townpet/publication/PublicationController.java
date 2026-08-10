package com.townpet.publication;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
  PublicationResponse get(@PathVariable UUID publicationId) {
    return publications
        .findVisible(publicationId)
        .map(PublicationController::toResponse)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
  }

  private static UUID memberId(UserDetails principal) {
    try {
      return UUID.fromString(principal.getUsername());
    } catch (IllegalArgumentException exception) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid principal");
    }
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

  record PublicationResponse(
      UUID id,
      PublicationType type,
      PublicationScope scope,
      UUID authorId,
      @Nullable UUID neighborhoodId,
      String title,
      String body,
      PublicationLifecycle lifecycle,
      Instant createdAt,
      Instant updatedAt,
      long version) {}
}
