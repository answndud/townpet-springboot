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
class BookmarkControllerTest {
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
    jdbc.update("DELETE FROM engagement_bookmark");
    jdbc.update("DELETE FROM publication");
  }

  @Test
  void memberBookmarkIsIdempotentAndDeletedPublicationsRejectChanges() throws Exception {
    Cookie author = login("demo-member-1@townpet.local");
    Cookie other = login("demo-member-2@townpet.local");
    String publicationId = createPublication(author);

    mockMvc
        .perform(get("/api/v1/publications/{id}/bookmark", publicationId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.active").value(false));
    mockMvc
        .perform(
            put("/api/v1/publications/{id}/bookmark", publicationId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"active\":true}"))
        .andExpect(status().isUnauthorized());
    setBookmark(publicationId, author, true);
    setBookmark(publicationId, author, true);
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject("SELECT COUNT(*) FROM engagement_bookmark", Integer.class))
        .isEqualTo(1);
    mockMvc
        .perform(get("/api/v1/publications/{id}/bookmark", publicationId).cookie(other))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.active").value(false));
    setBookmark(publicationId, author, false);
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject("SELECT COUNT(*) FROM engagement_bookmark", Integer.class))
        .isEqualTo(0);
    mockMvc
        .perform(
            delete("/api/v1/publications/{id}", publicationId)
                .cookie(author)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\":0}"))
        .andExpect(status().isNoContent());
    mockMvc
        .perform(get("/api/v1/publications/{id}/bookmark", publicationId))
        .andExpect(status().isNotFound());
    mockMvc
        .perform(
            put("/api/v1/publications/{id}/bookmark", publicationId)
                .cookie(other)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"active\":true}"))
        .andExpect(status().isNotFound());
  }

  private void setBookmark(String id, Cookie session, boolean active) throws Exception {
    mockMvc
        .perform(
            put("/api/v1/publications/{id}/bookmark", id)
                .cookie(session)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"active\":" + active + "}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.active").value(active));
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

  private String createPublication(Cookie session) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/publications")
                    .cookie(session)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"title\":\"bookmark\",\"body\":\"body\",\"scope\":\"GLOBAL\"}"))
            .andExpect(status().isCreated())
            .andReturn();
    return com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), "$.id");
  }
}
