package com.townpet.publication.api;

import java.util.Optional;
import java.util.UUID;

/** Read-only publication visibility contract for interaction modules. */
public interface PublicationAccess {
  boolean existsActive(UUID publicationId);

  /** Finds an active publication, whether it was written by a member or a guest. */
  Optional<ActivePublication> findActive(UUID publicationId);
}
