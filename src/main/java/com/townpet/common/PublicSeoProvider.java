package com.townpet.common;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.lang.Nullable;

/** Supplies only public, indexable content for the server-rendered SEO shell. */
public interface PublicSeoProvider {
  String route();

  Optional<SeoPage> find(UUID id);

  record SeoPage(
      String title,
      String description,
      String body,
      @Nullable Instant publishedAt,
      @Nullable Instant modifiedAt) {}
}
