package com.townpet.lostfound;

import com.townpet.common.UuidV7;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class LostFoundSightingService {
  private final JdbcTemplate jdbc;
  private final LostFoundAlertService alerts;

  LostFoundSightingService(JdbcTemplate jdbc, LostFoundAlertService alerts) {
    this.jdbc = jdbc;
    this.alerts = alerts;
  }

  @Transactional
  SightingView create(
      UUID reporterMemberId,
      UUID alertId,
      Instant seenAt,
      String description,
      double latitude,
      double longitude,
      Double exactLatitude,
      Double exactLongitude) {
    LostFoundAlertService.AlertView alert =
        alerts.find(alertId).orElseThrow(LostFoundAlertNotFoundException::new);
    if (alert.status() != LostFoundAlertStatus.ACTIVE) {
      throw new LostFoundAlertStateException();
    }
    UUID id = UuidV7.randomUuid();
    jdbc.update(
        "INSERT INTO lost_found_sighting_report "
            + "(id, alert_id, reporter_member_id, seen_at, description, approx_location, "
            + "exact_location, visibility) "
            + "VALUES (?, ?, ?, ?, ?, ST_SetSRID(ST_MakePoint(?, ?), 4326)::geography, "
            + "CASE WHEN ? IS NULL OR ? IS NULL THEN NULL "
            + "ELSE ST_SetSRID(ST_MakePoint(?, ?), 4326)::geography END, "
            + "CASE WHEN ? IS NULL OR ? IS NULL THEN 'PUBLIC_APPROXIMATE' "
            + "ELSE 'OWNER_ONLY_EXACT' END)",
        id,
        alertId,
        reporterMemberId,
        Timestamp.from(seenAt),
        description.trim(),
        longitude,
        latitude,
        exactLongitude,
        exactLatitude,
        exactLongitude,
        exactLatitude,
        exactLongitude,
        exactLatitude);
    return find(id).orElseThrow();
  }

  @Transactional(readOnly = true)
  Optional<SightingView> find(UUID id) {
    return jdbc.query(
            "SELECT id, alert_id, reporter_member_id, seen_at, description, "
                + "ST_Y(approx_location::geometry) AS latitude, "
                + "ST_X(approx_location::geometry) AS longitude, created_at "
                + "FROM lost_found_sighting_report WHERE id = ?",
            (rs, rowNum) ->
                new SightingView(
                    rs.getObject("id", UUID.class),
                    rs.getObject("alert_id", UUID.class),
                    rs.getObject("reporter_member_id", UUID.class),
                    rs.getTimestamp("seen_at").toInstant(),
                    rs.getString("description"),
                    rs.getDouble("latitude"),
                    rs.getDouble("longitude"),
                    rs.getTimestamp("created_at").toInstant()),
            id)
        .stream()
        .findFirst();
  }

  record SightingView(
      UUID id,
      UUID alertId,
      UUID reporterMemberId,
      Instant seenAt,
      String description,
      double latitude,
      double longitude,
      Instant createdAt) {}
}
