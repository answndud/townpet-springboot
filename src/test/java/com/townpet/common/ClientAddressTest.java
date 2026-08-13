package com.townpet.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

class ClientAddressTest {
  @Test
  void prefersTheFirstForwardedAddressWhenBehindCaddy() {
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.10, 172.18.0.2");
    when(request.getRemoteAddr()).thenReturn("172.18.0.2");

    assertEquals("203.0.113.10", ClientAddress.resolve(request));
  }

  @Test
  void fallsBackToSocketAddressWithoutForwardedHeader() {
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getRemoteAddr()).thenReturn("127.0.0.1");

    assertEquals("127.0.0.1", ClientAddress.resolve(request));
  }
}
