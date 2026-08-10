package com.townpet.common;

import java.security.SecureRandom;
import java.util.UUID;

public final class UuidV7 {
  private static final SecureRandom RANDOM = new SecureRandom();

  private UuidV7() {}

  public static UUID randomUuid() {
    long unixMillis = System.currentTimeMillis() & 0x0000FFFFFFFFFFFFL;
    long randomA = RANDOM.nextInt(1 << 12);
    long mostSignificantBits = (unixMillis << 16) | 0x7000L | randomA;
    long leastSignificantBits = (RANDOM.nextLong() & 0x3FFFFFFFFFFFFFFFL) | 0x8000000000000000L;
    return new UUID(mostSignificantBits, leastSignificantBits);
  }
}
