package com.townpet.catalog;

import com.townpet.catalog.api.AnimalCommunityTagger;
import com.townpet.catalog.api.AnimalInterestCatalog;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
class AnimalCommunityTaggerImpl implements AnimalCommunityTagger {
  private final JdbcTemplate jdbc;

  AnimalCommunityTaggerImpl(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  @Transactional
  public void replace(
      String contentKind, UUID contentId, @Nullable Collection<String> animalCodes) {
    requireValidIdentity(contentKind, contentId);
    Set<String> normalized =
        animalCodes == null
            ? Set.of()
            : animalCodes.stream()
                .filter(code -> code != null && !code.isBlank())
                .map(code -> code.trim().toUpperCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
    if (!AnimalInterestCatalog.codes().containsAll(normalized)) {
      throw new IllegalArgumentException("Invalid animal community code");
    }
    jdbc.update(
        "DELETE FROM content_animal_community WHERE content_kind = ? AND content_id = ?",
        contentKind,
        contentId);
    for (String code : normalized) {
      jdbc.update(
          "INSERT INTO content_animal_community (content_kind, content_id, animal_code) VALUES (?, ?, ?)",
          contentKind,
          contentId,
          code);
    }
  }

  @Override
  @Transactional(readOnly = true)
  public Map<UUID, List<String>> findAll(String contentKind, Collection<UUID> contentIds) {
    if (contentIds == null) throw new IllegalArgumentException("Content ids are required");
    Set<UUID> ids = Set.copyOf(contentIds);
    if (ids.isEmpty()) return Map.of();
    requireValidIdentity(contentKind, ids.iterator().next());
    String placeholders = String.join(",", java.util.Collections.nCopies(ids.size(), "?"));
    List<Object> arguments = new ArrayList<>(ids.size() + 1);
    arguments.add(contentKind);
    arguments.addAll(ids);
    Map<UUID, List<String>> mutable = new LinkedHashMap<>();
    ids.forEach(id -> mutable.put(id, new ArrayList<>()));
    jdbc.query(
        "SELECT c.content_id, c.animal_code FROM content_animal_community c "
            + "JOIN animal_interest_option o ON o.code = c.animal_code "
            + "WHERE c.content_kind = ? AND c.content_id IN ("
            + placeholders
            + ") ORDER BY c.content_id, o.sort_order, c.animal_code",
        (org.springframework.jdbc.core.RowCallbackHandler)
            resultSet ->
                mutable
                    .computeIfAbsent(
                        resultSet.getObject("content_id", UUID.class), ignored -> new ArrayList<>())
                    .add(resultSet.getString("animal_code")),
        arguments.toArray());
    return mutable.entrySet().stream()
        .collect(
            java.util.stream.Collectors.toUnmodifiableMap(
                Map.Entry::getKey, entry -> List.copyOf(entry.getValue())));
  }

  private static void requireValidIdentity(String contentKind, UUID contentId) {
    if (contentKind == null || !contentKind.matches("[A-Z0-9_]{1,40}")) {
      throw new IllegalArgumentException("Invalid content kind");
    }
    if (contentId == null) throw new IllegalArgumentException("Content id is required");
  }
}
