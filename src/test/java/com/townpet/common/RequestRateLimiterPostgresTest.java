package com.townpet.common;

import static org.junit.jupiter.api.Assertions.assertThrows;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.web.server.ResponseStatusException;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
class RequestRateLimiterPostgresTest {
  @Container
  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>(DockerImageName.parse("postgres:18"));

  @Test
  void twoLimiterInstancesShareOneAtomicWindow() {
    JdbcTemplate database =
        new JdbcTemplate(
            new DriverManagerDataSource(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()));
    database.execute(
        """
        CREATE TABLE security_rate_limit_window (
          bucket VARCHAR(80) NOT NULL,
          rate_key VARCHAR(255) NOT NULL,
          window_started_at TIMESTAMPTZ NOT NULL,
          request_count INTEGER NOT NULL,
          PRIMARY KEY (bucket, rate_key)
        )
        """);
    RequestRateLimiter first = new RequestRateLimiter(database, new SimpleMeterRegistry());
    RequestRateLimiter second = new RequestRateLimiter(database, new SimpleMeterRegistry());

    first.requireCapacity("shared", "same-key", 2, Duration.ofMinutes(1));
    second.requireCapacity("shared", "same-key", 2, Duration.ofMinutes(1));

    assertThrows(
        ResponseStatusException.class,
        () -> first.requireCapacity("shared", "same-key", 2, Duration.ofMinutes(1)));
  }
}
