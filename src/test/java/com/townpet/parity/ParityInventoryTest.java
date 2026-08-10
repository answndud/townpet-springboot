package com.townpet.parity;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ParityInventoryTest {

  private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());
  private static final Set<String> SPRING_STATUSES =
      Set.of("pending", "adapter", "spring-owned", "verified", "excluded");

  @Test
  void capturesTheLegacyPageAndApiInventory() throws IOException {
    JsonNode matrix = readMatrix();
    JsonNode pages = matrix.path("pages");
    JsonNode apiRoutes = matrix.path("apiRoutes");

    assertThat(pages).hasSize(49);
    assertThat(apiRoutes).hasSize(55);
    assertThat(matrix.path("counts").path("pages").asInt()).isEqualTo(pages.size());
    assertThat(matrix.path("counts").path("apiRoutes").asInt()).isEqualTo(apiRoutes.size());
    assertUniquePaths(pages, "page");
    assertUniquePaths(apiRoutes, "api route");
  }

  @Test
  void keepsRouteTemplatesAndHttpMethodsExplicit() throws IOException {
    JsonNode apiRoutes = readMatrix().path("apiRoutes");

    for (JsonNode route : apiRoutes) {
      String path = route.path("path").asText();
      assertThat(path).startsWith("/api/");
      assertThat(route.path("methods")).isNotEmpty();
      assertThat(route.path("legacy").asBoolean()).isTrue();
      String springStatus = route.path("spring").asText();
      assertThat(SPRING_STATUSES).contains(springStatus);
      if (springStatus.equals("excluded")) {
        assertThat(route.path("decision").asText()).startsWith("ADR-");
      }
    }
  }

  private static JsonNode readMatrix() throws IOException {
    try (InputStream input = ParityInventoryTest.class.getResourceAsStream("/parity/matrix.yaml")) {
      assertThat(input).as("parity matrix resource").isNotNull();
      return YAML.readTree(input);
    }
  }

  private static void assertUniquePaths(JsonNode entries, String label) {
    Set<String> paths = new HashSet<>();
    for (JsonNode entry : entries) {
      assertThat(paths.add(entry.path("path").asText())).as(label).isTrue();
    }
  }
}
