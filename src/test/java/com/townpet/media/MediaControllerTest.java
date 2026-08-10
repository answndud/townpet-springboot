package com.townpet.media;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
class MediaControllerTest {
  private static final String CHECKSUM =
      "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

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
    jdbc.update("DELETE FROM upload_asset");
    jdbc.update("DELETE FROM publication");
  }

  @Test
  void uploadMustFinalizeBeforeAuthorCanAttachItToAnActivePublication() throws Exception {
    Cookie author = login("demo-member-1@townpet.local");
    Cookie other = login("demo-member-2@townpet.local");
    String publicationId = createPublication(author);
    String assetId = createUpload(author);

    mockMvc
        .perform(
            post(
                    "/api/v1/media/uploads/{assetId}/attachments/publications/{publicationId}",
                    assetId,
                    publicationId)
                .cookie(author)
                .with(csrf()))
        .andExpect(status().isConflict());
    mockMvc
        .perform(
            post("/api/v1/media/uploads/{assetId}/finalize", assetId)
                .cookie(author)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"checksumSha256\":\"bad\"}"))
        .andExpect(status().isBadRequest());
    mockMvc
        .perform(
            post("/api/v1/media/uploads/{assetId}/finalize", assetId)
                .cookie(author)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"checksumSha256\":\"" + CHECKSUM + "\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("READY"));
    mockMvc
        .perform(
            post(
                    "/api/v1/media/uploads/{assetId}/attachments/publications/{publicationId}",
                    assetId,
                    publicationId)
                .cookie(other)
                .with(csrf()))
        .andExpect(status().isNotFound());
    mockMvc
        .perform(
            post(
                    "/api/v1/media/uploads/{assetId}/attachments/publications/{publicationId}",
                    assetId,
                    publicationId)
                .cookie(author)
                .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("ATTACHED"))
        .andExpect(jsonPath("$.publicationId").value(publicationId));
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM upload_asset WHERE status = 'ATTACHED'", Integer.class))
        .isEqualTo(1);
  }

  private String createUpload(Cookie session) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/media/uploads")
                    .cookie(session)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"checksumSha256\":\""
                            + CHECKSUM
                            + "\",\"contentType\":\"image/jpeg\",\"byteSize\":1024}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UPLOADING"))
            .andReturn();
    return new com.fasterxml.jackson.databind.ObjectMapper()
        .readTree(result.getResponse().getContentAsString())
        .path("id")
        .asText();
  }

  private String createPublication(Cookie session) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/publications")
                    .cookie(session)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"title\":\"media\",\"body\":\"body\",\"scope\":\"GLOBAL\"}"))
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
