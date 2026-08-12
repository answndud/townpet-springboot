package com.townpet.marketplace;

import com.townpet.common.MemberOnly;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/lounges/breeds/{breedCode}/groupbuys")
class GroupBuyCompatibilityController {
  private final MarketplaceListingService listings;

  GroupBuyCompatibilityController(MarketplaceListingService listings) {
    this.listings = listings;
  }

  @GetMapping
  List<MarketplaceListingController.ListingResponse> list() {
    return listings
        .listAvailable(java.util.Optional.of(MarketplaceListingKind.GROUP_BUY), 50)
        .stream()
        .map(GroupBuyCompatibilityController::response)
        .toList();
  }

  @PostMapping
  @MemberOnly
  @ResponseStatus(HttpStatus.CREATED)
  MarketplaceListingController.ListingResponse create(
      @AuthenticationPrincipal UserDetails principal, @Valid @RequestBody Request request) {
    try {
      return response(
          listings.create(
              memberId(principal),
              MarketplaceListingKind.GROUP_BUY,
              request.title(),
              request.description(),
              request.priceKrw()));
    } catch (org.springframework.dao.DataIntegrityViolationException exception) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid group buy price policy");
    }
  }

  private static MarketplaceListingController.ListingResponse response(
      MarketplaceListingService.ListingView listing) {
    return new MarketplaceListingController.ListingResponse(
        listing.id(),
        listing.ownerMemberId(),
        listing.kind(),
        listing.status(),
        listing.title(),
        listing.description(),
        listing.priceKrw(),
        java.util.List.of(),
        listing.createdAt(),
        listing.updatedAt(),
        listing.version());
  }

  private static UUID memberId(UserDetails principal) {
    try {
      return UUID.fromString(principal.getUsername());
    } catch (IllegalArgumentException exception) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
    }
  }

  record Request(
      @NotBlank @Size(max = 120) String title,
      @NotBlank @Size(max = 5000) String description,
      @NotNull @Min(0) Long priceKrw) {}
}
