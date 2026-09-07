package com.townpet.operations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.modulith.events.IncompleteEventPublications;
import org.springframework.modulith.events.ResubmissionOptions;

class EventPublicationRecoveryTest {
  @Test
  void resubmitsOnlyBoundedOldNonFailedPublications() {
    IncompleteEventPublications publications = org.mockito.Mockito.mock(IncompleteEventPublications.class);
    JdbcTemplate jdbc = org.mockito.Mockito.mock(JdbcTemplate.class);
    SimpleMeterRegistry metrics = new SimpleMeterRegistry();
    when(jdbc.queryForObject(any(String.class), eq(Integer.class))).thenReturn(3);
    when(jdbc.queryForObject(any(String.class), eq(Long.class))).thenReturn(900L);
    EventPublicationRecovery recovery =
        new EventPublicationRecovery(
            publications, jdbc, metrics, Duration.ofMinutes(5), 4, 2);

    recovery.recover();

    var options =
        org.mockito.ArgumentCaptor.forClass(ResubmissionOptions.class);
    verify(publications).resubmitIncompletePublications(options.capture());
    assertEquals(Duration.ofMinutes(5), options.getValue().getMinAge());
    assertEquals(4, options.getValue().getMaxInFlight());
    assertEquals(2, options.getValue().getBatchSize());
    assertEquals(3, metrics.get("townpet.events.backlog").gauge().value(), 0.0);
  }
}
