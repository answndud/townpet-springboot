package com.townpet.publication.api;

import java.util.UUID;

public interface GuestDirectory {
  GuestIdentity authenticate(UUID publicId, String password);

  GuestIdentity createCommentAuthor(String password);

  GuestIdentity authenticateByInternalId(UUID internalId, String password);

  record GuestIdentity(UUID internalId, UUID publicId) {}
}
