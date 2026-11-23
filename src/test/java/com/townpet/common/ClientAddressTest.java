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

    assertEquals("203.0.113.10", new ClientAddress("172.18.0.0/16").resolve(request));
  }

  @Test
  void fallsBackToSocketAddressWithoutForwardedHeader() {
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getRemoteAddr()).thenReturn("127.0.0.1");

    assertEquals("127.0.0.1", new ClientAddress("172.18.0.0/16").resolve(request));
  }

  @Test
  void ignoresClientSuppliedAddressesToTheLeftOfTheTrustedProxy() {
    // The client sent "1.1.1.1"; the proxy appended the address it actually saw.
    assertEquals(
        "203.0.113.7", resolve("172.18.0.2", "1.1.1.1, 203.0.113.7", "172.18.0.0/16"));
    assertEquals(
        "203.0.113.7", resolve("172.18.0.2", "1.1.1.1, 203.0.113.7, 172.18.0.3", "172.18.0.0/16"));
  }

  @Test
  void skipsEveryTrustedHopBeforeChoosingTheClient() {
    assertEquals(
        "198.51.100.9",
        resolve("172.18.0.2", "198.51.100.9, 172.18.0.5, 172.18.0.4", "172.18.0.0/16"));
  }

  @Test
  void usesTheLeftmostAddressWhenEveryHopIsTrusted() {
    assertEquals("172.18.0.9", resolve("172.18.0.2", "172.18.0.9, 172.18.0.3", "172.18.0.0/16"));
  }

  @Test
  void handlesIpv6ClientsAndNormalizesTheirAddress() {
    assertEquals(
        "2001:db8:0:0:0:0:0:1",
        resolve("172.18.0.2", "2001:db8::1", "172.18.0.0/16"));
    assertEquals(
        "2001:db8:0:0:0:0:0:2",
        resolve("fd00::2", "2001:db8::2, fd00::3", "fd00::/8"));
  }

  @Test
  void neverResolvesHostNamesFromTheForwardedHeader() {
    // A host name is not an IP literal; it must be rejected without a DNS lookup.
    assertEquals("172.18.0.2", resolve("172.18.0.2", "localhost", "172.18.0.0/16"));
    assertEquals("172.18.0.2", resolve("172.18.0.2", "evil.example.com", "172.18.0.0/16"));
    assertEquals(
        "203.0.113.7",
        resolve("172.18.0.2", "evil.example.com, 203.0.113.7", "172.18.0.0/16"));
  }

  @Test
  void stopsTrustingTheListAtAMalformedHop() {
    assertEquals("172.18.0.3", resolve("172.18.0.2", "1.1.1.1, garbage, 172.18.0.3", "172.18.0.0/16"));
  }

  @Test
  void rejectsHostNamesInTheTrustedProxyConfiguration() {
    org.junit.jupiter.api.Assertions.assertThrows(
        IllegalArgumentException.class, () -> new ClientAddress("localhost"));
  }

  @Test
  void ignoresForwardedAddressFromUntrustedSocket() {
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.10");
    when(request.getRemoteAddr()).thenReturn("198.51.100.7");

    assertEquals("198.51.100.7", new ClientAddress("172.18.0.0/16").resolve(request));
  }

  private static String resolve(String remoteAddress, String forwardedFor, String trustedCidrs) {
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getRemoteAddr()).thenReturn(remoteAddress);
    when(request.getHeader("X-Forwarded-For")).thenReturn(forwardedFor);
    return new ClientAddress(trustedCidrs).resolve(request);
  }
}
