package com.townpet.care;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CareApplicationServiceClockTest {
  private static final Instant NOW = Instant.parse("2026-06-01T09:00:00Z");
  private static final UUID REQUEST_ID = UUID.fromString("00000000-0000-4000-8000-000000000501");
  private static final UUID REQUESTER = UUID.fromString("00000000-0000-4000-8000-000000000201");
  private static final UUID APPLICANT = UUID.fromString("00000000-0000-4000-8000-000000000202");

  private final CareRequestRepository requests = mock(CareRequestRepository.class);
  private final CareApplicationRepository applications = mock(CareApplicationRepository.class);
  private final CareApplicationService service =
      new CareApplicationService(requests, applications, Clock.fixed(NOW, ZoneOffset.UTC));

  @Test
  void rejectsApplicationsOnceTheRequestHasStarted() {
    when(requests.findByIdAndStatus(REQUEST_ID, CareRequestStatus.OPEN))
        .thenReturn(Optional.of(requestStartingAt(NOW)));

    assertThatThrownBy(() -> service.apply(APPLICANT, REQUEST_ID, "도와드릴게요"))
        .isInstanceOf(CareStateConflictException.class)
        .hasMessage("Request has expired");
  }

  @Test
  void acceptsApplicationsBeforeTheStartAndStampsThemWithTheClock() {
    when(requests.findByIdAndStatus(REQUEST_ID, CareRequestStatus.OPEN))
        .thenReturn(Optional.of(requestStartingAt(NOW.plusSeconds(1))));
    when(applications.findByRequestIdAndApplicantMemberId(REQUEST_ID, APPLICANT))
        .thenReturn(Optional.empty());
    when(applications.saveAndFlush(any(CareApplicationEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    CareApplicationEntity application = service.apply(APPLICANT, REQUEST_ID, "도와드릴게요");

    assertThat(application.getCreatedAt()).isEqualTo(NOW);
  }

  private static CareRequestEntity requestStartingAt(Instant startsAt) {
    return new CareRequestEntity(
        REQUESTER,
        "산책 도움",
        "설명",
        "마포",
        startsAt,
        startsAt.plusSeconds(3600),
        null,
        NOW.minusSeconds(60));
  }
}
