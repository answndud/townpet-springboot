package com.townpet.relationship;

import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
class RelationshipMutationLock {
  private final JdbcTemplate jdbc;

  RelationshipMutationLock(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  void acquire(UUID viewerId, UUID targetId) {
    jdbc.queryForObject(
        "SELECT pg_advisory_xact_lock(?)",
        (resultSet, rowNumber) -> resultSet.getObject(1),
        viewerId.getMostSignificantBits() ^ targetId.getLeastSignificantBits());
  }
}
