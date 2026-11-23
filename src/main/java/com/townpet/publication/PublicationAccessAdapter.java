package com.townpet.publication;

import com.townpet.publication.api.ActivePublication;
import com.townpet.publication.api.PublicationAccess;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
class PublicationAccessAdapter implements PublicationAccess {
  private final PublicationRepository publications;

  PublicationAccessAdapter(PublicationRepository publications) {
    this.publications = publications;
  }

  @Override
  @Transactional(readOnly = true)
  public boolean existsActive(UUID publicationId) {
    return publications.existsByIdAndLifecycle(publicationId, PublicationLifecycle.ACTIVE);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<ActivePublication> findActive(UUID publicationId) {
    return publications
        .findByIdAndLifecycle(publicationId, PublicationLifecycle.ACTIVE)
        .map(
            publication ->
                new ActivePublication(publication.getId(), publication.getAuthorMemberId()));
  }
}
