package com.townpet.welfare;

import com.townpet.common.UuidV7;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdoptionService {
  private final JdbcTemplate jdbc;

  public AdoptionService(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Transactional
  public Listing create(
      UUID publisher,
      String title,
      String description,
      String species,
      @Nullable String breed,
      @Nullable UUID neighborhoodId) {
    UUID id = UuidV7.randomUuid();
    jdbc.update(
        "INSERT INTO adoption_listing (id, publisher_member_id, neighborhood_id, title, description, species, breed) VALUES (?, ?, ?, ?, ?, ?, ?)",
        id,
        publisher,
        neighborhoodId,
        title.trim(),
        description.trim(),
        species.trim(),
        breed == null ? null : breed.trim());
    return find(id).orElseThrow();
  }

  @Transactional(readOnly = true)
  public List<Listing> list(int limit) {
    return jdbc.query(
        "SELECT id, publisher_member_id, neighborhood_id, title, description, species, breed, status, created_at, updated_at, version FROM adoption_listing WHERE status IN ('OPEN', 'RESERVED') ORDER BY created_at DESC, id DESC LIMIT ?",
        (rs, row) -> map(rs),
        limit);
  }

  @Transactional(readOnly = true)
  public java.util.Optional<Listing> find(UUID id) {
    return jdbc
        .query(
            "SELECT id, publisher_member_id, neighborhood_id, title, description, species, breed, status, created_at, updated_at, version FROM adoption_listing WHERE id = ?",
            (rs, row) -> map(rs),
            id)
        .stream()
        .findFirst();
  }

  private static Listing map(ResultSet rs) throws SQLException {
    return new Listing(
        rs.getObject("id", UUID.class),
        rs.getObject("publisher_member_id", UUID.class),
        rs.getObject("neighborhood_id", UUID.class),
        rs.getString("title"),
        rs.getString("description"),
        rs.getString("species"),
        rs.getString("breed"),
        rs.getString("status"),
        rs.getTimestamp("created_at").toInstant(),
        rs.getTimestamp("updated_at").toInstant(),
        rs.getLong("version"));
  }

  public record Listing(
      UUID id,
      UUID publisherMemberId,
      UUID neighborhoodId,
      String title,
      String description,
      String species,
      String breed,
      String status,
      Instant createdAt,
      Instant updatedAt,
      long version) {}
}
