package com.townpet.lostfound;

import com.townpet.common.UuidV7;
import java.time.Instant;
import java.sql.Timestamp;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class LostFoundAlertService {
  private final JdbcTemplate jdbc;

  LostFoundAlertService(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Transactional
  AlertView create(
      UUID reporterMemberId,
      LostFoundAlertKind kind,
      String title,
      String description,
      Instant lastSeenAt,
      double latitude,
      double longitude) {
    UUID id = UuidV7.randomUuid();
    jdbc.update(
        "INSERT INTO lost_found_alert "
            + "(id, reporter_member_id, kind, title, description, last_seen_at, approx_location) "
            + "VALUES (?, ?, ?, ?, ?, ?, ST_SetSRID(ST_MakePoint(?, ?), 4326)::geography)",
        id,
        reporterMemberId,
        kind.name(),
        title.trim(),
        description.trim(),
        Timestamp.from(lastSeenAt),
        longitude,
        latitude);
    return find(id).orElseThrow();
  }

  @Transactional(readOnly = true)
  Optional<AlertView> find(UUID id) {
    return jdbc.query(
            "SELECT id, reporter_member_id, kind, status, title, description, last_seen_at, "
                + "ST_Y(approx_location::geometry) AS latitude, "
                + "ST_X(approx_location::geometry) AS longitude, created_at, updated_at, version, "
                + "resolution_outcome, close_reason "
                + "FROM lost_found_alert WHERE id = ?",
            (rs, rowNum) ->
                new AlertView(
                    rs.getObject("id", UUID.class),
                    rs.getObject("reporter_member_id", UUID.class),
                    LostFoundAlertKind.valueOf(rs.getString("kind")),
                    LostFoundAlertStatus.valueOf(rs.getString("status")),
                    rs.getString("title"),
                    rs.getString("description"),
                    rs.getTimestamp("last_seen_at").toInstant(),
                    rs.getDouble("latitude"),
                    rs.getDouble("longitude"),
                    rs.getTimestamp("created_at").toInstant(),
                    rs.getTimestamp("updated_at").toInstant(),
                    rs.getLong("version"),
                    rs.getString("resolution_outcome"),
                    rs.getString("close_reason")),
            id)
        .stream()
        .findFirst();
  }

  @Transactional
  AlertView changeStatus(
      UUID ownerMemberId,
      UUID alertId,
      LostFoundAlertStatus nextStatus,
      String resolutionOutcome,
      String closeReason,
      String reopenReason) {
    AlertView current =
        find(alertId).orElseThrow(LostFoundAlertNotFoundException::new);
    if (!current.reporterMemberId().equals(ownerMemberId)) {
      throw new LostFoundAlertOwnershipException();
    }
    boolean reopening = nextStatus == LostFoundAlertStatus.ACTIVE;
    if ((!reopening && current.status() != LostFoundAlertStatus.ACTIVE)
        || (reopening && current.status() == LostFoundAlertStatus.ACTIVE)) {
      throw new LostFoundAlertStateException();
    }
    if (nextStatus == LostFoundAlertStatus.RESOLVED
        && (resolutionOutcome == null || resolutionOutcome.isBlank())) {
      throw new LostFoundAlertStateException();
    }
    if (nextStatus == LostFoundAlertStatus.CLOSED
        && (closeReason == null || closeReason.isBlank())) {
      throw new LostFoundAlertStateException();
    }
    if (reopening && (reopenReason == null || reopenReason.isBlank())) {
      throw new LostFoundAlertStateException();
    }
    int updated =
        jdbc.update(
            "UPDATE lost_found_alert SET status = ?, resolution_outcome = ?, close_reason = ?, "
                + "reopen_reason = ?, "
                + "updated_at = CURRENT_TIMESTAMP, version = version + 1 "
                + "WHERE id = ? AND reporter_member_id = ? AND version = ?",
            nextStatus.name(),
            blankToNull(resolutionOutcome),
            blankToNull(closeReason),
            blankToNull(reopenReason),
            alertId,
            ownerMemberId,
            current.version());
    if (updated != 1) throw new LostFoundAlertStateException();
    jdbc.update(
        "INSERT INTO lost_found_alert_status_history "
            + "(id, alert_id, actor_member_id, from_status, to_status, reason) "
            + "VALUES (?, ?, ?, ?, ?, ?)",
        UuidV7.randomUuid(),
        alertId,
        ownerMemberId,
        current.status().name(),
        nextStatus.name(),
        blankToNull(reopening ? reopenReason : (nextStatus == LostFoundAlertStatus.RESOLVED ? resolutionOutcome : closeReason)));
    return find(alertId).orElseThrow(LostFoundAlertNotFoundException::new);
  }

  @Nullable
  private static String blankToNull(@Nullable String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  record AlertView(
      UUID id,
      UUID reporterMemberId,
      LostFoundAlertKind kind,
      LostFoundAlertStatus status,
      String title,
      String description,
      Instant lastSeenAt,
      double latitude,
      double longitude,
      Instant createdAt,
      Instant updatedAt,
      long version,
      String resolutionOutcome,
      String closeReason) {}
}

final class LostFoundAlertNotFoundException extends RuntimeException {}

final class LostFoundAlertOwnershipException extends RuntimeException {}

final class LostFoundAlertStateException extends RuntimeException {}
