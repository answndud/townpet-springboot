package com.townpet.gathering;

import com.townpet.common.PublicSeoProvider;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class GatheringSeoProvider implements PublicSeoProvider {
  private final GatheringService gatherings;

  GatheringSeoProvider(GatheringService gatherings) {
    this.gatherings = gatherings;
  }

  @Override
  public String route() {
    return "gatherings";
  }

  @Override
  public Optional<SeoPage> find(UUID id) {
    try {
      GatheringService.GatheringView gathering = gatherings.get(id, null);
      return gathering.status() == GatheringStatus.ACTIVE
          ? Optional.of(
              new SeoPage(
                  gathering.title(),
                  gathering.description(),
                  gathering.description(),
                  null,
                  null))
          : Optional.empty();
    } catch (org.springframework.web.server.ResponseStatusException exception) {
      return Optional.empty();
    }
  }
}
