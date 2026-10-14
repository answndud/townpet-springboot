package com.townpet.publication.api;

import java.util.UUID;
import java.util.Optional;

public interface GuestDirectory {
  GuestIdentity authenticate(UUID publicId, String password);

  Optional<UUID> internalId(UUID publicId);

  record GuestIdentity(UUID internalId, UUID publicId) {}
}
