package com.townpet.identity;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
final class StableSecurityProblemHandlers implements AuthenticationEntryPoint, AccessDeniedHandler {
  private static final String TRACE_HEADER = "X-Trace-Id";
  private static final Logger log = LoggerFactory.getLogger(StableSecurityProblemHandlers.class);
  private final ObjectMapper objectMapper;

  StableSecurityProblemHandlers(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public void commence(
      HttpServletRequest request, HttpServletResponse response, AuthenticationException failure)
      throws IOException {
    log.info(
        "event=security_rejected category=authentication_required method={} path={} failure_type={}",
        request.getMethod(),
        request.getRequestURI(),
        failure.getClass().getSimpleName());
    write(response, HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "Authentication is required.");
  }

  @Override
  public void handle(
      HttpServletRequest request,
      HttpServletResponse response,
      org.springframework.security.access.AccessDeniedException failure)
      throws IOException {
    log.info(
        "event=security_rejected category=access_denied method={} path={} failure_type={}",
        request.getMethod(),
        request.getRequestURI(),
        failure.getClass().getSimpleName());
    write(
        response,
        HttpStatus.FORBIDDEN,
        "FORBIDDEN",
        "You do not have permission to access this resource.");
  }

  private void write(HttpServletResponse response, HttpStatus status, String code, String detail)
      throws IOException {
    String traceId = response.getHeader(TRACE_HEADER);
    if (traceId == null || traceId.length() > 64 || !traceId.matches("[A-Za-z0-9._:-]+")) {
      traceId = UUID.randomUUID().toString();
      response.setHeader(TRACE_HEADER, traceId);
    }
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
    problem.setProperty("code", code);
    problem.setProperty("traceId", traceId);
    response.setStatus(status.value());
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    objectMapper.writeValue(response.getOutputStream(), problem);
  }
}
