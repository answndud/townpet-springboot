package com.townpet.media;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.townpet.identity.MfaTestSupport;
import jakarta.servlet.http.Cookie;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Objects;
import java.util.UUID;
import javax.imageio.ImageIO;
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
  private static final byte[] JPEG_BYTES = createJpegBytes();
  private static final String CHECKSUM = checksum(JPEG_BYTES);

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
  @Autowired LocalObjectStorage storage;
  @Autowired MediaService media;

  @BeforeEach
  void resetState() {
    jdbc.update("DELETE FROM upload_asset");
    jdbc.update("DELETE FROM publication");
  }

  @Test
  void uploadCreationIsLimitedPerMemberPerUtcDay() throws Exception {
    Cookie author = login("demo-member-1@townpet.local");

    for (int attempt = 0; attempt < MediaService.MAX_UPLOADS_PER_UTC_DAY; attempt++) {
      createUpload(author);
    }

    mockMvc
        .perform(
            post("/api/v1/media/uploads")
                .cookie(author)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"checksumSha256\":\""
                        + CHECKSUM
                        + "\",\"contentType\":\"image/jpeg\",\"byteSize\":"
                        + JPEG_BYTES.length
                        + "}"))
        .andExpect(status().isTooManyRequests());
  }

  @Test
  void uploadMustFinalizeBeforeAuthorCanAttachItToAnActivePublication() throws Exception {
    Cookie author = login("demo-member-1@townpet.local");
    Cookie other = login("demo-member-2@townpet.local");
    String publicationId = createPublication(author);
    String assetId = createUpload(author);
    storage.put(objectKey(assetId), "image/jpeg", JPEG_BYTES);

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
    mockMvc
        .perform(get("/api/v1/media/uploads/{assetId}/url", assetId).cookie(author))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.url")
                .value(org.hamcrest.Matchers.startsWith("http://local-object-storage.test/")));
    mockMvc
        .perform(get("/api/v1/media/uploads/{assetId}/url", assetId).cookie(other))
        .andExpect(status().isNotFound());
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM upload_asset WHERE status = 'ATTACHED'", Integer.class))
        .isEqualTo(1);

    String expiredAssetId = createUpload(author);
    storage.put(objectKey(expiredAssetId), "image/jpeg", JPEG_BYTES);
    jdbc.update(
        "UPDATE upload_asset SET expires_at = CURRENT_TIMESTAMP - INTERVAL '1 minute' WHERE id = ?",
        UUID.fromString(expiredAssetId));
    mockMvc
        .perform(
            post("/api/v1/media/uploads/{assetId}/finalize", expiredAssetId)
                .cookie(author)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"checksumSha256\":\"" + CHECKSUM + "\"}"))
        .andExpect(status().isConflict());
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject(
                "SELECT status FROM upload_asset WHERE id = ?",
                String.class,
                UUID.fromString(expiredAssetId)))
        .isEqualTo("UPLOADING");
    org.assertj.core.api.Assertions.assertThat(
            media.cleanupExpiredUploads(Instant.now()).deletedCount())
        .isEqualTo(1);
    org.assertj.core.api.Assertions.assertThat(
            media.cleanupExpiredUploads(Instant.now()).deletedCount())
        .isZero();
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM upload_asset WHERE id = ?",
                Integer.class,
                UUID.fromString(expiredAssetId)))
        .isZero();
  }

  @Test
  void moderatorCanAuditThenDeleteExpiredUploads() throws Exception {
    Cookie author = login("demo-member-1@townpet.local");
    String assetId = createUpload(author);
    String key = objectKey(assetId);
    storage.put(key, "image/jpeg", JPEG_BYTES);
    jdbc.update(
        "UPDATE upload_asset SET expires_at = CURRENT_TIMESTAMP - INTERVAL '1 minute' WHERE id = ?",
        UUID.fromString(assetId));

    jdbc.update(
        "UPDATE identity_credential SET password_hash = (SELECT password_hash FROM"
            + " identity_credential WHERE email = ?) WHERE email = ?",
        "demo-member-1@townpet.local",
        "demo-moderator@townpet.local");
    Cookie moderator = login("demo-moderator@townpet.local");
    moderator = MfaTestSupport.completeEnrollment(mockMvc, moderator);
    mockMvc
        .perform(
            post("/api/v1/operations/media/uploads/cleanup")
                .param("dryRun", "true")
                .cookie(moderator)
                .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.candidateCount").value(1))
        .andExpect(jsonPath("$.deletedCount").value(0));
    org.assertj.core.api.Assertions.assertThat(storage.inspect(key)).isPresent();

    mockMvc
        .perform(
            post("/api/v1/operations/media/uploads/cleanup")
                .param("dryRun", "false")
                .cookie(moderator)
                .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.candidateCount").value(1))
        .andExpect(jsonPath("$.deletedCount").value(1));
    org.assertj.core.api.Assertions.assertThat(storage.inspect(key)).isEmpty();
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
                            + "\",\"contentType\":\"image/jpeg\",\"byteSize\":"
                            + JPEG_BYTES.length
                            + "}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UPLOADING"))
            .andReturn();
    return new com.fasterxml.jackson.databind.ObjectMapper()
        .readTree(result.getResponse().getContentAsString())
        .path("id")
        .asText();
  }

  private static byte[] createJpegBytes() {
    try {
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      ImageIO.write(new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB), "jpg", output);
      return output.toByteArray();
    } catch (IOException exception) {
      throw new ExceptionInInitializerError(exception);
    }
  }

  private static String checksum(byte[] content) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
    } catch (java.security.NoSuchAlgorithmException exception) {
      throw new ExceptionInInitializerError(exception);
    }
  }

  private String objectKey(String assetId) {
    return Objects.requireNonNull(
        jdbc.queryForObject(
            "SELECT object_key FROM upload_asset WHERE id = ?",
            String.class,
            UUID.fromString(assetId)));
  }

  private String createPublication(Cookie session) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/publications")
                    .cookie(session)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"title\":\"media\",\"body\":\"body\"}"))
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
