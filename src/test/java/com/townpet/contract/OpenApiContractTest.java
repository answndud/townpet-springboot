package com.townpet.contract;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class OpenApiContractTest {

  private static final Path SPEC = Path.of("api/openapi/townpet.yaml");

  @Test
  void exposesVersionedRepresentativeRoutesAndSharedSchemas() throws Exception {
    String contract = Files.readString(SPEC);

    assertThat(contract).contains("openapi: 3.1.0", "/api/v1/feed:", "/api/v1/publications:");
    assertThat(contract).contains("ProblemDetail:", "Idempotency-Key", "sessionCookie:");
    assertThat(contract).contains("format: uuid", "format: date-time", "application/problem+json");
  }
}
