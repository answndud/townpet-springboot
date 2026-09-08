package com.townpet.discovery;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import org.flywaydb.core.Flyway;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.conf.ParamType;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

class CommunityFeedQueryPlanIntegrationTest {
  private static final int LIMIT = 20;
  private static PostgreSQLContainer<?> container;
  private static Target target;

  @BeforeAll
  static void setUp() throws SQLException {
    String externalUrl = System.getenv("TOWNPET_QUERY_PLAN_JDBC_URL");
    if (externalUrl != null && !externalUrl.isBlank()) {
      target =
          new Target(
              externalUrl,
              requiredEnv("TOWNPET_QUERY_PLAN_DB_USER"),
              requiredEnv("TOWNPET_QUERY_PLAN_DB_PASSWORD"));
      return;
    }
    container =
        new PostgreSQLContainer<>(
                DockerImageName.parse("postgis/postgis:18-3.6")
                    .asCompatibleSubstituteFor("postgres"))
            .withDatabaseName("townpet")
            .withUsername("townpet_app")
            .withPassword("townpet_performance");
    container.start();
    target = new Target(container.getJdbcUrl(), container.getUsername(), container.getPassword());
    try (Connection connection = target.open();
        Statement statement = connection.createStatement()) {
      statement.execute("CREATE EXTENSION IF NOT EXISTS postgis");
      statement.execute("CREATE EXTENSION IF NOT EXISTS citext");
    }
    Flyway.configure()
        .dataSource(target.url(), target.user(), target.password())
        .locations("classpath:db/migration")
        .cleanDisabled(true)
        .load()
        .migrate();
    seedFixture();
  }

  @AfterAll
  static void tearDown() {
    if (container != null) container.stop();
  }

  @Test
  void apiQueryAndNextCursorUseTheSameIntegratedViewQuery() throws Exception {
    DSLContext query;
    try (Connection connection = target.open()) {
      query = DSL.using(connection, SQLDialect.POSTGRES);
      var first =
          CommunityFeedQuery.build(
              query,
              new CommunityFeedQuery.Params(
                  null, null, null, "ALL", null, null, null, null, LIMIT));
      String firstSql = first.getSQL(ParamType.INDEXED);
      assertThat(firstSql).as(firstSql).containsIgnoringCase("townpet_public_feed_item");
      assertThat(firstSql).as(firstSql).containsIgnoringCase("order by");
      assertThat(firstSql).as(firstSql).containsIgnoringCase("fetch next");
      List<?> firstRows = first.fetch();
      assertThat(firstRows).hasSize(LIMIT + 1);

      var last = firstRows.get(LIMIT - 1);
      var record = (org.jooq.Record) last;
      var cursor =
          new CommunityFeedQuery.Cursor(
              record.get(CommunityFeedQuery.CREATED_AT).toInstant(),
              record.get(CommunityFeedQuery.ITEM_KIND),
              record.get(CommunityFeedQuery.SOURCE_ID));
      var next =
          CommunityFeedQuery.build(
              query,
              new CommunityFeedQuery.Params(
                  null, null, null, "ALL", null, null, null, cursor, LIMIT));
      assertThat(next.fetch()).hasSize(LIMIT + 1);

      writePlanArtifact(first, next);
    }
  }

  private static void seedFixture() throws SQLException {
    try (Connection connection = target.open();
        Statement statement = connection.createStatement()) {
      statement.execute(
          "INSERT INTO member_account (id, email, nickname) VALUES "
              + "('00000000-0000-4000-8000-000000000201', 'p3-feed@example.test', 'p3-feed') "
              + "ON CONFLICT (id) DO NOTHING");
      statement.execute(
          "INSERT INTO publication (id, author_member_id, type, title, body, lifecycle, created_at, updated_at, version) "
              + "SELECT md5('p3-publication-' || i)::uuid, "
              + "'00000000-0000-4000-8000-000000000201'::uuid, 'FREE_BOARD', "
              + "'p3-publication-' || i, 'p3 synthetic feed fixture ' || i, "
              + "CASE WHEN i % 29 = 0 THEN 'DELETED' ELSE 'ACTIVE' END, "
              + "CURRENT_TIMESTAMP - (i || ' seconds')::interval, "
              + "CURRENT_TIMESTAMP - (i || ' seconds')::interval, 0 "
              + "FROM generate_series(1, 100000) AS series(i)");
      statement.execute("ANALYZE publication");
      try (ResultSet result = statement.executeQuery("SELECT count(*) FROM publication WHERE title LIKE 'p3-publication-%'")) {
        assertThat(result.next()).isTrue();
        assertThat(result.getLong(1)).isEqualTo(100_000L);
      }
    }
  }

  private static void writePlanArtifact(org.jooq.Query first, org.jooq.Query next)
      throws SQLException, IOException {
    String artifactDirectory = System.getenv("TOWNPET_QUERY_PLAN_ARTIFACT");
    if (artifactDirectory == null || artifactDirectory.isBlank()) return;
    Path directory = Path.of(artifactDirectory);
    Files.createDirectories(directory);
    Files.writeString(
        directory.resolve("query.sql"),
        "-- first page\n"
            + first.getSQL(ParamType.INDEXED)
            + "\n-- next cursor\n"
            + next.getSQL(ParamType.INDEXED));
    Files.writeString(
        directory.resolve("binds.txt"), first.getBindValues() + "\n" + next.getBindValues());
    Files.writeString(
        directory.resolve("metadata.txt"),
        """
        endpoint=/api/v1/discovery?limit=20
        fixture_scale=100000_publications
        first_cursor=none
        next_cursor=last_row_of_first_page
        query_source=CommunityFeedQuery
        explain_status=recorded
        """);
    try (Connection connection = target.open()) {
      writeExplain(connection, first, directory.resolve("explain-first.json"));
      writeExplain(connection, next, directory.resolve("explain-next.json"));
    }
  }

  private static void writeExplain(Connection connection, org.jooq.Query query, Path output)
      throws SQLException, IOException {
    String sql = "EXPLAIN (ANALYZE, BUFFERS, FORMAT JSON) " + query.getSQL(ParamType.INDEXED);
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      bind(statement, query.getBindValues());
      try (ResultSet result = statement.executeQuery()) {
        assertThat(result.next()).isTrue();
        Files.writeString(output, result.getString(1) + "\n");
      }
    }
  }

  private static void bind(PreparedStatement statement, List<Object> values) throws SQLException {
    for (int index = 0; index < values.size(); index++)
      statement.setObject(index + 1, values.get(index));
  }

  private static String requiredEnv(String name) {
    String value = System.getenv(name);
    if (value == null || value.isBlank()) throw new IllegalStateException(name + " is required");
    return value;
  }

  private record Target(String url, String user, String password) {
    Connection open() throws SQLException {
      return DriverManager.getConnection(url, user, password);
    }
  }
}
