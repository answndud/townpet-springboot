package com.townpet.welfare;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

class VolunteerServiceClockTest {
  private static final Instant NOW = Instant.parse("2026-09-01T10:00:00Z");
  private static final UUID ID = UUID.fromString("00000000-0000-4000-8000-000000000701");
  private static final UUID APPLICANT = UUID.fromString("00000000-0000-4000-8000-000000000202");

  private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
  private final VolunteerService service =
      new VolunteerService(jdbc, Clock.fixed(NOW, ZoneOffset.UTC));

  @Test
  void rejectsApplicationsAtTheStartInstant() {
    stubOpportunityStartingAt(NOW);

    assertThatThrownBy(() -> service.apply(APPLICANT, ID, "참여할게요"))
        .isInstanceOf(VolunteerStateException.class)
        .hasMessage("Opportunity has already started");
  }

  @Test
  void letsApplicationsThroughOneSecondBeforeTheStart() {
    stubOpportunityStartingAt(NOW.plusSeconds(1));
    when(jdbc.queryForObject(anyString(), eq(Integer.class), eq(ID))).thenReturn(10);

    // The opportunity is full (capacity 10), which proves the start check was passed.
    assertThatThrownBy(() -> service.apply(APPLICANT, ID, "참여할게요"))
        .isInstanceOf(VolunteerStateException.class)
        .hasMessage("Opportunity is full");
  }

  @SuppressWarnings("unchecked")
  private void stubOpportunityStartingAt(Instant startsAt) {
    VolunteerService.Opportunity opportunity =
        new VolunteerService.Opportunity(
            ID,
            UUID.randomUUID(),
            "유기견 산책",
            "설명",
            "단체",
            "마포",
            startsAt,
            10,
            "OPEN",
            NOW.minusSeconds(3600),
            NOW.minusSeconds(3600),
            0L);
    when(jdbc.query(anyString(), any(RowMapper.class), eq(ID))).thenReturn(List.of(opportunity));
  }
}
