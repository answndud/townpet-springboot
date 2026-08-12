package com.townpet.welfare;

import com.townpet.common.UuidV7;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class VolunteerService {
  private static final int PUBLIC_LIMIT = 100;
  private final JdbcTemplate jdbc;

  VolunteerService(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Transactional(readOnly = true)
  List<Opportunity> list() {
    return jdbc.query(
        "SELECT id,publisher_member_id,title,description,organization,location,starts_at,capacity,status,created_at,updated_at,version FROM volunteer_opportunity WHERE status IN ('OPEN','FULL') ORDER BY starts_at,id LIMIT ?",
        (rs, n) -> map(rs),
        PUBLIC_LIMIT);
  }

  @Transactional(readOnly = true)
  Optional<Opportunity> find(UUID id) {
    return jdbc
        .query(
            "SELECT id,publisher_member_id,title,description,organization,location,starts_at,capacity,status,created_at,updated_at,version FROM volunteer_opportunity WHERE id=?",
            (rs, n) -> map(rs),
            id)
        .stream()
        .findFirst();
  }

  @Transactional
  Opportunity create(
      UUID publisher,
      String title,
      String description,
      String organization,
      String location,
      Instant startsAt,
      int capacity) {
    UUID id = UuidV7.randomUuid();
    jdbc.update(
        "INSERT INTO volunteer_opportunity(id,publisher_member_id,title,description,organization,location,starts_at,capacity) VALUES(?,?,?,?,?,?,?,?)",
        id,
        publisher,
        title.trim(),
        description.trim(),
        organization.trim(),
        location.trim(),
        startsAt,
        capacity);
    return find(id).orElseThrow();
  }

  @Transactional
  void apply(UUID applicant, UUID id, String message) {
    Opportunity opportunity =
        jdbc
            .query(
                "SELECT id,publisher_member_id,title,description,organization,location,starts_at,capacity,status,created_at,updated_at,version "
                    + "FROM volunteer_opportunity WHERE id = ? FOR UPDATE",
                (rs, row) -> map(rs),
                id)
            .stream()
            .findFirst()
            .orElseThrow(NoSuchElementException::new);
    if (!Set.of("OPEN", "FULL").contains(opportunity.status())) {
      throw new IllegalStateException("Opportunity is closed");
    }
    Integer applicationCountValue =
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM volunteer_application WHERE opportunity_id = ?",
            Integer.class,
            id);
    int applicationCount = Objects.requireNonNullElse(applicationCountValue, 0);
    if (applicationCount >= opportunity.capacity()) {
      jdbc.update(
          "UPDATE volunteer_opportunity SET status = 'FULL', updated_at = CURRENT_TIMESTAMP WHERE id = ?",
          id);
      throw new IllegalStateException("Opportunity is full");
    }
    try {
      jdbc.update(
          "INSERT INTO volunteer_application(id,opportunity_id,applicant_member_id,message) VALUES(?,?,?,?)",
          UuidV7.randomUuid(),
          id,
          applicant,
          message.trim());
      if (applicationCount + 1 >= opportunity.capacity()) {
        jdbc.update(
            "UPDATE volunteer_opportunity SET status = 'FULL', updated_at = CURRENT_TIMESTAMP WHERE id = ?",
            id);
      }
    } catch (org.springframework.dao.DataIntegrityViolationException e) {
      throw new IllegalStateException("Already applied");
    }
  }

  private static Opportunity map(ResultSet r) throws SQLException {
    return new Opportunity(
        r.getObject("id", UUID.class),
        r.getObject("publisher_member_id", UUID.class),
        r.getString("title"),
        r.getString("description"),
        r.getString("organization"),
        r.getString("location"),
        r.getTimestamp("starts_at").toInstant(),
        r.getInt("capacity"),
        r.getString("status"),
        r.getTimestamp("created_at").toInstant(),
        r.getTimestamp("updated_at").toInstant(),
        r.getLong("version"));
  }

  record Opportunity(
      UUID id,
      UUID publisherMemberId,
      String title,
      String description,
      String organization,
      String location,
      Instant startsAt,
      int capacity,
      String status,
      Instant createdAt,
      Instant updatedAt,
      long version) {}
}
