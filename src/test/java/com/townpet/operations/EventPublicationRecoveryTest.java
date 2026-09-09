package com.townpet.operations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.modulith.events.EventPublication;
import org.springframework.modulith.events.IncompleteEventPublications;
import org.springframework.modulith.events.ResubmissionOptions;

class EventPublicationRecoveryTest {
  @Test
  void resubmitsOnlyBoundedOldNonFailedPublications() {
    IncompleteEventPublications publications =
        org.mockito.Mockito.mock(IncompleteEventPublications.class);
    JdbcTemplate jdbc = org.mockito.Mockito.mock(JdbcTemplate.class);
    SimpleMeterRegistry metrics = new SimpleMeterRegistry();
    when(jdbc.queryForObject(any(String.class), eq(Integer.class), any(Object[].class)))
        .thenReturn(3);
    when(jdbc.queryForObject(any(String.class), eq(Long.class), any(Object[].class)))
        .thenReturn(900L);
    EventPublicationRecovery recovery =
        new EventPublicationRecovery(publications, jdbc, metrics, Duration.ofMinutes(5), 4, 2, 3);

    recovery.recover();

    var options = org.mockito.ArgumentCaptor.forClass(ResubmissionOptions.class);
    verify(publications).resubmitIncompletePublications(options.capture());
    assertEquals(Duration.ofMinutes(5), options.getValue().getMinAge());
    assertEquals(4, options.getValue().getMaxInFlight());
    assertEquals(2, options.getValue().getBatchSize());
    assertEquals(3, metrics.get("townpet.events.backlog").gauge().value(), 0.0);

    EventPublication retryable = org.mockito.Mockito.mock(EventPublication.class);
    when(retryable.getCompletionAttempts()).thenReturn(2);
    assertTrue(options.getValue().getFilter().test(retryable));

    EventPublication exhausted = org.mockito.Mockito.mock(EventPublication.class);
    when(exhausted.getCompletionAttempts()).thenReturn(3);
    assertFalse(options.getValue().getFilter().test(exhausted));
  }

  @Test
  void skipsOverlappingRecoveryRun() throws Exception {
    IncompleteEventPublications publications =
        org.mockito.Mockito.mock(IncompleteEventPublications.class);
    JdbcTemplate jdbc = org.mockito.Mockito.mock(JdbcTemplate.class);
    SimpleMeterRegistry metrics = new SimpleMeterRegistry();
    CountDownLatch resubmissionStarted = new CountDownLatch(1);
    CountDownLatch releaseResubmission = new CountDownLatch(1);
    when(jdbc.queryForObject(any(String.class), eq(Integer.class), any(Object[].class)))
        .thenReturn(0);
    when(jdbc.queryForObject(any(String.class), eq(Long.class), any(Object[].class)))
        .thenReturn(0L);
    doAnswer(
            invocation -> {
              resubmissionStarted.countDown();
              try {
                assertTrue(releaseResubmission.await(2, TimeUnit.SECONDS));
              } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new AssertionError("Interrupted while releasing recovery", exception);
              }
              return null;
            })
        .when(publications)
        .resubmitIncompletePublications(any(ResubmissionOptions.class));
    EventPublicationRecovery recovery =
        new EventPublicationRecovery(publications, jdbc, metrics, Duration.ZERO, 2, 2, 3);

    Thread firstRun = new Thread(recovery::recover);
    firstRun.start();
    assertTrue(resubmissionStarted.await(2, TimeUnit.SECONDS));

    recovery.recover();

    releaseResubmission.countDown();
    firstRun.join(2_000);
    assertFalse(firstRun.isAlive());
    verify(publications).resubmitIncompletePublications(any(ResubmissionOptions.class));
  }
}
