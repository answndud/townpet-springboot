package com.townpet.lostfound;

import com.townpet.common.UuidV7;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class LostFoundExactLocationService {
  private final JdbcTemplate jdbc;

  LostFoundExactLocationService(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Transactional
  Optional<ExactLocation> getForAlertOwner(UUID sightingId, UUID viewerMemberId) {
    return jdbc.query(
            "SELECT s.id, ST_Y(s.exact_location::geometry) AS latitude, "
                + "ST_X(s.exact_location::geometry) AS longitude "
                + "FROM lost_found_sighting_report s "
                + "JOIN lost_found_alert a ON a.id = s.alert_id "
                + "WHERE s.id = ? AND a.reporter_member_id = ? AND s.exact_location IS NOT NULL",
            (rs, rowNum) ->
                new ExactLocation(
                    rs.getObject("id", UUID.class),
                    rs.getDouble("latitude"),
                    rs.getDouble("longitude")),
            sightingId,
            viewerMemberId)
        .stream()
        .findFirst()
        .map(
            location -> {
              jdbc.update(
                  "INSERT INTO lost_found_location_access_audit "
                      + "(id, sighting_id, viewer_member_id) VALUES (?, ?, ?)",
                  UuidV7.randomUuid(),
                  sightingId,
                  viewerMemberId);
              return location;
            });
  }

  record ExactLocation(UUID sightingId, double latitude, double longitude) {}
}
