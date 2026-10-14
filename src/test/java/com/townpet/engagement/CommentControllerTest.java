package com.townpet.engagement;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import com.townpet.identity.MfaTestSupport;
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
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
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
class CommentControllerTest {
  private static final UUID MEMBER_ID = UUID.fromString("00000000-0000-4000-8000-000000000201");
  private static final UUID NEIGHBORHOOD_ID =
      UUID.fromString("00000000-0000-4000-8000-000000000101");

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
    registry.add("townpet.engagement.guest-comments.enabled", () -> true);
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
  }

  @Autowired MockMvc mockMvc;
  @Autowired JdbcTemplate jdbc;

  @BeforeEach
  void resetState() {
    jdbc.update("DELETE FROM relationship_block");
    jdbc.update("DELETE FROM engagement_comment");
    jdbc.update("DELETE FROM publication");
    jdbc.update("DELETE FROM member_profile WHERE member_id = ?", MEMBER_ID);
    jdbc.update(
        "INSERT INTO member_profile (member_id, bio, neighborhood_id, updated_at) "
            + "VALUES (?, '', ?, CURRENT_TIMESTAMP)",
        MEMBER_ID,
        NEIGHBORHOOD_ID);
  }

  @Test
  void membersCreateStableCommentsAndOnlyAuthorsCanDelete() throws Exception {
    String publicationId = createPublication(login("demo-member-1@townpet.local"));
    CookiePair actors =
        new CookiePair(login("demo-member-1@townpet.local"), login("demo-member-2@townpet.local"));

    mockMvc
        .perform(get("/api/v1/publications/{publicationId}/comments", publicationId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()").value(0));
    mockMvc
        .perform(
            post("/api/v1/publications/{publicationId}/comments", publicationId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"body\":\"비회원 댓글은 막혀야 합니다.\"}"))
        .andExpect(status().isUnauthorized());

    String firstCommentId = createComment(publicationId, actors.author(), "첫 번째 댓글");
    String secondCommentId = createComment(publicationId, actors.other(), "두 번째 댓글");

    mockMvc
        .perform(get("/api/v1/publications/{publicationId}/comments", publicationId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()").value(2))
        .andExpect(jsonPath("$.items[0].id").value(firstCommentId))
        .andExpect(jsonPath("$.items[0].body").value("첫 번째 댓글"))
        .andExpect(jsonPath("$.items[0].authorType").value("MEMBER"))
        .andExpect(jsonPath("$.items[1].id").value(secondCommentId));

    mockMvc
        .perform(
            delete(
                    "/api/v1/publications/{publicationId}/comments/{commentId}",
                    publicationId,
                    firstCommentId)
                .cookie(actors.other())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\":0}"))
        .andExpect(status().isForbidden());

    jdbc.update(
        "UPDATE engagement_comment SET version = 1 WHERE id = ?", UUID.fromString(firstCommentId));
    mockMvc
        .perform(
            delete(
                    "/api/v1/publications/{publicationId}/comments/{commentId}",
                    publicationId,
                    firstCommentId)
                .cookie(actors.author())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\":0}"))
        .andExpect(status().isConflict());
    mockMvc
        .perform(
            delete(
                    "/api/v1/publications/{publicationId}/comments/{commentId}",
                    publicationId,
                    firstCommentId)
                .cookie(actors.author())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\":1}"))
        .andExpect(status().isNoContent());
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject(
                "SELECT lifecycle FROM engagement_comment WHERE id = ?",
                String.class,
                UUID.fromString(firstCommentId)))
        .isEqualTo("DELETED");
    mockMvc
        .perform(get("/api/v1/publications/{publicationId}/comments", publicationId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()").value(1))
        .andExpect(jsonPath("$.items[0].id").value(secondCommentId));

    String otherPublicationId = createPublication(actors.author());
    mockMvc
        .perform(
            delete(
                    "/api/v1/publications/{publicationId}/comments/{commentId}",
                    otherPublicationId,
                    secondCommentId)
                .cookie(actors.other())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\":0}"))
        .andExpect(status().isNotFound());

    mockMvc
        .perform(
            delete("/api/v1/publications/{publicationId}", publicationId)
                .cookie(actors.author())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\":0}"))
        .andExpect(status().isNoContent());
    mockMvc
        .perform(
            post("/api/v1/publications/{publicationId}/comments", publicationId)
                .cookie(actors.author())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"body\":\"삭제된 글에는 쓸 수 없습니다.\"}"))
        .andExpect(status().isNotFound());
    mockMvc
        .perform(get("/api/v1/publications/{publicationId}/comments", publicationId))
        .andExpect(status().isNotFound());
  }

  @Test
  void guestsCanCommentOnPublicationsAndRepliesAreLimitedToOneLevel() throws Exception {
    String publicationId = createPublication(login("demo-member-1@townpet.local"));
    MvcResult guestResult =
        mockMvc
            .perform(
                post("/api/guest/authors")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"password\":\"guest-password\"}"))
            .andExpect(status().isCreated())
            .andReturn();
    Cookie guestCookie =
        Objects.requireNonNull(guestResult.getResponse().getCookie("TOWNPET_GUEST_ID"));

    MvcResult rootResult =
        mockMvc
            .perform(
                post("/api/guest/posts/{publicationId}/comments", publicationId)
                    .cookie(guestCookie)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"password\":\"guest-password\",\"body\":\"비회원 댓글\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.authorType").value("GUEST"))
            .andReturn();
    String rootId =
        new com.fasterxml.jackson.databind.ObjectMapper()
            .readTree(rootResult.getResponse().getContentAsString())
            .path("id")
            .asText();

    MvcResult replyResult =
        mockMvc
            .perform(
                post("/api/guest/posts/{publicationId}/comments", publicationId)
                    .cookie(guestCookie)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"password\":\"guest-password\",\"body\":\"비회원 답글\","
                            + "\"parentCommentId\":\""
                            + rootId
                            + "\"}"))
            .andExpect(status().isCreated())
            .andReturn();
    String replyId =
        new com.fasterxml.jackson.databind.ObjectMapper()
            .readTree(replyResult.getResponse().getContentAsString())
            .path("id")
            .asText();

    mockMvc
        .perform(
            post("/api/guest/posts/{publicationId}/comments", publicationId)
                .cookie(guestCookie)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"password\":\"guest-password\",\"body\":\"깊은 답글\","
                        + "\"parentCommentId\":\""
                        + replyId
                        + "\"}"))
        .andExpect(status().isBadRequest());

    mockMvc
        .perform(get("/api/v1/publications/{publicationId}/comments", publicationId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()").value(2))
        .andExpect(jsonPath("$.items[0].authorType").value("GUEST"))
        .andExpect(jsonPath("$.items[0].authorId").isEmpty())
        .andExpect(jsonPath("$.items[0].guestOwnedByViewer").value(false));
    mockMvc
        .perform(get("/api/v1/publications/{publicationId}/comments", publicationId).cookie(guestCookie))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].guestOwnedByViewer").value(true));
    mockMvc
        .perform(
            delete("/api/guest/posts/{publicationId}/comments/{commentId}", publicationId, rootId)
                .cookie(guestCookie)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"password\":\"guest-password\",\"version\":0}"))
        .andExpect(status().isNoContent());
  }

  @Test
  void moderatorCanFindUnreportedGuestCommentsDeleteAndRestoreWithAudit() throws Exception {
    String publicationId = createPublication(login("demo-member-1@townpet.local"));
    MvcResult guestResult =
        mockMvc
            .perform(
                post("/api/guest/authors")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"password\":\"guest-password\"}"))
            .andExpect(status().isCreated())
            .andReturn();
    Cookie guestCookie =
        Objects.requireNonNull(guestResult.getResponse().getCookie("TOWNPET_GUEST_ID"));
    MvcResult commentResult =
        mockMvc
            .perform(
                post("/api/guest/posts/{publicationId}/comments", publicationId)
                    .cookie(guestCookie)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"password\":\"guest-password\",\"body\":\"신고하지 않은 댓글\"}"))
            .andExpect(status().isCreated())
            .andReturn();
    String commentId =
        new com.fasterxml.jackson.databind.ObjectMapper()
            .readTree(commentResult.getResponse().getContentAsString())
            .path("id")
            .asText();
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM trust_report WHERE target_type = 'COMMENT' AND target_id = ?",
                Integer.class,
                UUID.fromString(commentId)))
        .isZero();

    jdbc.update(
        "UPDATE identity_credential SET password_hash = (SELECT password_hash FROM"
            + " identity_credential WHERE email = ?) WHERE email = ?",
        "demo-member-1@townpet.local",
        "demo-moderator@townpet.local");
    Cookie moderator = MfaTestSupport.completeEnrollment(mockMvc, login("demo-moderator@townpet.local"));

    mockMvc
        .perform(get("/api/admin/comments").param("authorType", "GUEST").cookie(moderator))
        .andDo(MockMvcResultHandlers.print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(commentId))
        .andExpect(jsonPath("$[0].lifecycle").value("ACTIVE"));

    mockMvc
        .perform(
            post("/api/admin/comments/{commentId}/moderation", commentId)
                .cookie(moderator)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"action\":\"DELETE\",\"reason\":\"운영 기준 위반\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.lifecycle").value("MODERATOR_DELETED"));
    mockMvc
        .perform(get("/api/v1/publications/{publicationId}/comments", publicationId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()").value(0));

    mockMvc
        .perform(
            post("/api/admin/comments/{commentId}/moderation", commentId)
                .cookie(moderator)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"action\":\"RESTORE\",\"reason\":\"재검토 결과 복구\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.lifecycle").value("ACTIVE"));
    mockMvc
        .perform(get("/api/v1/publications/{publicationId}/comments", publicationId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()").value(1));
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForList(
                "SELECT action FROM moderation_action WHERE target_type = 'COMMENT' AND target_id = ? ORDER BY created_at",
                String.class,
                UUID.fromString(commentId)))
        .containsExactly("COMMENT_DELETED", "COMMENT_RESTORED");
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
                        "{\"title\":\"댓글 대상 글\",\"body\":\"댓글을 남겨 주세요.\"}"))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", startsWith("/api/v1/publications/")))
            .andReturn();
    return new com.fasterxml.jackson.databind.ObjectMapper()
        .readTree(result.getResponse().getContentAsString())
        .path("id")
        .asText();
  }

  private String createComment(String publicationId, Cookie session, String body) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/publications/{publicationId}/comments", publicationId)
                    .cookie(session)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"body\":\"" + body + "\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.body").value(body))
            .andExpect(jsonPath("$.version").value(0))
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

  private record CookiePair(Cookie author, Cookie other) {}
}
