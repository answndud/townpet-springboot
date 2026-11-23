package com.townpet.identity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.townpet.support.PostgresTestContainer;
import jakarta.servlet.http.Cookie;
import java.util.Objects;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Characterizes who may reach which kind of endpoint: anonymous visitors, members and moderators
 * (with completed MFA). "Allowed" means the request got past authorization; the handler may still
 * answer 400 or 404. Anything not listed in {@code SecurityConfig} must stay behind
 * authentication.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("integration")
class SecurityAuthorizationMatrixTest {
  private static final String MISSING_ID = "00000000-0000-4000-8000-000000009999";

  /** Allowed past authorization (any status except 401/403). */
  private static final String ALLOWED = "ALLOWED";
  /** Rejected because nobody is signed in. */
  private static final String UNAUTHENTICATED = "UNAUTHENTICATED";
  /** Rejected because the signed-in account may not use the endpoint. */
  private static final String FORBIDDEN = "FORBIDDEN";

  @DynamicPropertySource
  static void databaseProperties(DynamicPropertyRegistry registry) {
    PostgresTestContainer.register(registry);
  }

  @Autowired MockMvc mockMvc;
  @Autowired JdbcTemplate jdbc;

  private Cookie member;
  private Cookie moderator;

  @BeforeAll
  void signIn() throws Exception {
    member = login("demo-member-1@townpet.local");
    // The seeded moderator has its own password; reuse the member's so the shared helper works.
    jdbc.update(
        "UPDATE identity_credential SET password_hash = (SELECT password_hash FROM"
            + " identity_credential WHERE email = ?) WHERE email = ?",
        "demo-member-1@townpet.local",
        "demo-moderator@townpet.local");
    moderator = MfaTestSupport.completeEnrollment(mockMvc, login("demo-moderator@townpet.local"));
  }

  static Stream<Arguments> endpoints() {
    return Stream.of(
        // Public reads and health.
        row("GET", "/actuator/health", ALLOWED, ALLOWED, ALLOWED),
        row("GET", "/api/v1/auth/csrf", ALLOWED, ALLOWED, ALLOWED),
        row("GET", "/api/v1/discovery", ALLOWED, ALLOWED, ALLOWED),
        row("GET", "/api/v1/publications/" + MISSING_ID, ALLOWED, ALLOWED, ALLOWED),
        row("GET", "/api/v1/publications/" + MISSING_ID + "/comments", ALLOWED, ALLOWED, ALLOWED),
        row("GET", "/api/v1/lost-found/alerts", ALLOWED, ALLOWED, ALLOWED),
        // Public writes: sign-in, telemetry and guest flows.
        row("POST", "/api/v1/auth/sessions", ALLOWED, ALLOWED, ALLOWED),
        row("POST", "/api/search/log", ALLOWED, ALLOWED, ALLOWED),
        row("POST", "/api/guest/authors", ALLOWED, ALLOWED, ALLOWED),
        // Member-only operations: staff review accounts are turned away.
        // The body is valid so validation does not answer 400 before method security runs.
        row(
            "POST",
            "/api/v1/publications",
            "{\"title\":\"제목\",\"body\":\"본문\"}",
            UNAUTHENTICATED,
            ALLOWED,
            FORBIDDEN),
        row(
            "PUT",
            "/api/v1/publications/" + MISSING_ID + "/bookmark",
            "{\"active\":true}",
            UNAUTHENTICATED,
            ALLOWED,
            FORBIDDEN),
        row("GET", "/api/v1/notifications", UNAUTHENTICATED, ALLOWED, FORBIDDEN),
        // Any signed-in account, staff included.
        row("GET", "/api/v1/members/me", UNAUTHENTICATED, ALLOWED, ALLOWED),
        // Moderator-only areas.
        row("GET", "/api/admin/comments", UNAUTHENTICATED, FORBIDDEN, ALLOWED),
        row("GET", "/api/admin/auth-audits", UNAUTHENTICATED, FORBIDDEN, ALLOWED),
        row("GET", "/api/v1/operations/web-vitals/summary", UNAUTHENTICATED, FORBIDDEN, ALLOWED),
        row("GET", "/api/v1/trust-reports", UNAUTHENTICATED, FORBIDDEN, ALLOWED),
        row("GET", "/actuator/metrics", UNAUTHENTICATED, FORBIDDEN, ALLOWED),
        row("POST", "/api/v1/auth/mfa/enrollment", UNAUTHENTICATED, FORBIDDEN, ALLOWED),
        // Deny by default: anything that is not listed requires a signed-in account.
        row("GET", "/api/v1/not-a-real-route", UNAUTHENTICATED, ALLOWED, ALLOWED),
        row("POST", "/api/v1/not-a-real-route", UNAUTHENTICATED, ALLOWED, ALLOWED),
        row("GET", "/api/_test/anything", UNAUTHENTICATED, ALLOWED, ALLOWED));
  }

  private static Arguments row(
      String method, String path, String anonymous, String member, String moderator) {
    return row(method, path, "{}", anonymous, member, moderator);
  }

  private static Arguments row(
      String method, String path, String body, String anonymous, String member, String moderator) {
    return Arguments.of(method, path, body, anonymous, member, moderator);
  }

  @ParameterizedTest(name = "{0} {1}")
  @MethodSource("endpoints")
  void enforcesTheAuthorizationMatrix(
      String method,
      String path,
      String body,
      String anonymous,
      String member,
      String moderator)
      throws Exception {
    assertOutcome("anonymous", anonymous, send(method, path, body, null));
    assertOutcome("member", member, send(method, path, body, this.member));
    assertOutcome("moderator", moderator, send(method, path, body, this.moderator));
  }

  @Test
  void rejectsStateChangingRequestsWithoutACsrfToken() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/publications")
                .cookie(member)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isForbidden());
  }

  private int send(String method, String path, String body, Cookie session) throws Exception {
    MockHttpServletRequestBuilder builder = request(HttpMethod.valueOf(method), path);
    if (!"GET".equals(method)) {
      builder.with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body);
    }
    if (session != null) builder.cookie(session);
    return mockMvc.perform(builder).andReturn().getResponse().getStatus();
  }

  private static void assertOutcome(String actor, String expected, int status) {
    String label = actor + " expected " + expected + " but got HTTP " + status;
    switch (expected) {
      case ALLOWED -> {
        assertNotEquals(401, status, label);
        assertNotEquals(403, status, label);
      }
      case UNAUTHENTICATED -> assertEquals(401, status, label);
      case FORBIDDEN -> assertEquals(403, status, label);
      default -> throw new IllegalArgumentException(expected);
    }
  }

  private Cookie login(String email) throws Exception {
    var response =
        mockMvc
            .perform(
                post("/api/v1/auth/sessions")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"" + email + "\",\"password\":\"townpet-demo-123!\"}"))
            .andReturn()
            .getResponse();
    assertEquals(201, response.getStatus(), email + " sign-in: " + response.getContentAsString());
    return Objects.requireNonNull(response.getCookie("SESSION"));
  }
}
