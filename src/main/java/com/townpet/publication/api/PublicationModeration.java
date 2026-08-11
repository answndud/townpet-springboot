package com.townpet.publication.api;

import java.util.UUID;

public interface PublicationModeration {
  int setAuthorContentVisibility(UUID authorMemberId, boolean visible);
}
