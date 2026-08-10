package com.townpet.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UuidV7Test {
  @Test
  void createsRfc9562VersionSevenIdentifierWithCurrentTimestamp() {
    long before = Instant.now().toEpochMilli();
    UUID id = UuidV7.randomUuid();
    long after = Instant.now().toEpochMilli();

    assertThat(id.version()).isEqualTo(7);
    assertThat(id.variant()).isEqualTo(2);
    assertThat(id.getMostSignificantBits() >>> 16).isBetween(before, after);
  }
}
