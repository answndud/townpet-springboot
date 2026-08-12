package com.townpet.common.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestTraceFilterTest {
  private final RequestTraceFilter filter = new RequestTraceFilter();

  @Test
  void preservesSafeTraceIdAndClearsMdcAfterRequest() throws ServletException, IOException {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/health");
    request.addHeader(RequestTraceFilter.HEADER, "portfolio:health-1");
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain chain = mock(FilterChain.class);

    filter.doFilter(request, response, chain);

    assertThat(response.getHeader(RequestTraceFilter.HEADER)).isEqualTo("portfolio:health-1");
    assertThat(org.slf4j.MDC.get("traceId")).isNull();
    verify(chain).doFilter(request, response);
  }

  @Test
  void replacesUnsafeTraceIdWithUuid() throws ServletException, IOException {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/health");
    request.addHeader(RequestTraceFilter.HEADER, "invalid trace");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, mock(FilterChain.class));

    assertThat(response.getHeader(RequestTraceFilter.HEADER)).matches("[0-9a-f-]{36}");
  }
}
