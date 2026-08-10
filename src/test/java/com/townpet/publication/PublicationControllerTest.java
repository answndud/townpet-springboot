package com.townpet.publication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.lang.Nullable;
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
class PublicationControllerTest {
  private static final UUID MEMBER_ID = UUID.fromString("00000000-0000-4000-8000-000000000201");
  private static final UUID NEIGHBORHOOD_ID =
      UUID.fromString("00000000-0000-4000-8000-000000000101");
  private static final UUID OTHER_NEIGHBORHOOD_ID =
      UUID.fromString("00000000-0000-4000-8000-000000000102");

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
  void resetPublicationState() {
    jdbc.update("DELETE FROM relationship_block");
    jdbc.update("DELETE FROM publication");
    jdbc.update("DELETE FROM member_profile WHERE member_id = ?", MEMBER_ID);
    jdbc.update(
        "INSERT INTO member_profile (member_id, bio, neighborhood_id, updated_at) "
            + "VALUES (?, '', ?, CURRENT_TIMESTAMP)",
        MEMBER_ID,
        NEIGHBORHOOD_ID);
  }

  @Test
  void memberCreatesGlobalPublicationAndGuestReadsDirectDetail() throws Exception {
    Cookie session = login();
    MvcResult created =
        mockMvc
            .perform(
                post("/api/v1/publications")
                    .cookie(session)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "title": "  함께 걷기 좋은 길  ",
                          "body": "  저녁 산책 정보를 나눠요.  ",
                          "scope": "GLOBAL"
                        }
                        """))
            .andExpect(status().isCreated())
            .andExpect(
                header()
                    .string("Location", org.hamcrest.Matchers.startsWith("/api/v1/publications/")))
            .andExpect(jsonPath("$.type").value("FREE_BOARD"))
            .andExpect(jsonPath("$.scope").value("GLOBAL"))
            .andExpect(jsonPath("$.lifecycle").value("ACTIVE"))
            .andExpect(jsonPath("$.title").value("함께 걷기 좋은 길"))
            .andExpect(jsonPath("$.body").value("저녁 산책 정보를 나눠요."))
            .andReturn();

    String id = Objects.requireNonNull(created.getResponse().getContentAsString());
    String publicationId =
        new com.fasterxml.jackson.databind.ObjectMapper().readTree(id).path("id").asText();
    assertThat(UUID.fromString(publicationId).version()).isEqualTo(7);
    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM publication", Integer.class)).isEqualTo(1);

    mockMvc
        .perform(get("/api/v1/publications/{publicationId}", publicationId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.authorId").value(MEMBER_ID.toString()))
        .andExpect(jsonPath("$.title").value("함께 걷기 좋은 길"));
  }

  @Test
  void creationRequiresAuthenticationValidInputAndOwnedLocalNeighborhood() throws Exception {
    String localRequest =
        """
        {
          "title": "동네 산책 모임",
          "body": "주말 아침에 만나요.",
          "scope": "LOCAL",
          "neighborhoodId": "%s"
        }
        """;

    mockMvc
        .perform(
            post("/api/v1/publications")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(localRequest.formatted(NEIGHBORHOOD_ID)))
        .andExpect(status().isUnauthorized());

    Cookie session = login();
    mockMvc
        .perform(
            post("/api/v1/publications")
                .cookie(session)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(localRequest.formatted(OTHER_NEIGHBORHOOD_ID)))
        .andExpect(status().isBadRequest());

    mockMvc
        .perform(
            post("/api/v1/publications")
                .cookie(session)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"   \",\"body\":\"내용\",\"scope\":\"GLOBAL\"}"))
        .andExpect(status().isBadRequest());

    mockMvc
        .perform(
            post("/api/v1/publications")
                .cookie(session)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(localRequest.formatted(NEIGHBORHOOD_ID)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.scope").value("LOCAL"))
        .andExpect(jsonPath("$.neighborhoodId").value(NEIGHBORHOOD_ID.toString()));
  }

  @Test
  void feedUsesViewerSafeScopeAndStableCursor() throws Exception {
    UUID deletedId = UUID.fromString("00000000-0000-4000-8000-000000000305");
    UUID globalNewId = UUID.fromString("00000000-0000-4000-8000-000000000304");
    UUID localOwnedId = UUID.fromString("00000000-0000-4000-8000-000000000303");
    UUID localOtherId = UUID.fromString("00000000-0000-4000-8000-000000000302");
    UUID globalOldId = UUID.fromString("00000000-0000-4000-8000-000000000301");
    insertPublication(deletedId, "삭제된 글", "GLOBAL", null, "DELETED", "2026-08-10T10:05:00Z");
    insertPublication(globalNewId, "새 전체 글", "GLOBAL", null, "ACTIVE", "2026-08-10T10:04:00Z");
    insertPublication(
        localOwnedId, "내 동네 글", "LOCAL", NEIGHBORHOOD_ID, "ACTIVE", "2026-08-10T10:03:00Z");
    insertPublication(
        localOtherId, "다른 동네 글", "LOCAL", OTHER_NEIGHBORHOOD_ID, "ACTIVE", "2026-08-10T10:02:00Z");
    insertPublication(globalOldId, "이전 전체 글", "GLOBAL", null, "ACTIVE", "2026-08-10T10:01:00Z");

    MvcResult firstPage =
        mockMvc
            .perform(get("/api/v1/feed").queryParam("limit", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items.length()").value(1))
            .andExpect(jsonPath("$.items[0].id").value(globalNewId.toString()))
            .andExpect(jsonPath("$.page.hasNext").value(true))
            .andExpect(jsonPath("$.page.nextCursor").isNotEmpty())
            .andReturn();
    String cursor =
        new com.fasterxml.jackson.databind.ObjectMapper()
            .readTree(firstPage.getResponse().getContentAsString())
            .path("page")
            .path("nextCursor")
            .asText();

    mockMvc
        .perform(get("/api/v1/feed").queryParam("limit", "1").queryParam("cursor", cursor))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()").value(1))
        .andExpect(jsonPath("$.items[0].id").value(globalOldId.toString()))
        .andExpect(jsonPath("$.page.hasNext").value(false))
        .andExpect(jsonPath("$.page.nextCursor").isEmpty());

    mockMvc
        .perform(get("/api/v1/feed").cookie(login()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()").value(3))
        .andExpect(jsonPath("$.items[0].id").value(globalNewId.toString()))
        .andExpect(jsonPath("$.items[1].id").value(localOwnedId.toString()))
        .andExpect(jsonPath("$.items[2].id").value(globalOldId.toString()));

    mockMvc
        .perform(get("/api/v1/feed").cookie(login()).queryParam("audience", "GLOBAL"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()").value(2))
        .andExpect(jsonPath("$.items[0].id").value(globalNewId.toString()))
        .andExpect(jsonPath("$.items[1].id").value(globalOldId.toString()));

    mockMvc
        .perform(get("/api/v1/feed").queryParam("cursor", "not-a-cursor"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void blockHidesAuthorFromViewerFeedAndDetailButNotGlobalGuestReads() throws Exception {
    UUID publicationId = UUID.fromString("00000000-0000-4000-8000-000000000306");
    insertPublication(publicationId, "차단 작성자 글", "GLOBAL", null, "ACTIVE", "2026-08-10T10:06:00Z");
    UUID viewerId = UUID.fromString("00000000-0000-4000-8000-000000000202");
    jdbc.update(
        "INSERT INTO relationship_block (id, blocker_member_id, blocked_member_id, created_at) "
            + "VALUES (?, ?, ?, CURRENT_TIMESTAMP)",
        UUID.fromString("00000000-0000-4000-8000-000000000406"),
        viewerId,
        MEMBER_ID);

    Cookie viewer = login("demo-member-2@townpet.local");
    mockMvc
        .perform(get("/api/v1/feed").cookie(viewer).queryParam("audience", "VIEWER"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()").value(0));
    mockMvc
        .perform(get("/api/v1/feed").cookie(viewer).queryParam("audience", "GLOBAL"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].id").value(publicationId.toString()));
    mockMvc
        .perform(get("/api/v1/publications/{publicationId}", publicationId).cookie(viewer))
        .andExpect(status().isNotFound());
    mockMvc
        .perform(get("/api/v1/publications/{publicationId}", publicationId))
        .andExpect(status().isOk());
  }

  @Test
  void authorEditsAndDeletesWhileOwnershipAndVersionAreEnforced() throws Exception {
    Cookie author = login("demo-member-1@townpet.local");
    Cookie otherMember = login("demo-member-2@townpet.local");
    MvcResult created =
        mockMvc
            .perform(
                post("/api/v1/publications")
                    .cookie(author)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "title": "수정 전 제목",
                          "body": "수정 전 본문",
                          "scope": "GLOBAL"
                        }
                        """))
            .andExpect(status().isCreated())
            .andReturn();
    String publicationId =
        new com.fasterxml.jackson.databind.ObjectMapper()
            .readTree(created.getResponse().getContentAsString())
            .path("id")
            .asText();

    mockMvc
        .perform(
            put("/api/v1/publications/{publicationId}", publicationId)
                .cookie(otherMember)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"title":"가로챈 제목","body":"가로챈 본문","scope":"GLOBAL","version":0}
                    """))
        .andExpect(status().isForbidden());

    mockMvc
        .perform(
            put("/api/v1/publications/{publicationId}", publicationId)
                .cookie(author)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"title":"수정한 제목","body":"수정한 본문","scope":"GLOBAL","version":0}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("수정한 제목"))
        .andExpect(jsonPath("$.body").value("수정한 본문"))
        .andExpect(jsonPath("$.version").value(1));

    mockMvc
        .perform(
            put("/api/v1/publications/{publicationId}", publicationId)
                .cookie(author)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"title":"오래된 수정","body":"덮어쓰면 안 됨","scope":"GLOBAL","version":0}
                    """))
        .andExpect(status().isConflict());

    mockMvc
        .perform(
            delete("/api/v1/publications/{publicationId}", publicationId)
                .cookie(otherMember)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\":1}"))
        .andExpect(status().isForbidden());

    mockMvc
        .perform(
            delete("/api/v1/publications/{publicationId}", publicationId)
                .cookie(author)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\":0}"))
        .andExpect(status().isConflict());

    mockMvc
        .perform(
            delete("/api/v1/publications/{publicationId}", publicationId)
                .cookie(author)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\":1}"))
        .andExpect(status().isNoContent())
        .andExpect(header().string("ETag", "\"2\""));

    mockMvc
        .perform(get("/api/v1/publications/{publicationId}", publicationId))
        .andExpect(status().isNotFound());
    mockMvc
        .perform(get("/api/v1/feed").queryParam("audience", "GLOBAL"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()").value(0));
    assertThat(
            jdbc.queryForObject(
                "SELECT lifecycle FROM publication WHERE id = ?",
                String.class,
                UUID.fromString(publicationId)))
        .isEqualTo("DELETED");

    mockMvc
        .perform(
            post("/api/v1/publications/{publicationId}/restore", publicationId)
                .cookie(otherMember)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\":2}"))
        .andExpect(status().isForbidden());
    mockMvc
        .perform(
            post("/api/v1/publications/{publicationId}/restore", publicationId)
                .cookie(author)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\":2}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.lifecycle").value("ACTIVE"))
        .andExpect(jsonPath("$.version").value(3));
    mockMvc
        .perform(get("/api/v1/publications/{publicationId}", publicationId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("수정한 제목"));
  }

  private void insertPublication(
      UUID id,
      String title,
      String scope,
      @Nullable UUID neighborhoodId,
      String lifecycle,
      String createdAt) {
    OffsetDateTime timestamp = OffsetDateTime.parse(createdAt);
    jdbc.update(
        "INSERT INTO publication (id, author_member_id, type, scope, neighborhood_id, title, body, "
            + "lifecycle, created_at, updated_at, version) VALUES (?, ?, 'FREE_BOARD', ?, ?, ?, ?, ?, ?, ?, 0)",
        id,
        MEMBER_ID,
        scope,
        neighborhoodId,
        title,
        title + " 본문",
        lifecycle,
        timestamp,
        timestamp);
  }

  private Cookie login() throws Exception {
    return login("demo-member-1@townpet.local");
  }

  private Cookie login(String email) throws Exception {
    MvcResult login =
        mockMvc
            .perform(
                post("/api/v1/auth/sessions")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"email\":\"" + email + "\"," + "\"password\":\"townpet-demo-123!\"}"))
            .andExpect(status().isCreated())
            .andReturn();
    return Objects.requireNonNull(login.getResponse().getCookie("SESSION"));
  }
}
