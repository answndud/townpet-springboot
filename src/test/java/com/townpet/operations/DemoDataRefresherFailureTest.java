package com.townpet.operations;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;

class DemoDataRefresherFailureTest {
  @Test
  void aFailingRefreshNeverStopsStartupOrTheScheduler() {
    JdbcTemplate jdbc = mock(JdbcTemplate.class);
    when(jdbc.update(anyString(), any(Object[].class)))
        .thenThrow(new BadSqlGrammarException("update", "UPDATE gathering", new java.sql.SQLException("no table")));
    DemoDataRefresher refresher = new DemoDataRefresher(jdbc, Clock.systemUTC());

    assertDoesNotThrow(refresher::refreshOnStartup);
    assertDoesNotThrow(refresher::refreshDaily);
  }
}
