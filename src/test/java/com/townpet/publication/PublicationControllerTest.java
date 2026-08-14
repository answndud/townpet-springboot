package com.townpet.publication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
    // V021/V022 contain public catalog demo rows; isolate publication feed
    // assertions from those rows because /api/v1/feed is now an aggregate feed.
    jdbc.update("DELETE FROM content_animal_community");
    jdbc.update("DELETE FROM gathering_participant");
    jdbc.update("DELETE FROM gathering");
    jdbc.update("DELETE FROM market_listing");
    jdbc.update("DELETE FROM local_resource");
    jdbc.update("DELETE FROM relationship_block");
    jdbc.update("DELETE FROM engagement_reaction");
    jdbc.update("DELETE FROM publication");
    jdbc.update("DELETE FROM member_profile WHERE member_id = ?", MEMBER_ID);
    jdbc.update(
        "INSERT INTO member_profile (member_id, bio, neighborhood_id, updated_at) "
            + "VALUES (?, '', ?, CURRENT_TIMESTAMP)",
        MEMBER_ID,
        NEIGHBORHOOD_ID);
  }

  @Test
  void legacyPostsApiUsesTheSamePublicationLifecycle() throws Exception {
    Cookie author = login();
    MvcResult created =
        mockMvc
            .perform(
                post("/api/posts")
                    .cookie(author)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"title\":\"레거시 제목\",\"body\":\"레거시 본문\"}"))
            .andExpect(status().isCreated())
            .andReturn();
    String publicationId =
        new com.fasterxml.jackson.databind.ObjectMapper()
            .readTree(created.getResponse().getContentAsString())
            .path("id")
            .asText();

    mockMvc
        .perform(get("/api/posts/{id}", publicationId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("레거시 제목"));
    mockMvc
        .perform(
            patch("/api/posts/{id}", publicationId)
                .cookie(author)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"title\":\"변경 제목\",\"body\":\"변경 본문\",\"version\":0}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("변경 제목"));
  }

  @Test
  void memberCreatesPublicationAndGuestReadsDirectDetail() throws Exception {
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
                          "body": "  저녁 산책 정보를 나눠요.  "
                        }
                        """))
            .andExpect(status().isCreated())
            .andExpect(
                header()
                    .string("Location", org.hamcrest.Matchers.startsWith("/api/v1/publications/")))
            .andExpect(jsonPath("$.type").value("FREE_BOARD"))
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
  void animalCommunityFeedKeepsAnimalAndBoardBoundaries() throws Exception {
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
                          "title": "강아지 질문",
                          "body": "강아지 산책 질문입니다.",
                          "type": "QA_QUESTION",
                          "animalCommunityCodes": ["DOG", "CAT"]
                        }
                        """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.animalCommunityCodes[0]").value("DOG"))
            .andExpect(jsonPath("$.animalCommunityCodes[1]").value("CAT"))
            .andReturn();
    String publicationId =
        new com.fasterxml.jackson.databind.ObjectMapper()
            .readTree(created.getResponse().getContentAsString())
            .path("id")
            .asText();

    mockMvc
        .perform(
            put("/api/v1/publications/{publicationId}", publicationId)
                .cookie(session)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "title": "강아지 질문 수정",
                      "body": "기존 다중 동물 태그를 유지합니다.",
                      "version": 0
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.animalCommunityCodes[0]").value("DOG"))
        .andExpect(jsonPath("$.animalCommunityCodes[1]").value("CAT"));

    mockMvc
        .perform(get("/api/v1/communities/dog/feed?board=questions"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.animalCode").value("dog"))
        .andExpect(jsonPath("$.board").value("questions"))
        .andExpect(jsonPath("$.items[0].title").value("강아지 질문 수정"))
        .andExpect(jsonPath("$.items[0].animalCode").value("DOG"));

    mockMvc
        .perform(get("/api/v1/communities/cat/feed?board=questions"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].title").value("강아지 질문 수정"))
        .andExpect(jsonPath("$.items[0].animalCode").value("CAT"));

    mockMvc
        .perform(
            post("/api/v1/publications")
                .cookie(session)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "title": "잘못된 태그",
                      "body": "알 수 없는 동물 코드는 거부합니다.",
                      "animalCommunityCodes": ["NOT_AN_ANIMAL"]
                    }
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
  }

  @Test
  void commonBoardFeedIsSharedAndNotNestedUnderAnimalBoards() throws Exception {
    Cookie session = login();
    mockMvc
        .perform(
            post("/api/v1/marketplace/listings")
                .cookie(session)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "kind": "SELL",
                      "title": "공통 거래 게시판 글",
                      "description": "모든 동물 가족이 볼 수 있는 거래 글입니다.",
                      "priceKrw": 10000
                    }
                    """))
        .andExpect(status().isCreated());

    mockMvc
        .perform(get("/api/v1/boards/marketplace/feed"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].title").value("공통 거래 게시판 글"))
        .andExpect(jsonPath("$.items[0].kind").value("MARKETPLACE"));

    mockMvc
        .perform(get("/api/v1/boards/all/feed"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].title").value("공통 거래 게시판 글"));

    mockMvc
        .perform(get("/api/v1/communities/dog/feed").queryParam("board", "marketplace"))
        .andExpect(status().isNotFound());

    mockMvc
        .perform(get("/api/v1/communities/dog/feed"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items").isEmpty());
  }

  @Test
  void creationRequiresAuthenticationAndValidInput() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/publications")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"동네 산책 모임\",\"body\":\"주말 아침에 만나요.\"}"))
        .andExpect(status().isUnauthorized());

    Cookie session = login();
    mockMvc
        .perform(
            post("/api/v1/publications")
                .cookie(session)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"   \",\"body\":\"내용\"}"))
        .andExpect(status().isBadRequest());

    mockMvc
        .perform(
            post("/api/v1/publications")
                .cookie(session)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"동네 산책 모임\",\"body\":\"주말 아침에 만나요.\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.title").value("동네 산책 모임"));
  }

  @Test
  void feedIsPublicAndUsesStableCursor() throws Exception {
    UUID deletedId = UUID.fromString("00000000-0000-4000-8000-000000000305");
    UUID globalNewId = UUID.fromString("00000000-0000-4000-8000-000000000304");
    UUID localOwnedId = UUID.fromString("00000000-0000-4000-8000-000000000303");
    UUID localOtherId = UUID.fromString("00000000-0000-4000-8000-000000000302");
    UUID globalOldId = UUID.fromString("00000000-0000-4000-8000-000000000301");
    insertPublication(deletedId, "삭제된 글", "DELETED", "2026-08-10T10:05:00Z");
    insertPublication(globalNewId, "새 공개 글", "ACTIVE", "2026-08-10T10:04:00Z");
    insertPublication(localOwnedId, "두 번째 공개 글", "ACTIVE", "2026-08-10T10:03:00Z");
    jdbc.update(
        "INSERT INTO content_animal_community (content_kind, content_id, animal_code) VALUES ('PUBLICATION', ?, 'DOG')",
        localOwnedId);
    insertPublication(localOtherId, "세 번째 공개 글", "ACTIVE", "2026-08-10T10:02:00Z");
    insertPublication(globalOldId, "이전 공개 글", "ACTIVE", "2026-08-10T10:01:00Z");

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
        .andExpect(jsonPath("$.items[0].id").value(localOwnedId.toString()))
        .andExpect(jsonPath("$.page.hasNext").value(true));

    mockMvc
        .perform(get("/api/v1/feed").cookie(login()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()").value(4))
        .andExpect(jsonPath("$.items[0].id").value(globalNewId.toString()))
        .andExpect(jsonPath("$.items[1].id").value(localOwnedId.toString()))
        .andExpect(jsonPath("$.items[2].id").value(localOtherId.toString()))
        .andExpect(jsonPath("$.items[3].id").value(globalOldId.toString()));

    mockMvc
        .perform(get("/api/v1/feed").cookie(login()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()").value(4))
        .andExpect(jsonPath("$.items[0].id").value(globalNewId.toString()))
        .andExpect(jsonPath("$.items[1].id").value(localOwnedId.toString()));

    mockMvc
        .perform(get("/api/v1/communities/dog/feed").cookie(login()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].id").value(localOwnedId.toString()));

    mockMvc
        .perform(get("/api/v1/feed").queryParam("cursor", "not-a-cursor"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void feedKeepsGeneralPostsAndFiltersAnimalSpecificPosts() throws Exception {
    UUID generalId = UUID.fromString("00000000-0000-4000-8000-000000000321");
    UUID dogId = UUID.fromString("00000000-0000-4000-8000-000000000322");
    UUID catId = UUID.fromString("00000000-0000-4000-8000-000000000323");
    insertPublication(generalId, "일반 글", "ACTIVE", "2026-08-10T10:03:00Z");
    insertPublicationWithAnimal(dogId, "강아지 산책", "DOG", "2026-08-10T10:02:00Z");
    insertPublicationWithAnimal(catId, "고양이 놀이", "CAT", "2026-08-10T10:01:00Z");

    mockMvc
        .perform(get("/api/v1/feed").queryParam("animals", "DOG"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()").value(2))
        .andExpect(jsonPath("$.items[0].id").value(generalId.toString()))
        .andExpect(jsonPath("$.items[1].animalInterestCode").value("DOG"));

    mockMvc
        .perform(get("/api/v1/feed").queryParam("animals", "NOPE"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()").value(1))
        .andExpect(jsonPath("$.items[0].animalInterestCode").doesNotExist());
  }

  @Test
  void publicFeedProjectsCommunityBoardItemsWithTheirDetailLinks() throws Exception {
    OffsetDateTime now = OffsetDateTime.parse("2026-08-10T12:00:00Z");
    jdbc.update(
        "INSERT INTO market_listing (id, owner_member_id, kind, status, title, description, price_krw, created_at, updated_at, version) "
            + "VALUES (?, ?, 'SELL', 'AVAILABLE', '이동장 판매', '깨끗하게 사용한 이동장입니다.', 20000, ?, ?, 0)",
        UUID.fromString("00000000-0000-4000-8000-000000000331"),
        MEMBER_ID,
        now,
        now);
    jdbc.update(
        "INSERT INTO adoption_listing (id, publisher_member_id, neighborhood_id, title, description, species, breed, status, created_at, updated_at, version) "
            + "VALUES (?, ?, ?, '믹스견 입양', '신중한 상담 후 입양을 진행합니다.', 'DOG', '믹스', 'OPEN', ?, ?, 0)",
        UUID.fromString("00000000-0000-4000-8000-000000000332"),
        MEMBER_ID,
        NEIGHBORHOOD_ID,
        now.minusMinutes(1),
        now.minusMinutes(1));
    jdbc.update(
        "INSERT INTO lost_found_alert (id, reporter_member_id, kind, status, title, description, last_seen_at, approx_location, created_at, updated_at, version) "
            + "VALUES (?, ?, 'LOST', 'ACTIVE', '강아지를 찾습니다', '발견하면 안전하게 제보해 주세요.', ?, ST_SetSRID(ST_MakePoint(126.9, 37.55), 4326)::geography, ?, ?, 0)",
        UUID.fromString("00000000-0000-4000-8000-000000000333"),
        MEMBER_ID,
        now,
        now.minusMinutes(2),
        now.minusMinutes(2));
    jdbc.update(
        "INSERT INTO hospital_review (id, author_member_id, hospital_name, address, rating, body, created_at, updated_at, version) "
            + "VALUES (?, ?, 'TownPet 동물병원', '서울 마포구', 5, '설명이 친절했습니다.', ?, ?, 0)",
        UUID.fromString("00000000-0000-4000-8000-000000000334"),
        MEMBER_ID,
        now.minusMinutes(3),
        now.minusMinutes(3));
    jdbc.update(
        "INSERT INTO gathering (id, host_member_id, title, description, location, starts_at, capacity, status, created_at, version) "
            + "VALUES (?, ?, '저녁 산책 모임', '천천히 함께 걸어요.', '망원나들목', ?, 8, 'ACTIVE', ?, 0)",
        UUID.fromString("00000000-0000-4000-8000-000000000335"),
        MEMBER_ID,
        now.plusDays(1),
        now.minusMinutes(4));
    jdbc.update(
        "INSERT INTO care_request (id, requester_member_id, title, description, location, starts_at, ends_at, reward_hint, status, created_at, updated_at, version) "
            + "VALUES (?, ?, '주말 돌봄 요청', '사료와 물을 확인해 주세요.', '망원동', ?, ?, NULL, 'OPEN', ?, ?, 0)",
        UUID.fromString("00000000-0000-4000-8000-000000000336"),
        MEMBER_ID,
        now.plusDays(2),
        now.plusDays(2).plusHours(2),
        now.minusMinutes(5),
        now.minusMinutes(5));
    jdbc.update(
        "INSERT INTO volunteer_opportunity (id, publisher_member_id, title, description, organization, location, starts_at, capacity, status, created_at, updated_at, version) "
            + "VALUES (?, ?, '보호소 산책 봉사', '동물 산책을 도와주세요.', 'TownPet 보호소', '마포구', ?, 10, 'OPEN', ?, ?, 0)",
        UUID.fromString("00000000-0000-4000-8000-000000000337"),
        MEMBER_ID,
        now.plusDays(3),
        now.minusMinutes(6),
        now.minusMinutes(6));
    jdbc.update(
        "INSERT INTO local_resource (id, kind, title, summary, content, source_name, source_url, updated_at) "
            + "VALUES (?, 'LOCAL_GUIDE', '산책 가이드', '동네 산책 팁입니다.', '자세한 안내입니다.', 'TownPet 운영팀', NULL, ?)",
        UUID.fromString("00000000-0000-4000-8000-000000000338"),
        now.minusMinutes(7));

    mockMvc
        .perform(get("/api/v1/feed").queryParam("limit", "20"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()").value(8))
        .andExpect(
            jsonPath("$.items[*].kind")
                .value(
                    org.hamcrest.Matchers.containsInAnyOrder(
                        "MARKETPLACE",
                        "ADOPTION",
                        "LOST_FOUND",
                        "HOSPITAL_REVIEW",
                        "GATHERING",
                        "CARE_REQUEST",
                        "VOLUNTEER",
                        "RESOURCE")))
        .andExpect(
            jsonPath("$.items[?(@.kind == 'MARKETPLACE')].href")
                .value(
                    org.hamcrest.Matchers.contains(
                        "/marketplace/00000000-0000-4000-8000-000000000331")))
        .andExpect(
            jsonPath("$.items[?(@.kind == 'ADOPTION')].animalInterestCode")
                .value(org.hamcrest.Matchers.contains("DOG")));
  }

  @Test
  void popularFeedRanksOnlyActivePostsByRecommendationCount() throws Exception {
    UUID mostRecommendedId = UUID.fromString("00000000-0000-4000-8000-000000000307");
    UUID lessRecommendedId = UUID.fromString("00000000-0000-4000-8000-000000000308");
    UUID noRecommendationId = UUID.fromString("00000000-0000-4000-8000-000000000309");
    UUID localId = UUID.fromString("00000000-0000-4000-8000-000000000310");
    UUID deletedId = UUID.fromString("00000000-0000-4000-8000-000000000311");
    insertPublication(mostRecommendedId, "추천이 가장 많은 글", "ACTIVE", "2026-08-10T10:10:00Z");
    insertPublication(lessRecommendedId, "추천이 적은 글", "ACTIVE", "2026-08-10T10:11:00Z");
    insertPublication(noRecommendationId, "추천이 없는 글", "ACTIVE", "2026-08-10T10:12:00Z");
    insertPublication(localId, "추천 공개 글", "ACTIVE", "2026-08-10T10:13:00Z");
    insertPublication(deletedId, "삭제된 추천 글", "DELETED", "2026-08-10T10:14:00Z");

    jdbc.update(
        "INSERT INTO engagement_reaction (id, publication_id, author_member_id, type, created_at) "
            + "VALUES (?, ?, ?, 'LIKE', CURRENT_TIMESTAMP), (?, ?, ?, 'LIKE', CURRENT_TIMESTAMP), "
            + "(?, ?, ?, 'LIKE', CURRENT_TIMESTAMP)",
        UUID.fromString("00000000-0000-4000-8000-000000000407"),
        mostRecommendedId,
        MEMBER_ID,
        UUID.fromString("00000000-0000-4000-8000-000000000408"),
        mostRecommendedId,
        UUID.fromString("00000000-0000-4000-8000-000000000202"),
        UUID.fromString("00000000-0000-4000-8000-000000000409"),
        lessRecommendedId,
        MEMBER_ID);

    mockMvc
        .perform(get("/api/v1/feed/popular"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()").value(2))
        .andExpect(jsonPath("$.items[0].id").value(mostRecommendedId.toString()))
        .andExpect(jsonPath("$.items[0].recommendationCount").value(2))
        .andExpect(jsonPath("$.items[0].rank").value(1))
        .andExpect(jsonPath("$.page.totalPages").value(1))
        .andExpect(jsonPath("$.items[0].viewCount").doesNotExist())
        .andExpect(jsonPath("$.items[1].id").value(lessRecommendedId.toString()))
        .andExpect(jsonPath("$.items[1].recommendationCount").value(1))
        .andExpect(jsonPath("$.items[1].rank").value(2));

    MvcResult firstPopularPage =
        mockMvc
            .perform(get("/api/v1/feed/popular").queryParam("limit", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items.length()").value(1))
            .andExpect(jsonPath("$.items[0].id").value(mostRecommendedId.toString()))
            .andExpect(jsonPath("$.page.hasNext").value(true))
            .andExpect(jsonPath("$.page.totalPages").value(2))
            .andExpect(jsonPath("$.page.nextCursor").isNotEmpty())
            .andReturn();
    String popularCursor =
        new com.fasterxml.jackson.databind.ObjectMapper()
            .readTree(firstPopularPage.getResponse().getContentAsString())
            .path("page")
            .path("nextCursor")
            .asText();

    mockMvc
        .perform(
            get("/api/v1/feed/popular")
                .queryParam("limit", "1")
                .queryParam("cursor", popularCursor))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()").value(1))
        .andExpect(jsonPath("$.items[0].id").value(lessRecommendedId.toString()))
        .andExpect(jsonPath("$.page.hasNext").value(false));
  }

  @Test
  void blockHidesAuthorFromViewerFeedAndDetailButNotGuestReads() throws Exception {
    UUID publicationId = UUID.fromString("00000000-0000-4000-8000-000000000306");
    insertPublication(publicationId, "차단 작성자 글", "ACTIVE", "2026-08-10T10:06:00Z");
    UUID viewerId = UUID.fromString("00000000-0000-4000-8000-000000000202");
    jdbc.update(
        "INSERT INTO relationship_block (id, blocker_member_id, blocked_member_id, created_at) "
            + "VALUES (?, ?, ?, CURRENT_TIMESTAMP)",
        UUID.fromString("00000000-0000-4000-8000-000000000406"),
        viewerId,
        MEMBER_ID);

    Cookie viewer = login("demo-member-2@townpet.local");
    mockMvc
        .perform(get("/api/v1/feed").cookie(viewer))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()").value(0));
    mockMvc
        .perform(get("/api/v1/feed"))
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
                          "body": "수정 전 본문"
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
                    {"title":"가로챈 제목","body":"가로챈 본문","version":0}
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
                    {"title":"수정한 제목","body":"수정한 본문","version":0}
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
                    {"title":"오래된 수정","body":"덮어쓰면 안 됨","version":0}
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
        .perform(get("/api/v1/feed"))
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

  private void insertPublication(UUID id, String title, String lifecycle, String createdAt) {
    OffsetDateTime timestamp = OffsetDateTime.parse(createdAt);
    jdbc.update(
        "INSERT INTO publication (id, author_member_id, type, title, body, "
            + "lifecycle, created_at, updated_at, version) VALUES (?, ?, 'FREE_BOARD', ?, ?, ?, ?, ?, 0)",
        id,
        MEMBER_ID,
        title,
        title + " 본문",
        lifecycle,
        timestamp,
        timestamp);
  }

  private void insertPublicationWithAnimal(
      UUID id, String title, String animalInterestCode, String createdAt) {
    OffsetDateTime timestamp = OffsetDateTime.parse(createdAt);
    jdbc.update(
        "INSERT INTO publication (id, author_member_id, type, animal_interest_code, title, body, "
            + "lifecycle, created_at, updated_at, version) VALUES (?, ?, 'FREE_BOARD', ?, ?, ?, 'ACTIVE', ?, ?, 0)",
        id,
        MEMBER_ID,
        animalInterestCode,
        title,
        title + " 본문",
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
