package com.townpet.contract;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.townpet.support.PostgresTestContainer;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * Keeps {@code api/openapi/townpet-v1.yaml} honest: every documented path and method must still
 * exist in the application, and every reference inside the document must resolve.
 */
@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Tag("integration")
class OpenApiContractTest {
  private static final Path SPEC = Path.of("api/openapi/townpet-v1.yaml");
  private static final Set<String> METHODS = Set.of("get", "post", "put", "patch", "delete");

  @DynamicPropertySource
  static void databaseProperties(DynamicPropertyRegistry registry) {
    PostgresTestContainer.register(registry);
  }

  @Autowired
  @Qualifier("requestMappingHandlerMapping")
  RequestMappingHandlerMapping handlerMapping;

  @Test
  void everyDocumentedOperationExistsInTheApplication() throws Exception {
    JsonNode spec = readSpec();
    List<String> missing = new ArrayList<>();
    int operations = 0;
    for (var path : spec.path("paths").properties()) {
      String concrete = path.getKey().replaceAll("\\{[^}]+}", UUID.randomUUID().toString());
      for (var entry : path.getValue().properties()) {
        if (!METHODS.contains(entry.getKey())) continue;
        operations++;
        MockHttpServletRequest request =
            new MockHttpServletRequest(entry.getKey().toUpperCase(), concrete);
        try {
          if (handlerMapping.getHandler(request) == null) {
            missing.add(entry.getKey().toUpperCase() + " " + path.getKey());
          }
        } catch (Exception exception) {
          missing.add(
              entry.getKey().toUpperCase() + " " + path.getKey() + " (" + exception.getMessage() + ")");
        }
      }
    }
    assertThat(operations).as("documented operations").isGreaterThan(20);
    assertThat(missing).as("documented operations the application does not serve").isEmpty();
  }

  @Test
  void everyReferenceInsideTheDocumentResolves() throws Exception {
    JsonNode spec = readSpec();
    List<String> unresolved = new ArrayList<>();
    collectUnresolved(spec, spec, unresolved);
    assertThat(unresolved).as("unresolved $ref values").isEmpty();
    assertThat(spec.path("openapi").asText()).startsWith("3.");
    assertThat(spec.at("/components/schemas/Problem/properties/code").isMissingNode()).isFalse();
  }

  private static void collectUnresolved(JsonNode root, JsonNode node, List<String> unresolved) {
    if (node.isObject()) {
      JsonNode ref = node.get("$ref");
      if (ref != null && ref.isTextual()) {
        String pointer = ref.asText().replaceFirst("^#", "");
        if (!ref.asText().startsWith("#/") || root.at(pointer).isMissingNode()) {
          unresolved.add(ref.asText());
        }
      }
      node.elements().forEachRemaining(child -> collectUnresolved(root, child, unresolved));
    } else if (node.isArray()) {
      node.elements().forEachRemaining(child -> collectUnresolved(root, child, unresolved));
    }
  }

  private static JsonNode readSpec() throws IOException {
    return new ObjectMapper(new YAMLFactory()).readTree(Files.readString(SPEC));
  }
}
