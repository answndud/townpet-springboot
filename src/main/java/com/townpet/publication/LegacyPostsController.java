package com.townpet.publication;

import com.townpet.common.MemberOnly;
import com.townpet.publication.api.PublicationFeed;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/posts")
class LegacyPostsController {
  private final PublicationFeed feed;
  private final PublicationService publications;

  LegacyPostsController(PublicationFeed feed, PublicationService publications) {
    this.feed = feed;
    this.publications = publications;
  }

  @GetMapping
  LegacyList list(
      @AuthenticationPrincipal @Nullable UserDetails principal,
      @RequestParam(defaultValue = "20") int limit,
      @RequestParam(required = false) @Nullable String cursor,
      @RequestParam(required = false) @Nullable String query) {
    if (limit < 1 || limit > 50) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "limit must be between 1 and 50");
    }
    PublicationFeed.Page page =
        feed.list(memberId(principal), principal != null, cursor, limit, query);
    return new LegacyList(page.items(), page.nextCursor(), page.hasNext());
  }

  @PostMapping
  @MemberOnly
  @ResponseStatus(HttpStatus.CREATED)
  PublicationController.PublicationResponse create(
      @AuthenticationPrincipal UserDetails principal, @Valid @RequestBody CreateRequest request) {
    try {
      PublicationEntity publication =
          publications.create(
              requiredMemberId(principal),
              request.scope() == null ? PublicationScope.GLOBAL : request.scope(),
              request.neighborhoodId(),
              request.title(),
              request.body());
      return response(publication);
    } catch (PublicationPolicyException exception) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage());
    }
  }

  @GetMapping("/{id}")
  PublicationController.PublicationResponse get(
      @PathVariable UUID id, @AuthenticationPrincipal @Nullable UserDetails principal) {
    return publications
        .findVisible(id, memberId(principal))
        .map(LegacyPostsController::response)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
  }

  @PatchMapping("/{id}")
  @MemberOnly
  PublicationController.PublicationResponse edit(
      @PathVariable UUID id,
      @AuthenticationPrincipal UserDetails principal,
      @Valid @RequestBody EditRequest request) {
    try {
      return response(
          publications.edit(
              requiredMemberId(principal),
              id,
              request.version(),
              request.scope(),
              request.neighborhoodId(),
              request.title(),
              request.body()));
    } catch (PublicationNotFoundException exception) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    } catch (PublicationOwnershipException exception) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    } catch (PublicationVersionConflictException exception) {
      throw new ResponseStatusException(HttpStatus.CONFLICT);
    } catch (PublicationPolicyException exception) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage());
    }
  }

  @DeleteMapping("/{id}")
  @MemberOnly
  void delete(
      @PathVariable UUID id,
      @AuthenticationPrincipal UserDetails principal,
      @Valid @RequestBody DeleteRequest request) {
    try {
      publications.delete(requiredMemberId(principal), id, request.version());
    } catch (PublicationNotFoundException exception) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    } catch (PublicationOwnershipException exception) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    } catch (PublicationVersionConflictException exception) {
      throw new ResponseStatusException(HttpStatus.CONFLICT);
    }
  }

  @Nullable
  private static UUID memberId(@Nullable UserDetails principal) {
    if (principal == null) return null;
    try {
      return UUID.fromString(principal.getUsername());
    } catch (IllegalArgumentException exception) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
    }
  }

  private static UUID requiredMemberId(UserDetails principal) {
    UUID memberId = memberId((UserDetails) principal);
    if (memberId == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
    return memberId;
  }

  private static PublicationController.PublicationResponse response(PublicationEntity publication) {
    return new PublicationController.PublicationResponse(
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

  record LegacyList(
      List<PublicationFeed.Item> items, @Nullable String nextCursor, boolean hasNext) {}

  record CreateRequest(
      @NotBlank @Size(max = 120) String title,
      @NotBlank @Size(max = 20000) String body,
      @Nullable PublicationScope scope,
      @Nullable UUID neighborhoodId) {}

  record EditRequest(
      @NotBlank @Size(max = 120) String title,
      @NotBlank @Size(max = 20000) String body,
      @NotNull PublicationScope scope,
      @Nullable UUID neighborhoodId,
      @NotNull @Min(0) Long version) {}

  record DeleteRequest(@NotNull @Min(0) Long version) {}
}
