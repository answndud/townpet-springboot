package com.townpet.operations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.townpet.notification.api.NotificationEvent;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

@SpringBootTest(
    properties = {
      "townpet.events.recovery.enabled=true",
      "townpet.events.recovery.min-age=PT0S",
      "townpet.events.recovery.max-attempts=3",
      "townpet.events.recovery.initial-delay-ms=3600000"
    })
@ActiveProfiles("test")
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class EventPublicationRecoveryIntegrationTest {
  private static final UUID ACTOR = UUID.fromString("00000000-0000-4000-8300-000000000002");

  @Container
  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>(
              DockerImageName.parse("postgis/postgis:18-3.6").asCompatibleSubstituteFor("postgres"))
          .withDatabaseName("townpet")
          .withUsername("townpet_app")
          .withPassword("townpet_test")
          .withCopyFileToContainer(
              MountableFile.forClasspathResource("postgres-extensions.sql"),
              "/docker-entrypoint-initdb.d/001_extensions.sql");

  @DynamicPropertySource
  static void databaseProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
  }

  @Autowired EventPublicationRecovery recovery;
  @Autowired ApplicationEventPublisher events;
  @Autowired org.springframework.transaction.PlatformTransactionManager transactionManager;
  @Autowired JdbcTemplate jdbc;

  @Test
  void failedPublicationCompletesAfterDependencyRecoversWithoutRestart() {
    UUID recipient = UUID.randomUUID();
    UUID eventId = UUID.randomUUID();
    insertMember(ACTOR, "recovery-actor@townpet.local", "recovery-actor");

    new TransactionTemplate(transactionManager)
        .executeWithoutResult(
            status ->
                events.publishEvent(
                    new NotificationEvent(
                        recipient,
                        eventId,
                        ACTOR,
                        "RECOVERY_TEST",
                        "재처리 테스트",
                        "의존성 복구 후 재처리됩니다.")));

    UUID publicationId = awaitLatestNotificationPublication();
    assertTrue(awaitPublicationStatus(publicationId, "FAILED"));

    insertMember(recipient, "recovery-recipient@townpet.local", "recovery-recipient");
    recovery.recover();

    assertTrue(awaitPublicationCompletion(publicationId));
    assertEquals(1, notificationCount(eventId));
    assertNotNull(
        jdbc.queryForObject(
            "select completion_date from event_publication where id = ?",
            Object.class,
            publicationId));
    int completionAttempts =
        Objects.requireNonNull(
            jdbc.queryForObject(
                "select completion_attempts from event_publication where id = ?",
                Integer.class,
                publicationId));
    assertTrue(completionAttempts >= 1);
  }

  private void insertMember(UUID id, String email, String nickname) {
    jdbc.update(
        "insert into member_account (id, email, nickname) values (?, ?, ?) on conflict (id) do nothing",
        id,
        email,
        nickname);
  }

  private UUID awaitLatestNotificationPublication() {
    for (int attempt = 0; attempt < 400; attempt++) {
      Optional<UUID> id =
          jdbc.query(
              "select id from event_publication where event_type like '%NotificationEvent%' order by publication_date desc limit 1",
              result ->
                  result.next() ? Optional.of(result.getObject(1, UUID.class)) : Optional.empty());
      if (id.isPresent()) return id.get();
      sleep();
    }
    throw new AssertionError("Notification event publication was not recorded");
  }

  private boolean awaitPublicationStatus(UUID publicationId, String expected) {
    for (int attempt = 0; attempt < 400; attempt++) {
      String status =
          jdbc.queryForObject(
              "select status from event_publication where id = ?", String.class, publicationId);
      if (expected.equals(status)) return true;
      sleep();
    }
    return expected.equals(
        jdbc.queryForObject(
            "select status from event_publication where id = ?", String.class, publicationId));
  }

  private boolean awaitPublicationCompletion(UUID publicationId) {
    for (int attempt = 0; attempt < 400; attempt++) {
      Boolean completed =
          jdbc.queryForObject(
              "select completion_date is not null from event_publication where id = ?",
              Boolean.class,
              publicationId);
      if (Boolean.TRUE.equals(completed)) return true;
      sleep();
    }
    return Boolean.TRUE.equals(
        jdbc.queryForObject(
            "select completion_date is not null from event_publication where id = ?",
            Boolean.class,
            publicationId));
  }

  private int notificationCount(UUID eventId) {
    return Objects.requireNonNull(
        jdbc.queryForObject(
            "select count(*) from notification where event_id = ?", Integer.class, eventId));
  }

  private void sleep() {
    try {
      Thread.sleep(50);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new AssertionError("Interrupted while waiting for event publication", exception);
    }
  }
}
