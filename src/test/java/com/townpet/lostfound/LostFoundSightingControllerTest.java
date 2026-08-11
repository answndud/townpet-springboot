package com.townpet.lostfound;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import java.util.Objects;
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
class LostFoundSightingControllerTest {
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

  @Test
  void activeAlertAcceptsSightingAndClosedAlertRejectsIt() throws Exception {
    Cookie member = login("demo-member-1@townpet.local");
    String alertId = createAlert(member);

    MvcResult sighting =
        mockMvc
            .perform(
                post("/api/v1/lost-found/alerts/{alertId}/sightings", alertId)
                    .cookie(login("demo-member-2@townpet.local"))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"seenAt\":\"2026-08-11T10:00:00Z\","
                            + "\"description\":\"Seen by the playground\","
                            + "\"latitude\":37.551,\"longitude\":126.912,"
                            + "\"exactLatitude\":37.5512,\"exactLongitude\":126.9123}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.alertId").value(alertId))
            .andExpect(jsonPath("$.approximateLocation.latitude").value(37.551))
            .andReturn();

    String sightingId =
        new com.fasterxml.jackson.databind.ObjectMapper()
            .readTree(sighting.getResponse().getContentAsString())
            .path("id")
            .asText();

    mockMvc
        .perform(
            get("/api/v1/lost-found/sightings/{sightingId}/exact-location", sightingId)
                .cookie(member))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.latitude").value(37.5512));

    mockMvc
        .perform(
            get("/api/v1/lost-found/sightings/{sightingId}/exact-location", sightingId)
                .cookie(login("demo-member-3@townpet.local")))
        .andExpect(status().isNotFound());

    mockMvc
        .perform(get("/api/v1/lost-found/alerts/{alertId}/sightings", alertId).param("limit", "5"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(sightingId))
        .andExpect(jsonPath("$[0].approximateLocation.latitude").value(37.551));
    mockMvc
        .perform(
            patch("/api/v1/lost-found/alerts/{alertId}/status", alertId)
                .cookie(member)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"CLOSED\",\"closeReason\":\"reunited\"}"))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            post("/api/v1/lost-found/alerts/{alertId}/sightings", alertId)
                .cookie(login("demo-member-3@townpet.local"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"seenAt\":\"2026-08-11T11:00:00Z\","
                        + "\"description\":\"Late sighting\","
                        + "\"latitude\":37.552,\"longitude\":126.913}"))
        .andExpect(status().isConflict());
  }

  private String createAlert(Cookie session) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/lost-found/alerts")
                    .cookie(session)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"kind\":\"LOST\",\"title\":\"Mango missing\","
                            + "\"description\":\"Near the park\","
                            + "\"lastSeenAt\":\"2026-08-11T09:00:00Z\","
                            + "\"latitude\":37.55,\"longitude\":126.91}"))
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
