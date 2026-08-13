package com.townpet.common;

import jakarta.servlet.http.HttpServletRequest;

/** Resolves the client address used by local abuse limits behind the Caddy reverse proxy. */
public final class ClientAddress {
  private ClientAddress() {}

  public static String resolve(HttpServletRequest request) {
    String forwarded = request.getHeader("X-Forwarded-For");
    if (forwarded != null && !forwarded.isBlank()) {
      String first = forwarded.split(",", 2)[0].trim();
      if (!first.isBlank()) return first;
    }
    return request.getRemoteAddr();
  }
}
