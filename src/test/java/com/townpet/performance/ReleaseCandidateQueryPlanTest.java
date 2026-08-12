package com.townpet.performance;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
class ReleaseCandidateQueryPlanTest {
  private static final UUID MEMBER_ID = UUID.fromString("00000000-0000-4000-8000-000000000201");
  private static final UUID PUBLICATION_ID = UUID.randomUUID();

  @Container
  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>(
              DockerImageName.parse("postgis/postgis:18-3.6").asCompatibleSubstituteFor("postgres"))
          .withDatabaseName("townpet")
          .withUsername("townpet_app")
          .withPassword("townpet_performance");

  @BeforeAll
  static void migrateAndSeed() throws SQLException {
    try (Connection connection = POSTGRES.createConnection("");
        Statement statement = connection.createStatement()) {
      statement.execute("CREATE EXTENSION IF NOT EXISTS postgis");
      statement.execute("CREATE EXTENSION IF NOT EXISTS citext");
    }
    Flyway.configure()
        .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
        .locations("classpath:db/migration")
        .cleanDisabled(true)
        .load()
        .migrate();
    try (Connection connection = POSTGRES.createConnection("");
        PreparedStatement publication =
            connection.prepareStatement(
                "INSERT INTO publication "
                    + "(id, author_member_id, type, scope, title, body, lifecycle, created_at, updated_at) "
                    + "VALUES (?, ?, 'FREE_BOARD', 'GLOBAL', 'performance', 'fixture', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)");
        PreparedStatement volunteer =
            connection.prepareStatement(
                "INSERT INTO volunteer_opportunity "
                    + "(id, publisher_member_id, title, description, organization, location, "
                    + "starts_at, capacity, status) VALUES (?, ?, ?, ?, ?, ?, ?, 10, ?)");
        PreparedStatement report =
            connection.prepareStatement(
                "INSERT INTO trust_report "
                    + "(id, reporter_member_id, target_type, target_id, reason, detail) "
                    + "VALUES (?, ?, 'PUBLICATION', ?, 'SPAM', 'performance fixture')")) {
      publication.setObject(1, PUBLICATION_ID);
      publication.setObject(2, MEMBER_ID);
      publication.executeUpdate();
      for (int i = 0; i < 2_000; i++) {
        UUID volunteerId = UUID.randomUUID();
        volunteer.setObject(1, volunteerId);
        volunteer.setObject(2, MEMBER_ID);
        volunteer.setString(3, "opportunity-" + i);
        volunteer.setString(4, "performance fixture");
        volunteer.setString(5, "TownPet");
        volunteer.setString(6, "Seoul");
        volunteer.setObject(7, java.sql.Timestamp.from(Instant.now().plusSeconds(i)));
        volunteer.setString(8, i % 3 == 0 ? "CLOSED" : "OPEN");
        volunteer.addBatch();

        report.setObject(1, UUID.randomUUID());
        report.setObject(2, MEMBER_ID);
        report.setObject(3, UUID.randomUUID());
        report.addBatch();
      }
      volunteer.executeBatch();
      report.executeBatch();
    }
    try (Connection connection = POSTGRES.createConnection("");
        Statement statement = connection.createStatement()) {
      statement.execute("ANALYZE volunteer_opportunity");
      statement.execute("ANALYZE trust_report");
    }
  }

  @Test
  void representativeQueuesUseStableReleaseCandidateIndexes() throws SQLException {
    try (Connection connection = POSTGRES.createConnection("");
        Statement statement = connection.createStatement()) {
      statement.execute("SET enable_seqscan = off");
      assertThat(
              explain(
                  statement,
                  "SELECT id FROM volunteer_opportunity WHERE status = 'OPEN' "
                      + "ORDER BY starts_at ASC, id ASC LIMIT 100"))
          .contains("volunteer_opportunity_public_ix");
      assertThat(
              explain(
                  statement,
                  "SELECT id FROM trust_report WHERE status = 'OPEN' "
                      + "ORDER BY created_at ASC, id ASC LIMIT 100"))
          .contains("trust_report_queue_stable_ix");
    }
  }

  @Test
  void publicationViewUpsertRemainsAtomicUnderConcurrentWrites() throws Exception {
    ExecutorService executor = Executors.newFixedThreadPool(8);
    try {
      List<Future<?>> writes = new ArrayList<>();
      for (int i = 0; i < 160; i++) {
        writes.add(
            executor.submit(
                () -> {
                  try (Connection connection = POSTGRES.createConnection("");
                      PreparedStatement statement =
                          connection.prepareStatement(
                              "INSERT INTO publication_metric (publication_id, view_count) "
                                  + "VALUES (?, 1) ON CONFLICT (publication_id) DO UPDATE SET "
                                  + "view_count = publication_metric.view_count + 1")) {
                    statement.setObject(1, PUBLICATION_ID);
                    statement.executeUpdate();
                  } catch (SQLException exception) {
                    throw new IllegalStateException(exception);
                  }
                }));
      }
      for (Future<?> write : writes) write.get();
    } finally {
      executor.shutdownNow();
    }
    try (Connection connection = POSTGRES.createConnection("");
        PreparedStatement statement =
            connection.prepareStatement(
                "SELECT view_count FROM publication_metric WHERE publication_id = ?")) {
      statement.setObject(1, PUBLICATION_ID);
      try (var result = statement.executeQuery()) {
        assertThat(result.next()).isTrue();
        assertThat(result.getLong(1)).isEqualTo(160);
      }
    }
  }

  private static String explain(Statement statement, String query) throws SQLException {
    try (var rows = statement.executeQuery("EXPLAIN (ANALYZE, BUFFERS) " + query)) {
      StringBuilder plan = new StringBuilder();
      while (rows.next()) plan.append(rows.getString(1)).append('\n');
      return plan.toString();
    }
  }
}
