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

  @Transactional(readOnly = true)
  ListingView findAny(UUID id) {
    return jdbc.query(
            "SELECT id, owner_member_id, kind, status, title, description, price_krw, "
                + "created_at, updated_at, version FROM market_listing WHERE id = ?",
            (rs, rowNum) -> map(rs),
            id)
        .stream()
        .findFirst()
        .orElseThrow(MarketplaceListingNotFoundException::new);
  }

  @Transactional
  ListingView changeStatus(
      UUID ownerMemberId, UUID listingId, MarketplaceListingStatus nextStatus, long version) {
    ListingView current = findAny(listingId);
    if (!current.ownerMemberId().equals(ownerMemberId)) {
      throw new MarketplaceListingOwnershipException();
    }
    if (!isAllowed(current.status(), nextStatus)) {
      throw new MarketplaceListingStateException();
    }
    int updated =
        jdbc.update(
            "UPDATE market_listing SET status = ?, updated_at = CURRENT_TIMESTAMP, version = version + 1 "
                + "WHERE id = ? AND owner_member_id = ? AND version = ?",
            nextStatus.name(),
            listingId,
            ownerMemberId,
            version);
    if (updated != 1) throw new MarketplaceListingStateException();
    jdbc.update(
        "INSERT INTO market_listing_status_history "
            + "(id, listing_id, actor_member_id, from_status, to_status) VALUES (?, ?, ?, ?, ?)",
        UuidV7.randomUuid(),
        listingId,
        ownerMemberId,
        current.status().name(),
        nextStatus.name());
    return findAny(listingId);
  }

  @Transactional
  ListingView update(
      UUID ownerMemberId,
      UUID listingId,
      String title,
      String description,
      Long priceKrw,
      long version) {
    ListingView current = findAny(listingId);
    if (!current.ownerMemberId().equals(ownerMemberId)) {
      throw new MarketplaceListingOwnershipException();
    }
    if (current.status() != MarketplaceListingStatus.AVAILABLE) {
      throw new MarketplaceListingStateException();
    }
    int updated =
        jdbc.update(
            "UPDATE market_listing SET title = ?, description = ?, price_krw = ?, "
                + "updated_at = CURRENT_TIMESTAMP, version = version + 1 "
                + "WHERE id = ? AND owner_member_id = ? AND status = 'AVAILABLE' AND version = ?",
            title.trim(),
            description.trim(),
            priceKrw,
            listingId,
            ownerMemberId,
            version);
    if (updated != 1) throw new MarketplaceListingStateException();
    return findAny(listingId);
  }

  private static boolean isAllowed(
      MarketplaceListingStatus current, MarketplaceListingStatus next) {
    return (current == MarketplaceListingStatus.AVAILABLE
            && (next == MarketplaceListingStatus.RESERVED
                || next == MarketplaceListingStatus.COMPLETED
                || next == MarketplaceListingStatus.CANCELLED))
        || (current == MarketplaceListingStatus.RESERVED
            && (next == MarketplaceListingStatus.AVAILABLE
                || next == MarketplaceListingStatus.COMPLETED
                || next == MarketplaceListingStatus.CANCELLED));
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

final class MarketplaceListingNotFoundException extends RuntimeException {}

final class MarketplaceListingOwnershipException extends RuntimeException {}

final class MarketplaceListingStateException extends RuntimeException {}
