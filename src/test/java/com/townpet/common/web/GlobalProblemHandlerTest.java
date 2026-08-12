package com.townpet.common.web;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.http.HttpServletResponse;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;

class GlobalProblemHandlerTest {
  private final GlobalProblemHandler handler = new GlobalProblemHandler();

  @Test
  void statusExceptionUsesStableCodeAndSanitizedTraceId() {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/example");
    request.addHeader("X-Trace-Id", "trace:demo-123");
    MockHttpServletResponse response = new MockHttpServletResponse();

    ProblemDetail problem =
        handler.handleStatus(
            new ResponseStatusException(HttpStatus.CONFLICT, "database detail"), request, response);

    assertThat(problem.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
    assertThat(problem.getProperties()).containsEntry("code", "STATE_CONFLICT");
    assertThat(problem.getProperties()).containsEntry("traceId", "trace:demo-123");
    assertThat(problem.getDetail()).doesNotContain("database detail");
    assertThat(response.getHeader("X-Trace-Id")).isEqualTo("trace:demo-123");
  }

  @Test
  void accessDeniedIsForbiddenAndInvalidTraceHeaderIsReplaced() {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/example");
    request.addHeader("X-Trace-Id", "bad\ntrace");
    MockHttpServletResponse response = new MockHttpServletResponse();

    ProblemDetail problem =
        handler.handleAccessDenied(new AccessDeniedException("internal reason"), request, response);

    assertThat(problem.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
    assertThat(problem.getProperties()).containsEntry("code", "FORBIDDEN");
    String traceId = (String) Objects.requireNonNull(problem.getProperties()).get("traceId");
    assertThat(traceId).matches("[0-9a-f-]{36}");
    assertThat(response.getHeader("X-Trace-Id")).isEqualTo(traceId);
    assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
  }
}
