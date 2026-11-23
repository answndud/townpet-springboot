package com.townpet.lostfound;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.townpet.support.PostgresTestContainer;
import jakarta.servlet.http.Cookie;
import java.util.Objects;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Tag("integration")
class LostFoundAlertControllerTest {
  @DynamicPropertySource
  static void databaseProperties(DynamicPropertyRegistry registry) {
    PostgresTestContainer.register(registry);
  }

  @Autowired MockMvc mockMvc;
  @Autowired JdbcTemplate jdbc;

  @Test
  void memberCreatesAlertAndAnonymousReaderSeesOnlyApproximateLocation() throws Exception {
    Cookie member = login("demo-member-1@townpet.local");
    MvcResult created =
        mockMvc
            .perform(
                post("/api/v1/lost-found/alerts")
                    .cookie(member)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"kind\":\"LOST\",\"title\":\"Mango missing\","
                            + "\"description\":\"Blue collar near the park\","
                            + "\"lastSeenAt\":\"2026-08-11T09:00:00Z\","
                            + "\"latitude\":37.55,\"longitude\":126.91}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("ACTIVE"))
            .andExpect(jsonPath("$.approximateLocation.latitude").value(37.55))
            .andReturn();
    String id =
        new com.fasterxml.jackson.databind.ObjectMapper()
            .readTree(created.getResponse().getContentAsString())
            .path("id")
            .asText();

    mockMvc
        .perform(get("/api/v1/lost-found/alerts/{id}", id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.kind").value("LOST"))
        .andExpect(jsonPath("$.approximateLocation.longitude").value(126.91));

    mockMvc
        .perform(get("/api/v1/lost-found/alerts").param("kind", "LOST").param("limit", "1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(id))
        .andExpect(jsonPath("$[0].status").value("ACTIVE"));

    mockMvc
        .perform(
            get("/api/v1/lost-found/alerts")
                .param("latitude", "37.55")
                .param("longitude", "126.91")
                .param("radiusMeters", "1000"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(id));
    mockMvc
        .perform(get("/api/v1/lost-found/alerts").param("latitude", "37.55"))
        .andExpect(status().isBadRequest());

    mockMvc
        .perform(
            patch("/api/v1/lost-found/alerts/{id}/status", id)
                .cookie(member)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"RESOLVED\",\"resolutionOutcome\":\"Reunited\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("RESOLVED"))
        .andExpect(jsonPath("$.resolutionOutcome").value("Reunited"));

    mockMvc
        .perform(
            patch("/api/v1/lost-found/alerts/{id}/status", id)
                .cookie(login("demo-member-2@townpet.local"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"CLOSED\",\"closeReason\":\"duplicate\"}"))
        .andExpect(status().isForbidden());

    mockMvc
        .perform(
            patch("/api/v1/lost-found/alerts/{id}/status", id)
                .cookie(member)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"ACTIVE\",\"reopenReason\":\"new sighting\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("ACTIVE"));
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM lost_found_alert_status_history WHERE alert_id = ?",
                Integer.class,
                UUID.fromString(id)))
        .isEqualTo(2);
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
