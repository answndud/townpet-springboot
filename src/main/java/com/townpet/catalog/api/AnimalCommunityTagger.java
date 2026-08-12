package com.townpet.catalog.api;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.lang.Nullable;

/** Manages the catalog-owned content-to-animal index used by community feeds and edit forms. */
public interface AnimalCommunityTagger {
  void replace(String contentKind, UUID contentId, @Nullable Collection<String> animalCodes);

  default List<String> find(String contentKind, UUID contentId) {
    return findAll(contentKind, Set.of(contentId)).getOrDefault(contentId, List.of());
  }

  Map<UUID, List<String>> findAll(String contentKind, Collection<UUID> contentIds);

  default void replace(String contentKind, UUID contentId, String animalCode) {
    replace(contentKind, contentId, animalCode == null ? Set.of() : Set.of(animalCode));
  }
}
