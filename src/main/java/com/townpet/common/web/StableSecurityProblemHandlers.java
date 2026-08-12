package com.townpet.common.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/** Keeps filter-level authentication and authorization failures on the HTTP problem contract. */
@Component
public class StableSecurityProblemHandlers
    implements AuthenticationEntryPoint, AccessDeniedHandler {
  private static final String TRACE_HEADER = "X-Trace-Id";
  private final ObjectMapper objectMapper;

  public StableSecurityProblemHandlers(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public void commence(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException authenticationException)
      throws IOException {
    write(response, HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED");
  }

  @Override
  public void handle(
      HttpServletRequest request,
      HttpServletResponse response,
      org.springframework.security.access.AccessDeniedException accessDeniedException)
      throws IOException {
    write(response, HttpStatus.FORBIDDEN, "FORBIDDEN");
  }

  private void write(HttpServletResponse response, HttpStatus status, String code)
      throws IOException {
    if (response.isCommitted()) return;
    String traceId = response.getHeader(TRACE_HEADER);
    if (traceId == null || !traceId.matches("[A-Za-z0-9._:-]{1,128}")) {
      traceId = UUID.randomUUID().toString();
      response.setHeader(TRACE_HEADER, traceId);
    }
    ProblemDetail problem = ProblemDetail.forStatus(status);
    problem.setTitle(status.getReasonPhrase());
    problem.setDetail("The request could not be processed.");
    problem.setProperty("code", code);
    problem.setProperty("traceId", traceId);
    response.setStatus(status.value());
    response.setContentType("application/problem+json");
    objectMapper.writeValue(response.getWriter(), problem);
  }
}
