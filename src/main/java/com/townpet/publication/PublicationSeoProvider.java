package com.townpet.publication;

import com.townpet.common.PublicSeoProvider;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class PublicationSeoProvider implements PublicSeoProvider {
  private final PublicationService publications;

  PublicationSeoProvider(PublicationService publications) {
    this.publications = publications;
  }

  @Override
  public String route() {
    return "posts";
  }

  @Override
  public Optional<SeoPage> find(UUID id) {
    return publications
        .findVisible(id, null)
        .map(
            publication ->
                new SeoPage(
                    publication.getTitle(),
                    publication.getBody(),
                    publication.getBody(),
                    publication.getCreatedAt(),
                    publication.getUpdatedAt()));
  }
}
