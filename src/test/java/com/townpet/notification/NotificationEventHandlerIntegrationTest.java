package com.townpet.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.townpet.notification.api.NotificationEvent;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class NotificationEventHandlerIntegrationTest {
  private static final UUID RECIPIENT = UUID.fromString("00000000-0000-4000-8300-000000000001");
  private static final UUID ACTOR = UUID.fromString("00000000-0000-4000-8300-000000000002");

  @Container
  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>(
              DockerImageName.parse("postgis/postgis:18-3.6")
                  .asCompatibleSubstituteFor("postgres"))
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

  @Autowired ApplicationEventPublisher events;
  @Autowired org.springframework.transaction.PlatformTransactionManager transactionManager;
  @Autowired JdbcTemplate jdbc;

  @BeforeEach
  void seedMembers() {
    jdbc.update(
        "insert into member_account (id, email, nickname) values (?, ?, ?) on conflict (id) do nothing",
        RECIPIENT, "notification-recipient@townpet.local", "notification-recipient");
    jdbc.update(
        "insert into member_account (id, email, nickname) values (?, ?, ?) on conflict (id) do nothing",
        ACTOR, "notification-actor@townpet.local", "notification-actor");
  }

  @Test
  void replayedEventCreatesOneNotification() {
    UUID eventId = UUID.randomUUID();
    NotificationEvent event = event(eventId);

    publish(event);
    publish(event(eventId));

    assertEquals(1, awaitNotificationCount(eventId));
    assertEquals(2, awaitCompletedPublicationCount(eventId, 2));
  }

  @Test
  void concurrentlyReplayedEventCreatesOneNotification() throws Exception {
    UUID eventId = UUID.randomUUID();
    ExecutorService executor = Executors.newFixedThreadPool(8);
    CountDownLatch start = new CountDownLatch(1);
    try {
      var futures = new java.util.ArrayList<Future<Void>>();
      for (int i = 0; i < 8; i++) {
        futures.add(executor.submit(() -> {
          start.await();
          publish(event(eventId));
          return null;
        }));
      }
      start.countDown();
      for (Future<Void> future : futures) future.get();
      assertEquals(1, awaitNotificationCount(eventId));
      assertEquals(8, awaitCompletedPublicationCount(eventId, 8));
    } finally {
      executor.shutdownNow();
    }
  }

  @Test
  void publishedEventCompletesInEventPublicationRegistry() {
    UUID eventId = UUID.randomUUID();
    publish(event(eventId));

    assertEquals(1, awaitNotificationCount(eventId));
    assertEquals(1, awaitCompletedPublicationCount(eventId, 1));
  }

  private void publish(NotificationEvent event) {
    new TransactionTemplate(transactionManager)
        .executeWithoutResult(status -> events.publishEvent(event));
  }

  private NotificationEvent event(UUID eventId) {
    return new NotificationEvent(
        RECIPIENT, eventId, ACTOR, "REACTION", "새 반응", "게시글에 새 반응이 있습니다.");
  }

  private int notificationCount(UUID eventId) {
    return jdbc.queryForObject(
        "select count(*) from notification where event_id = ?", Integer.class, eventId);
  }

  private int awaitNotificationCount(UUID eventId) {
    for (int attempt = 0; attempt < 400; attempt++) {
      if (notificationCount(eventId) == 1) return 1;
      try {
        Thread.sleep(50);
      } catch (InterruptedException exception) {
        Thread.currentThread().interrupt();
        throw new AssertionError("Interrupted while waiting for notification consumer", exception);
      }
    }
    return notificationCount(eventId);
  }

  private int publicationCount(UUID eventId, String completionPredicate) {
    return jdbc.queryForObject(
        "select count(*) from event_publication "
            + "where event_type like '%NotificationEvent%' "
            + "and serialized_event like ? "
            + completionPredicate,
        Integer.class,
        "%" + eventId + "%");
  }

  private int awaitCompletedPublicationCount(UUID eventId, int expectedCount) {
    for (int attempt = 0; attempt < 400; attempt++) {
      if (publicationCount(eventId, "and completion_date is not null") >= expectedCount) {
        return expectedCount;
      }
      try {
        Thread.sleep(50);
      } catch (InterruptedException exception) {
        Thread.currentThread().interrupt();
        throw new AssertionError("Interrupted while waiting for event publication", exception);
      }
    }
    return publicationCount(eventId, "and completion_date is not null");
  }
}
