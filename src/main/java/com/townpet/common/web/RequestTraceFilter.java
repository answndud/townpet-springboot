package com.townpet.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Carries a bounded correlation id through logs and every HTTP response. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestTraceFilter extends OncePerRequestFilter {
  static final String HEADER = "X-Trace-Id";
  private static final String MDC_KEY = "traceId";
  private static final Logger log = LoggerFactory.getLogger(RequestTraceFilter.class);
  private final String buildVersion;

  public RequestTraceFilter(
      @Value("${townpet.build.version:unknown}") String buildVersion) {
    this.buildVersion = buildVersion;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String traceId = traceId(request.getHeader(HEADER));
    response.setHeader(HEADER, traceId);
    MDC.put(MDC_KEY, traceId);
    long startedAt = System.nanoTime();
    try {
      filterChain.doFilter(request, response);
    } finally {
      log.info(
          "event=http_request method={} path={} query_present={} status={} duration_ms={} build_version={} trace_id={}",
          request.getMethod(),
          request.getRequestURI(),
          request.getQueryString() != null,
          response.getStatus(),
          (System.nanoTime() - startedAt) / 1_000_000,
          buildVersion,
          traceId);
      MDC.remove(MDC_KEY);
    }
  }

  private static String traceId(String supplied) {
    return supplied != null && supplied.matches("[A-Za-z0-9._:-]{1,128}")
        ? supplied
        : UUID.randomUUID().toString();
  }
}
