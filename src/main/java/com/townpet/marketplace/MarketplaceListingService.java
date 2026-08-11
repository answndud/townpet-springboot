package com.townpet.marketplace;

import com.townpet.common.UuidV7;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class MarketplaceListingService {
  private final JdbcTemplate jdbc;

  MarketplaceListingService(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Transactional
  ListingView create(
      UUID ownerMemberId,
      MarketplaceListingKind kind,
      String title,
      String description,
      Long priceKrw) {
    UUID id = UuidV7.randomUuid();
    jdbc.update(
        "INSERT INTO market_listing "
            + "(id, owner_member_id, kind, title, description, price_krw) "
            + "VALUES (?, ?, ?, ?, ?, ?)",
        id,
        ownerMemberId,
        kind.name(),
        title.trim(),
        description.trim(),
        priceKrw);
    return find(id).orElseThrow();
  }

  @Transactional(readOnly = true)
  Optional<ListingView> find(UUID id) {
    return jdbc.query(
            "SELECT id, owner_member_id, kind, status, title, description, price_krw, "
                + "created_at, updated_at, version FROM market_listing "
                + "WHERE id = ? AND status = 'AVAILABLE'",
            (rs, rowNum) -> map(rs),
            id)
        .stream()
        .findFirst();
  }

  private static ListingView map(ResultSet rs) throws SQLException {
    return new ListingView(
        rs.getObject("id", UUID.class),
        rs.getObject("owner_member_id", UUID.class),
        MarketplaceListingKind.valueOf(rs.getString("kind")),
        MarketplaceListingStatus.valueOf(rs.getString("status")),
        rs.getString("title"),
        rs.getString("description"),
        rs.getObject("price_krw", Long.class),
        rs.getTimestamp("created_at").toInstant(),
        rs.getTimestamp("updated_at").toInstant(),
        rs.getLong("version"));
  }

  record ListingView(
      UUID id,
      UUID ownerMemberId,
      MarketplaceListingKind kind,
      MarketplaceListingStatus status,
      String title,
      String description,
      Long priceKrw,
      Instant createdAt,
      Instant updatedAt,
      long version) {}
}
