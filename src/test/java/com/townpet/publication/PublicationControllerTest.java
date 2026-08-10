package com.townpet.publication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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

  private Cookie login() throws Exception {
    MvcResult login =
        mockMvc
            .perform(
                post("/api/v1/auth/sessions")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"email\":\"demo-member-1@townpet.local\","
                            + "\"password\":\"townpet-demo-123!\"}"))
            .andExpect(status().isCreated())
            .andReturn();
    return Objects.requireNonNull(login.getResponse().getCookie("SESSION"));
  }
}
