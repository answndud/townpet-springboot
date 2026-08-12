package com.townpet.care;

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
class CareControllerTest {
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
  void ownerAcceptsSingleApplicantAndCompletesCareWithFeedback() throws Exception {
    Cookie owner = login("demo-member-1@townpet.local");
    Cookie caregiver = login("demo-member-2@townpet.local");

    MvcResult created =
        mockMvc
            .perform(
                post("/api/v1/care/requests")
                    .cookie(owner)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"title\":\"산책 돌봄\",\"description\":\"저녁 산책\","
                            + "\"location\":\"마포구\",\"startsAt\":\"2030-01-01T10:00:00Z\","
                            + "\"endsAt\":\"2030-01-01T11:00:00Z\",\"rewardHint\":\"간식\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("OPEN"))
            .andReturn();
    String requestId =
        new com.fasterxml.jackson.databind.ObjectMapper()
            .readTree(created.getResponse().getContentAsString())
            .path("id")
            .asText();

    MvcResult applied =
        mockMvc
            .perform(
                post("/api/v1/care/requests/{requestId}/applications", requestId)
                    .cookie(caregiver)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"message\":\"도와드릴게요\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andReturn();
    String applicationId =
        new com.fasterxml.jackson.databind.ObjectMapper()
            .readTree(applied.getResponse().getContentAsString())
            .path("id")
            .asText();
    long applicationVersion =
        new com.fasterxml.jackson.databind.ObjectMapper()
            .readTree(applied.getResponse().getContentAsString())
            .path("version")
            .asLong();

    MvcResult assignment =
        mockMvc
            .perform(
                post(
                        "/api/v1/care/requests/{requestId}/applications/{applicationId}/accept",
                        requestId,
                        applicationId)
                    .cookie(owner)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"version\":" + applicationVersion + "}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("MATCHED"))
            .andReturn();
    String assignmentId =
        new com.fasterxml.jackson.databind.ObjectMapper()
            .readTree(assignment.getResponse().getContentAsString())
            .path("id")
            .asText();
    long assignmentVersion =
        new com.fasterxml.jackson.databind.ObjectMapper()
            .readTree(assignment.getResponse().getContentAsString())
            .path("version")
            .asLong();

    mockMvc
        .perform(get("/api/v1/care/requests/{id}", requestId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("MATCHED"));

    MvcResult inProgress =
        mockMvc
            .perform(
                patch("/api/v1/care/assignments/{id}/status", assignmentId)
                    .cookie(caregiver)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"status\":\"IN_PROGRESS\",\"version\":" + assignmentVersion + "}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
            .andReturn();
    long inProgressVersion =
        new com.fasterxml.jackson.databind.ObjectMapper()
            .readTree(inProgress.getResponse().getContentAsString())
            .path("version")
            .asLong();

    MvcResult completed =
        mockMvc
            .perform(
                patch("/api/v1/care/assignments/{id}/status", assignmentId)
                    .cookie(owner)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"status\":\"COMPLETED\",\"version\":" + inProgressVersion + "}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("COMPLETED"))
            .andReturn();
    long completedVersion =
        new com.fasterxml.jackson.databind.ObjectMapper()
            .readTree(completed.getResponse().getContentAsString())
            .path("version")
            .asLong();
    org.assertj.core.api.Assertions.assertThat(completedVersion).isGreaterThan(inProgressVersion);

    mockMvc
        .perform(
            post("/api/v1/care/assignments/{id}/feedback", assignmentId)
                .cookie(owner)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"body\":\"안전하게 완료했습니다\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.body").value("안전하게 완료했습니다"));
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
