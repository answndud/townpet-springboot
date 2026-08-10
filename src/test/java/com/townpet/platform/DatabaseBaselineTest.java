package com.townpet.platform;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
class DatabaseBaselineTest {

  @Container
  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>(
              DockerImageName.parse("postgis/postgis:18-3.6").asCompatibleSubstituteFor("postgres"))
          .withDatabaseName("townpet")
          .withUsername("townpet_app")
          .withPassword("townpet_local_dev");

  @Test
  void flywayCreatesPlatformBaseline() throws SQLException {
    provisionExtensions();

    Flyway.configure()
        .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
        .locations("classpath:db/migration")
        .cleanDisabled(true)
        .load()
        .migrate();

    try (Connection connection = POSTGRES.createConnection("");
        Statement statement = connection.createStatement()) {
      assertThat(extensionExists(statement, "postgis")).isTrue();
      assertThat(extensionExists(statement, "citext")).isTrue();
      assertThat(tableExists(statement, "spring_session")).isTrue();
      assertThat(tableExists(statement, "spring_session_attributes")).isTrue();
      assertThat(tableExists(statement, "event_publication")).isTrue();
      assertThat(tableExists(statement, "member_account")).isTrue();
      assertThat(tableExists(statement, "identity_credential")).isTrue();
      assertThat(columnExists(statement, "identity_credential", "role")).isTrue();
      assertThat(
              queryInt(
                  statement, "SELECT COUNT(*) FROM identity_credential WHERE role = 'MODERATOR'"))
          .isEqualTo(1);
      assertThat(tableExists(statement, "neighborhood")).isTrue();
      assertThat(queryInt(statement, "SELECT COUNT(*) FROM neighborhood"))
          .isGreaterThanOrEqualTo(2);
      assertThat(tableExists(statement, "flyway_schema_history")).isTrue();
    }
  }

  private static void provisionExtensions() throws SQLException {
    try (Connection connection = POSTGRES.createConnection("");
        Statement statement = connection.createStatement()) {
      statement.execute("CREATE EXTENSION IF NOT EXISTS postgis");
      statement.execute("CREATE EXTENSION IF NOT EXISTS citext");
    }
  }

  private static boolean extensionExists(Statement statement, String extension)
      throws SQLException {
    try (ResultSet resultSet =
        statement.executeQuery("SELECT 1 FROM pg_extension WHERE extname = '" + extension + "'")) {
      return resultSet.next();
    }
  }

  private static boolean tableExists(Statement statement, String table) throws SQLException {
    try (ResultSet resultSet =
        statement.executeQuery(
            "SELECT 1 FROM information_schema.tables "
                + "WHERE table_schema = 'public' AND table_name = '"
                + table
                + "'")) {
      return resultSet.next();
    }
  }

  private static boolean columnExists(Statement statement, String table, String column)
      throws SQLException {
    try (ResultSet resultSet =
        statement.executeQuery(
            "SELECT 1 FROM information_schema.columns WHERE table_schema = 'public'"
                + " AND table_name = '"
                + table
                + "' AND column_name = '"
                + column
                + "'")) {
      return resultSet.next();
    }
  }

  private static int queryInt(Statement statement, String sql) throws SQLException {
    try (ResultSet resultSet = statement.executeQuery(sql)) {
      resultSet.next();
      return resultSet.getInt(1);
    }
  }
}
