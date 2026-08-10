package com.townpet.engagement;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import java.util.Objects;
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
class ReactionControllerTest {
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
    jdbc.update("DELETE FROM relationship_block");
    jdbc.update("DELETE FROM engagement_reaction");
    jdbc.update("DELETE FROM publication");
  }

  @Test
  void memberReactionIsIdempotentAndDeletedPublicationsRejectChanges() throws Exception {
    Cookie author = login("demo-member-1@townpet.local");
    Cookie other = login("demo-member-2@townpet.local");
    String publicationId = createPublication(author);

    mockMvc
        .perform(get("/api/v1/publications/{publicationId}/reaction", publicationId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.active").value(false))
        .andExpect(jsonPath("$.count").value(0));
    mockMvc
        .perform(
            put("/api/v1/publications/{publicationId}/reaction", publicationId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"active\":true}"))
        .andExpect(status().isUnauthorized());

    setReaction(publicationId, author, true, 1);
    setReaction(publicationId, author, true, 1);
    assertCount(1);
    setReaction(publicationId, other, true, 2);
    setReaction(publicationId, author, false, 1);
    assertCount(1);
    mockMvc
        .perform(get("/api/v1/publications/{publicationId}/reaction", publicationId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.active").value(false))
        .andExpect(jsonPath("$.count").value(1));
    setReaction(publicationId, other, false, 0);
    assertCount(0);
    mockMvc
        .perform(get("/api/v1/publications/{publicationId}/reaction", publicationId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.active").value(false))
        .andExpect(jsonPath("$.count").value(0));

    mockMvc
        .perform(
            delete("/api/v1/publications/{publicationId}", publicationId)
                .cookie(author)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\":0}"))
        .andExpect(status().isNoContent());
    mockMvc
        .perform(get("/api/v1/publications/{publicationId}/reaction", publicationId))
        .andExpect(status().isNotFound());
    mockMvc
        .perform(
            put("/api/v1/publications/{publicationId}/reaction", publicationId)
                .cookie(other)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"active\":true}"))
        .andExpect(status().isNotFound());
  }

  private void setReaction(String publicationId, Cookie session, boolean active, int count)
      throws Exception {
    mockMvc
        .perform(
            put("/api/v1/publications/{publicationId}/reaction", publicationId)
                .cookie(session)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"active\":" + active + "}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.active").value(active))
        .andExpect(jsonPath("$.count").value(count));
  }

  private void assertCount(int count) {
    Integer rows = jdbc.queryForObject("SELECT COUNT(*) FROM engagement_reaction", Integer.class);
    org.assertj.core.api.Assertions.assertThat(rows).isEqualTo(count);
  }

  private String createPublication(Cookie session) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/publications")
                    .cookie(session)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"title\":\"reaction 대상\",\"body\":\"좋아요를 눌러 주세요.\",\"scope\":\"GLOBAL\"}"))
            .andExpect(status().isCreated())
            .andReturn();
    return new com.fasterxml.jackson.databind.ObjectMapper()
        .readTree(result.getResponse().getContentAsString())
        .path("id")
        .asText();
  }

  private Cookie login(String email) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/auth/sessions")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"" + email + "\",\"password\":\"townpet-demo-123!\"}"))
            .andExpect(status().isCreated())
            .andReturn();
    return Objects.requireNonNull(result.getResponse().getCookie("SESSION"));
  }
}
