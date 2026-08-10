package com.townpet.engagement;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import java.util.Objects;
import java.util.UUID;
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
class BlockedEngagementPolicyTest {
  private static final UUID PUBLICATION_ID =
      UUID.fromString("00000000-0000-4000-8000-000000000507");
  private static final UUID AUTHOR_ID = UUID.fromString("00000000-0000-4000-8000-000000000201");

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
    jdbc.update("DELETE FROM engagement_comment");
    jdbc.update("DELETE FROM engagement_reaction");
    jdbc.update("DELETE FROM engagement_bookmark");
    jdbc.update("DELETE FROM relationship_block");
    jdbc.update("DELETE FROM publication");
    jdbc.update(
        "INSERT INTO publication (id, author_member_id, type, scope, neighborhood_id, title, body, lifecycle, created_at, updated_at, version) "
            + "VALUES (?, ?, 'FREE_BOARD', 'GLOBAL', NULL, 'blocked', 'blocked body', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)",
        PUBLICATION_ID,
        AUTHOR_ID);
    jdbc.update(
        "INSERT INTO relationship_block (id, blocker_member_id, blocked_member_id, created_at) VALUES (?, ?, ?, CURRENT_TIMESTAMP)",
        UUID.fromString("00000000-0000-4000-8000-000000000607"),
        UUID.fromString("00000000-0000-4000-8000-000000000202"),
        AUTHOR_ID);
  }

  @Test
  void blockedMemberCannotReadOrMutateEngagementButGuestCanRead() throws Exception {
    Cookie blockedViewer = login("demo-member-2@townpet.local");
    mockMvc
        .perform(get("/api/v1/publications/{id}/comments", PUBLICATION_ID).cookie(blockedViewer))
        .andExpect(status().isNotFound());
    mockMvc
        .perform(get("/api/v1/publications/{id}/reaction", PUBLICATION_ID).cookie(blockedViewer))
        .andExpect(status().isNotFound());
    mockMvc
        .perform(get("/api/v1/publications/{id}/bookmark", PUBLICATION_ID).cookie(blockedViewer))
        .andExpect(status().isNotFound());
    mockMvc
        .perform(
            post("/api/v1/publications/{id}/comments", PUBLICATION_ID)
                .cookie(blockedViewer)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"body\":\"blocked\"}"))
        .andExpect(status().isNotFound());
    mockMvc
        .perform(
            put("/api/v1/publications/{id}/reaction", PUBLICATION_ID)
                .cookie(blockedViewer)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"active\":true}"))
        .andExpect(status().isNotFound());
    mockMvc
        .perform(
            put("/api/v1/publications/{id}/bookmark", PUBLICATION_ID)
                .cookie(blockedViewer)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"active\":true}"))
        .andExpect(status().isNotFound());

    mockMvc
        .perform(get("/api/v1/publications/{id}/comments", PUBLICATION_ID))
        .andExpect(status().isOk());
    mockMvc
        .perform(get("/api/v1/publications/{id}/reaction", PUBLICATION_ID))
        .andExpect(status().isOk());
    mockMvc
        .perform(get("/api/v1/publications/{id}/bookmark", PUBLICATION_ID))
        .andExpect(status().isOk());
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
}
