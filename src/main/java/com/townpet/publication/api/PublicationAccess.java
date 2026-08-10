package com.townpet.publication.api;

import java.util.Optional;
import java.util.UUID;

/** Read-only publication visibility contract for interaction modules. */
public interface PublicationAccess {
  boolean existsActive(UUID publicationId);

  Optional<UUID> activeAuthorMemberId(UUID publicationId);
}
