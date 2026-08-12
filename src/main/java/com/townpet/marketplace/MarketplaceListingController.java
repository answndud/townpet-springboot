package com.townpet.marketplace;

import com.townpet.common.MemberOnly;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/marketplace/listings")
class MarketplaceListingController {
  private final MarketplaceListingService listings;

  MarketplaceListingController(MarketplaceListingService listings) {
    this.listings = listings;
  }

  @PostMapping
  @MemberOnly
  @ResponseStatus(HttpStatus.CREATED)
  ListingResponse create(
      @AuthenticationPrincipal UserDetails principal,
      @Valid @RequestBody CreateListingRequest request) {
    try {
      return toResponse(
          listings.create(
              memberId(principal),
              request.kind(),
              request.title(),
              request.description(),
              request.priceKrw()));
    } catch (org.springframework.dao.DataIntegrityViolationException exception) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid listing price policy");
    }
  }

  @GetMapping
  List<ListingResponse> list(
      @org.springframework.web.bind.annotation.RequestParam(required = false)
          MarketplaceListingKind kind,
      @org.springframework.web.bind.annotation.RequestParam(defaultValue = "20") int limit) {
    if (limit < 1 || limit > 50) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "limit must be between 1 and 50");
    }
    return listings.listAvailable(Optional.ofNullable(kind), limit).stream()
        .map(MarketplaceListingController::toResponse)
        .toList();
  }

  @GetMapping("/{listingId}")
  ListingResponse get(@PathVariable UUID listingId) {
    return listings
        .find(listingId)
        .map(MarketplaceListingController::toResponse)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
  }

  @org.springframework.web.bind.annotation.PatchMapping("/{listingId}/status")
  @MemberOnly
  ListingResponse changeStatus(
      @AuthenticationPrincipal UserDetails principal,
      @PathVariable UUID listingId,
      @Valid @RequestBody ChangeStatusRequest request) {
    try {
      return toResponse(
          listings.changeStatus(
              memberId(principal), listingId, request.status(), request.version()));
    } catch (MarketplaceListingNotFoundException exception) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    } catch (MarketplaceListingOwnershipException exception) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    } catch (MarketplaceListingStateException exception) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Invalid listing status transition");
    }
  }

  @org.springframework.web.bind.annotation.PatchMapping("/{listingId}")
  @MemberOnly
  ListingResponse update(
      @AuthenticationPrincipal UserDetails principal,
      @PathVariable UUID listingId,
      @Valid @RequestBody UpdateListingRequest request) {
    try {
      return toResponse(
          listings.update(
              memberId(principal),
              listingId,
              request.title(),
              request.description(),
              request.priceKrw(),
              request.version()));
    } catch (MarketplaceListingNotFoundException exception) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    } catch (MarketplaceListingOwnershipException exception) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    } catch (MarketplaceListingStateException exception) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Listing is not editable");
    }
  }

  private static UUID memberId(UserDetails principal) {
    try {
      return UUID.fromString(principal.getUsername());
    } catch (IllegalArgumentException exception) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid principal");
    }
  }

  private static ListingResponse toResponse(MarketplaceListingService.ListingView listing) {
    return new ListingResponse(
        listing.id(),
        listing.ownerMemberId(),
        listing.kind(),
        listing.status(),
        listing.title(),
        listing.description(),
        listing.priceKrw(),
        listing.createdAt(),
        listing.updatedAt(),
        listing.version());
  }

  record CreateListingRequest(
      @NotNull MarketplaceListingKind kind,
      @NotBlank @Size(max = 120) String title,
      @NotBlank @Size(max = 5000) String description,
      @Min(0) Long priceKrw) {}

  record ChangeStatusRequest(@NotNull MarketplaceListingStatus status, @Min(0) long version) {}

  record UpdateListingRequest(
      @NotBlank @Size(max = 120) String title,
      @NotBlank @Size(max = 5000) String description,
      @Min(0) Long priceKrw,
      @Min(0) long version) {}

  record ListingResponse(
      UUID id,
      UUID ownerMemberId,
      MarketplaceListingKind kind,
      MarketplaceListingStatus status,
      String title,
      String description,
      Long priceKrw,
      Instant createdAt,
      Instant updatedAt,
      long version) {}
}
