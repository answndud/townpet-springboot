package com.townpet.marketplace;

import com.townpet.common.PublicSeoProvider;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class MarketplaceSeoProvider implements PublicSeoProvider {
  private final MarketplaceListingService listings;

  MarketplaceSeoProvider(MarketplaceListingService listings) {
    this.listings = listings;
  }

  @Override
  public String route() {
    return "marketplace";
  }

  @Override
  public Optional<SeoPage> find(UUID id) {
    return listings
        .find(id)
        .map(
            listing ->
                new SeoPage(
                    listing.title(),
                    listing.description(),
                    listing.description(),
                    listing.createdAt(),
                    listing.updatedAt()));
  }
}
