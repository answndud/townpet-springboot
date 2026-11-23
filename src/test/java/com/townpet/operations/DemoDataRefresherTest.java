package com.townpet.operations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.townpet.support.PostgresTestContainer;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(properties = "townpet.demo-data.refresh.enabled=true")
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Tag("integration")
class DemoDataRefresherTest {
  private static final UUID HOST = UUID.fromString("00000000-0000-4000-8000-000000000201");
  // Fixture ids the refresher owns: a gathering (6 days ahead) and a care request (7 days ahead).
  private static final UUID FIXTURE_GATHERING =
      UUID.fromString("00000000-0000-4000-8000-000000000401");
  private static final UUID FIXTURE_CARE_REQUEST =
      UUID.fromString("00000000-0000-4000-8000-000000009601");

  @DynamicPropertySource
  static void databaseProperties(DynamicPropertyRegistry registry) {
    PostgresTestContainer.register(registry);
  }

  @Autowired DemoDataRefresher refresher;
  @Autowired JdbcTemplate jdbc;

  @BeforeEach
  void clean() {
    jdbc.update("DELETE FROM gathering");
    jdbc.update("DELETE FROM care_request");
  }

  @Test
  void movesPastFixtureActivitiesBackToTheirOffsetsAndLeavesOtherRowsAlone() {
    Instant yesterday = Instant.now().minus(1, ChronoUnit.DAYS);
    insertGathering(FIXTURE_GATHERING, yesterday);
    UUID unrelated = UUID.randomUUID();
    insertGathering(unrelated, yesterday);

    int moved = refresher.refresh();

    assertThat(moved).isEqualTo(1);
    assertThat(startsAt("gathering", FIXTURE_GATHERING))
        .isCloseTo(Instant.now().plus(Duration.ofDays(6)), within(1, ChronoUnit.MINUTES));
    assertThat(startsAt("gathering", unrelated)).isCloseTo(yesterday, within(1, ChronoUnit.SECONDS));
  }

  @Test
  void keepsActivitiesThatStillStartMoreThanADayAway() {
    Instant threeDays = Instant.now().plus(3, ChronoUnit.DAYS);
    insertCareRequest(FIXTURE_CARE_REQUEST, threeDays);

    assertThat(refresher.refresh()).isZero();
    assertThat(startsAt("care_request", FIXTURE_CARE_REQUEST))
        .isCloseTo(threeDays, within(1, ChronoUnit.SECONDS));
  }

  @Test
  void movesACareRequestWithItsEndTimeAndIsIdempotent() {
    insertCareRequest(FIXTURE_CARE_REQUEST, Instant.now().minus(2, ChronoUnit.HOURS));

    assertThat(refresher.refresh()).isEqualTo(1);
    assertThat(startsAt("care_request", FIXTURE_CARE_REQUEST))
        .isCloseTo(Instant.now().plus(Duration.ofDays(7)), within(1, ChronoUnit.MINUTES));
    Instant endsAt =
        jdbc.queryForObject(
                "SELECT ends_at FROM care_request WHERE id = ?", Timestamp.class, FIXTURE_CARE_REQUEST)
            .toInstant();
    assertThat(endsAt).isCloseTo(Instant.now().plus(Duration.ofDays(9)), within(1, ChronoUnit.MINUTES));

    assertThat(refresher.refresh()).isZero();
  }

  private Instant startsAt(String table, UUID id) {
    return jdbc.queryForObject("SELECT starts_at FROM " + table + " WHERE id = ?", Timestamp.class, id)
        .toInstant();
  }

  private void insertGathering(UUID id, Instant startsAt) {
    jdbc.update(
        "INSERT INTO gathering(id, host_member_id, title, description, location, starts_at, capacity)"
            + " VALUES (?, ?, '모임', '설명', '마포', ?, 5)",
        id,
        HOST,
        Timestamp.from(startsAt));
  }

  private void insertCareRequest(UUID id, Instant startsAt) {
    jdbc.update(
        "INSERT INTO care_request(id, requester_member_id, title, description, location, starts_at,"
            + " ends_at) VALUES (?, ?, '돌봄', '설명', '마포', ?, ?)",
        id,
        HOST,
        Timestamp.from(startsAt),
        Timestamp.from(startsAt.plus(1, ChronoUnit.HOURS)));
  }
}
