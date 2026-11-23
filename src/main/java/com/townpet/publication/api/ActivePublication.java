package com.townpet.publication.api;

import java.util.UUID;
import org.springframework.lang.Nullable;

/**
 * An active publication as seen by interaction modules.
 *
 * @param authorMemberId the member author, or {@code null} when a guest wrote the publication
 */
public record ActivePublication(UUID id, @Nullable UUID authorMemberId) {}
