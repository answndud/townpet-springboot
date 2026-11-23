package com.townpet.support;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

/**
 * One PostGIS container shared by every Spring integration test in the test JVM.
 *
 * <p>Each Spring context gets its own database cloned from the template database, so tests keep
 * the isolation they had with one container per class (fresh Flyway migration, no leftover rows,
 * no background jobs of another context touching the data) without paying for a container start
 * per class. Test classes keep {@code @DirtiesContext(AFTER_CLASS)} so a finished context closes
 * its connection pool and scheduled jobs instead of staying cached.
 */
public final class PostgresTestContainer {
  private static final String TEMPLATE_DATABASE = "townpet";
  private static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>(
              DockerImageName.parse("postgis/postgis:18-3.6").asCompatibleSubstituteFor("postgres"))
          .withDatabaseName(TEMPLATE_DATABASE)
          .withUsername("townpet_app")
          .withPassword("townpet_test")
          .withCopyFileToContainer(
              MountableFile.forClasspathResource("postgres-extensions.sql"),
              "/docker-entrypoint-initdb.d/001_extensions.sql");
  private static final AtomicInteger NEXT_DATABASE = new AtomicInteger();

  static {
    // Stopped by Testcontainers' resource reaper when the test JVM exits.
    POSTGRES.start();
  }

  private PostgresTestContainer() {}

  /** Creates a fresh database for the calling context and points the datasource at it. */
  public static void register(DynamicPropertyRegistry registry) {
    String database = "townpet_test_" + NEXT_DATABASE.incrementAndGet();
    createDatabase(database);
    registry.add("spring.datasource.url", () -> jdbcUrl(database));
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
  }

  private static void createDatabase(String database) {
    // The template database must have no open connections, so administer through "postgres".
    try (Connection connection =
            DriverManager.getConnection(
                jdbcUrl("postgres"), POSTGRES.getUsername(), POSTGRES.getPassword());
        Statement statement = connection.createStatement()) {
      statement.execute("CREATE DATABASE \"" + database + "\" TEMPLATE " + TEMPLATE_DATABASE);
    } catch (SQLException exception) {
      throw new IllegalStateException("Could not create test database " + database, exception);
    }
  }

  private static String jdbcUrl(String database) {
    return "jdbc:postgresql://"
        + POSTGRES.getHost()
        + ":"
        + POSTGRES.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT)
        + "/"
        + database;
  }
}
