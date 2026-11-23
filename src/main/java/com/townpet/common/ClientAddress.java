package com.townpet.common;

import jakarta.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

/**
 * Resolves the client address used by local abuse limits behind the Caddy reverse proxy.
 *
 * <p>{@code X-Forwarded-For} is only read when the socket peer is a trusted proxy. Each proxy
 * appends the address it received the request from, so the list is walked from the right and the
 * first address that is not a trusted proxy is the client. Everything to the left of it is
 * client-controlled and ignored. Only IP literals are accepted, so a header value can never
 * trigger a DNS lookup.
 */
@Component
public final class ClientAddress {
  private static final String FORWARDED_FOR = "X-Forwarded-For";

  private final List<Cidr> trustedProxies;

  public ClientAddress(@Value("${townpet.security.trusted-proxy-cidrs:}") String configuredCidrs) {
    this.trustedProxies = parseCidrs(configuredCidrs);
  }

  public String resolve(HttpServletRequest request) {
    String remoteAddress = request.getRemoteAddr();
    InetAddress peer = literal(remoteAddress);
    if (peer == null || !isTrustedProxy(peer)) return remoteAddress;
    String forwarded = request.getHeader(FORWARDED_FOR);
    if (forwarded == null || forwarded.isBlank()) return remoteAddress;

    String[] hops = forwarded.split(",");
    String client = null;
    for (int index = hops.length - 1; index >= 0; index--) {
      InetAddress hop = literal(hops[index].trim());
      // A malformed hop cannot be attributed to any proxy, so stop trusting the list here.
      if (hop == null) break;
      client = hop.getHostAddress();
      if (!isTrustedProxy(hop)) return client;
    }
    return client != null ? client : remoteAddress;
  }

  private boolean isTrustedProxy(InetAddress address) {
    return trustedProxies.stream().anyMatch(cidr -> cidr.contains(address));
  }

  /** Parses an IP literal without ever resolving a host name. */
  @Nullable
  private static InetAddress literal(@Nullable String value) {
    if (value == null || value.isBlank()) return null;
    try {
      return InetAddress.ofLiteral(value.trim());
    } catch (IllegalArgumentException exception) {
      return null;
    }
  }

  private static List<Cidr> parseCidrs(String configuredCidrs) {
    if (configuredCidrs == null || configuredCidrs.isBlank()) return List.of();
    List<Cidr> result = new ArrayList<>();
    StringTokenizer tokenizer = new StringTokenizer(configuredCidrs, ",");
    while (tokenizer.hasMoreTokens()) {
      result.add(Cidr.parse(tokenizer.nextToken().trim()));
    }
    return List.copyOf(result);
  }

  private static final class Cidr {
    private final byte[] network;
    private final int prefixLength;

    private Cidr(byte[] network, int prefixLength) {
      this.network = network;
      this.prefixLength = prefixLength;
    }

    private static Cidr parse(String value) {
      String[] parts = value.split("/", 2);
      InetAddress address = literal(parts[0]);
      if (address == null) throw new IllegalArgumentException("Invalid trusted proxy CIDR: " + value);
      try {
        int prefix =
            parts.length == 2 ? Integer.parseInt(parts[1]) : address.getAddress().length * 8;
        if (prefix < 0 || prefix > address.getAddress().length * 8) {
          throw new IllegalArgumentException("Invalid trusted proxy CIDR: " + value);
        }
        return new Cidr(address.getAddress(), prefix);
      } catch (NumberFormatException exception) {
        throw new IllegalArgumentException("Invalid trusted proxy CIDR: " + value, exception);
      }
    }

    private boolean contains(InetAddress address) {
      byte[] candidate = address.getAddress();
      if (candidate.length != network.length) return false;
      int fullBytes = prefixLength / 8;
      int remainingBits = prefixLength % 8;
      if (!Arrays.equals(Arrays.copyOf(candidate, fullBytes), Arrays.copyOf(network, fullBytes))) {
        return false;
      }
      if (remainingBits == 0) return true;
      int mask = 0xff << (8 - remainingBits);
      return (candidate[fullBytes] & mask) == (network[fullBytes] & mask);
    }
  }
}
