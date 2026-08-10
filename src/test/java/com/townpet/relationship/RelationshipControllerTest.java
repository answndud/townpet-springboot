package com.townpet.relationship;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class RelationshipControllerTest {
  @Container
  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>(
              DockerImageName.parse("postgis/postgis:18-3.6").asCompatibleSubstituteFor("postgres"))
          .withDatabaseName("townpet")
          .withUsername("townpet_app")
          .withPassword("townpet_test")
          .withCopyFileToContainer(
              MountableFile.forClasspathResource("postgres-extensions.sql"),
              "/docker-entrypoint-initdb.d/001_extensions.sql");

  @DynamicPropertySource
  static void databaseProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
  }

  @Autowired MockMvc mockMvc;
  @Autowired JdbcTemplate jdbc;

  @BeforeEach
  void resetState() {
    jdbc.update("DELETE FROM relationship_follow");
    jdbc.update("DELETE FROM relationship_block");
  }

  @Test
  void followAndBlockAreIdempotentAndSelfTargetIsRejected() throws Exception {
    Cookie viewer = login("demo-member-1@townpet.local");
    Cookie target = login("demo-member-2@townpet.local");
    UUID targetId = memberId(target);

    mockMvc
        .perform(get("/api/v1/members/{id}/relationship", targetId).cookie(viewer))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.following").value(false))
        .andExpect(jsonPath("$.blocking").value(false));
    set(viewer, targetId, true, false);
    set(viewer, targetId, true, false);
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject("SELECT COUNT(*) FROM relationship_follow", Integer.class))
        .isEqualTo(1);
    set(viewer, targetId, true, true);
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject("SELECT COUNT(*) FROM relationship_follow", Integer.class))
        .isEqualTo(0);
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject("SELECT COUNT(*) FROM relationship_block", Integer.class))
        .isEqualTo(1);
    set(viewer, targetId, false, false);
    mockMvc
        .perform(
            put("/api/v1/members/{id}/relationship", memberId(viewer))
                .cookie(viewer)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"following\":true,\"blocking\":false}"))
        .andExpect(status().isBadRequest());
    mockMvc
        .perform(get("/api/v1/members/{id}/relationship", UUID.randomUUID()).cookie(viewer))
        .andExpect(status().isNotFound());
  }

  @Test
  void concurrentFollowRequestsStayUniqueAndPrincipalStateIsIsolated() throws Exception {
    Cookie viewer = login("demo-member-1@townpet.local");
    Cookie target = login("demo-member-2@townpet.local");
    UUID targetId = memberId(target);
    ExecutorService executor = Executors.newFixedThreadPool(2);
    try {
      Future<Integer> first = executor.submit(() -> relationshipStatus(viewer, targetId));
      Future<Integer> second = executor.submit(() -> relationshipStatus(viewer, targetId));
      org.assertj.core.api.Assertions.assertThat(first.get()).isEqualTo(200);
      org.assertj.core.api.Assertions.assertThat(second.get()).isEqualTo(200);
    } finally {
      executor.shutdownNow();
    }
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject("SELECT COUNT(*) FROM relationship_follow", Integer.class))
        .isEqualTo(1);
    mockMvc
        .perform(
            get(
                    "/api/v1/members/{id}/relationship",
                    UUID.fromString("00000000-0000-4000-8000-000000000201"))
                .cookie(target))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.following").value(false))
        .andExpect(jsonPath("$.blocking").value(false));
  }

  private int relationshipStatus(Cookie session, UUID targetId) throws Exception {
    return mockMvc
        .perform(
            put("/api/v1/members/{id}/relationship", targetId)
                .cookie(session)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"following\":true,\"blocking\":false}"))
        .andReturn()
        .getResponse()
        .getStatus();
  }

  private void set(Cookie session, UUID targetId, boolean following, boolean blocking)
      throws Exception {
    mockMvc
        .perform(
            put("/api/v1/members/{id}/relationship", targetId)
                .cookie(session)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"following\":" + following + ",\"blocking\":" + blocking + "}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.following").value(following && !blocking))
        .andExpect(jsonPath("$.blocking").value(blocking));
  }

  private Cookie login(String email) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                        "/api/v1/auth/sessions")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"" + email + "\",\"password\":\"townpet-demo-123!\"}"))
            .andExpect(status().isCreated())
            .andReturn();
    return Objects.requireNonNull(result.getResponse().getCookie("SESSION"));
  }

  private UUID memberId(Cookie session) throws Exception {
    MvcResult result =
        mockMvc
            .perform(get("/api/v1/members/me").cookie(session))
            .andExpect(status().isOk())
            .andReturn();
    return UUID.fromString(
        new com.fasterxml.jackson.databind.ObjectMapper()
            .readTree(result.getResponse().getContentAsString())
            .path("id")
            .asText());
  }
}
