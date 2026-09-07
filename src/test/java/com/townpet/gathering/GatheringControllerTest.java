package com.townpet.gathering;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Objects;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.UUID;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
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
class GatheringControllerTest {
  private static final UUID HOST_GATHERING_ID =
      UUID.fromString("0198f342-13d7-7000-8000-000000000401");

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

  @Test
  void anonymousJoinIsRejected() throws Exception {
    mockMvc.perform(post("/api/v1/gatherings/{id}/join", HOST_GATHERING_ID).with(csrf()))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void duplicateJoinIsIdempotent() throws Exception {
    Cookie member = login("demo-member-2@townpet.local");
    mockMvc.perform(post("/api/v1/gatherings/{id}/participants", HOST_GATHERING_ID).cookie(member).with(csrf()))
        .andExpect(status().isOk());
    mockMvc.perform(post("/api/v1/gatherings/{id}/participants", HOST_GATHERING_ID).cookie(member).with(csrf()))
        .andExpect(status().isOk());
    mockMvc.perform(get("/api/v1/gatherings/{id}", HOST_GATHERING_ID))
        .andExpect(jsonPath("$.participantCount").value(1));
  }

  @Test
  void concurrentJoinsNeverExceedCapacityOrDuplicateParticipant() throws Exception {
    int capacity = 3;
    UUID gatheringId = createGathering(capacity);
    List<UUID> members = seedConcurrencyMembers(10);
    ExecutorService executor = Executors.newFixedThreadPool(members.size());
    CountDownLatch start = new CountDownLatch(1);
    try {
      var futures = new java.util.ArrayList<Future<Integer>>();
      for (UUID member : members) {
        futures.add(executor.submit(() -> {
          start.await();
          return joinStatus(gatheringId, member);
        }));
      }
      start.countDown();
      List<Integer> statuses = new java.util.ArrayList<>();
      for (Future<Integer> future : futures) statuses.add(future.get());
      assertEquals(3, statuses.stream().filter(status -> status == 200).count());
      assertEquals(7, statuses.stream().filter(status -> status == 409).count());
      assertEquals(0, statuses.stream().filter(status -> status >= 500).count());
      MvcResult detail = mockMvc.perform(get("/api/v1/gatherings/{id}", gatheringId)).andExpect(status().isOk()).andReturn();
      String body = detail.getResponse().getContentAsString();
      int joinedCount = com.jayway.jsonpath.JsonPath.read(body, "$.participantCount");
      assertEquals(capacity, joinedCount);
      assertEquals(capacity, jdbc.queryForObject(
          "select count(*) from gathering_participant where gathering_id = ?", Integer.class, gatheringId));
      assertEquals(0, jdbc.queryForObject(
          "select count(*) - count(distinct member_id) from gathering_participant where gathering_id = ?",
          Integer.class, gatheringId));
    } finally {
      executor.shutdownNow();
    }
  }

  @Test
  void concurrentDuplicateJoinsForSameMemberCreateOneParticipant() throws Exception {
    UUID gatheringId = createGathering(10);
    UUID member = seedConcurrencyMembers(1).getFirst();
    ExecutorService executor = Executors.newFixedThreadPool(10);
    CountDownLatch start = new CountDownLatch(1);
    try {
      var futures = new java.util.ArrayList<Future<Integer>>();
      for (int i = 0; i < 10; i++) {
        futures.add(executor.submit(() -> {
          start.await();
          return joinStatus(gatheringId, member);
        }));
      }
      start.countDown();
      List<Integer> statuses = new java.util.ArrayList<>();
      for (Future<Integer> future : futures) statuses.add(future.get());
      assertTrue(statuses.stream().allMatch(status -> status == 200));
      assertEquals(1, jdbc.queryForObject(
          "select count(*) from gathering_participant where gathering_id = ? and member_id = ?",
          Integer.class, gatheringId, member));
    } finally {
      executor.shutdownNow();
    }
  }

  private int joinStatus(UUID gatheringId, UUID member) throws Exception {
    return mockMvc
        .perform(
            post("/api/v1/gatherings/{id}/participants", gatheringId)
                .with(user(member.toString()).roles("MEMBER"))
                .with(csrf()))
        .andReturn()
        .getResponse()
        .getStatus();
  }

  private List<UUID> seedConcurrencyMembers(int count) {
    List<UUID> members = new java.util.ArrayList<>();
    for (int i = 1; i <= count; i++) {
      UUID id = UUID.fromString("00000000-0000-4000-8100-000000000" + String.format("%03d", i));
      jdbc.update(
          "insert into member_account (id, email, nickname) values (?, ?, ?) on conflict (id) do nothing",
          id, "concurrency-" + i + "@townpet.local", "concurrency-" + i);
      members.add(id);
    }
    return members;
  }

  @Test
  void cancelledGatheringRejectsNewJoins() throws Exception {
    UUID id = createGathering(4);
    Cookie host = login("demo-member-1@townpet.local");
    mockMvc.perform(patch("/api/v1/gatherings/{id}/cancel", id).cookie(host).with(csrf())).andExpect(status().isOk());
    mockMvc.perform(post("/api/v1/gatherings/{id}/participants", id).cookie(login("demo-member-2@townpet.local")).with(csrf()))
        .andExpect(status().isConflict());
  }

  private UUID createGathering(int capacity) throws Exception {
    Cookie host = login("demo-member-1@townpet.local");
    MvcResult created =
        mockMvc
            .perform(
                post("/api/v1/gatherings")
                    .cookie(host)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"title\":\"동시성 테스트 모임\",\"description\":\"정원 경합 검증\",\"location\":\"온라인\","
                            + "\"startsAt\":\"2027-01-01T09:00:00Z\",\"capacity\":"
                            + capacity
                            + "}"))
            .andExpect(status().isCreated())
            .andReturn();
    return UUID.fromString(com.jayway.jsonpath.JsonPath.read(created.getResponse().getContentAsString(), "$.id"));
  }

  private Cookie login(String email) throws Exception {
    MvcResult login =
        mockMvc
            .perform(
                post("/api/v1/auth/sessions")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"" + email + "\",\"password\":\"townpet-demo-123!\"}"))
            .andExpect(status().isCreated())
            .andReturn();
    return Objects.requireNonNull(login.getResponse().getCookie("SESSION"));
  }
}
