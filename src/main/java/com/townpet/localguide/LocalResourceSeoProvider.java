package com.townpet.localguide;

import com.townpet.common.PublicSeoProvider;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class LocalResourceSeoProvider implements PublicSeoProvider {
  private final LocalResourceRepository resources;

  LocalResourceSeoProvider(LocalResourceRepository resources) {
    this.resources = resources;
  }

  @Override
  public String route() {
    return "guides";
  }

  @Override
  public Optional<SeoPage> find(UUID id) {
    return resources
        .findById(id)
        .map(
            resource ->
                new SeoPage(
                    resource.getTitle(),
                    resource.getSummary(),
                    resource.getContent(),
                    null,
                    resource.getUpdatedAt()));
  }
}
