package com.townpet.common;

import jakarta.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Resolves the client address used by local abuse limits behind the Caddy reverse proxy. */
@Component
public final class ClientAddress {
  private final List<Cidr> trustedProxies;

  public ClientAddress(@Value("${townpet.security.trusted-proxy-cidrs:}") String configuredCidrs) {
    this.trustedProxies = parseCidrs(configuredCidrs);
  }

  public String resolve(HttpServletRequest request) {
    String remoteAddress = request.getRemoteAddr();
    String forwarded = request.getHeader("X-Forwarded-For");
    if (isTrustedProxy(remoteAddress) && forwarded != null && !forwarded.isBlank()) {
      String first = forwarded.split(",", 2)[0].trim();
      if (isAddress(first)) return first;
    }
    return remoteAddress;
  }

  private boolean isTrustedProxy(String address) {
    if (address == null || address.isBlank()) return false;
    return trustedProxies.stream().anyMatch(cidr -> cidr.contains(address));
  }

  private static boolean isAddress(String value) {
    if (value.isBlank() || value.indexOf(' ') >= 0 || value.indexOf('\t') >= 0) return false;
    try {
      InetAddress.getByName(value);
      return true;
    } catch (UnknownHostException exception) {
      return false;
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
      try {
        InetAddress address = InetAddress.getByName(parts[0]);
        int prefix =
            parts.length == 2 ? Integer.parseInt(parts[1]) : address.getAddress().length * 8;
        if (prefix < 0 || prefix > address.getAddress().length * 8) {
          throw new IllegalArgumentException("Invalid trusted proxy CIDR: " + value);
        }
        return new Cidr(address.getAddress(), prefix);
      } catch (UnknownHostException | NumberFormatException exception) {
        throw new IllegalArgumentException("Invalid trusted proxy CIDR: " + value, exception);
      }
    }

    private boolean contains(String value) {
      try {
        byte[] candidate = InetAddress.getByName(value).getAddress();
        if (candidate.length != network.length) return false;
        int fullBytes = prefixLength / 8;
        int remainingBits = prefixLength % 8;
        if (!Arrays.equals(
            Arrays.copyOf(candidate, fullBytes), Arrays.copyOf(network, fullBytes))) {
          return false;
        }
        if (remainingBits == 0) return true;
        int mask = 0xff << (8 - remainingBits);
        return (candidate[fullBytes] & mask) == (network[fullBytes] & mask);
      } catch (UnknownHostException exception) {
        return false;
      }
    }
  }
}
