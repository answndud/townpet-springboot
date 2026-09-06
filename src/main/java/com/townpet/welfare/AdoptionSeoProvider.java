package com.townpet.welfare;

import com.townpet.common.PublicSeoProvider;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class AdoptionSeoProvider implements PublicSeoProvider {
  private final AdoptionService adoptions;

  AdoptionSeoProvider(AdoptionService adoptions) {
    this.adoptions = adoptions;
  }

  @Override
  public String route() {
    return "adoptions";
  }

  @Override
  public Optional<SeoPage> find(UUID id) {
    return adoptions
        .find(id)
        .filter(item -> item.status().equals("OPEN") || item.status().equals("RESERVED"))
        .map(
            item ->
                new SeoPage(
                    item.title(),
                    item.description(),
                    item.description(),
                    item.createdAt(),
                    item.updatedAt()));
  }
}
