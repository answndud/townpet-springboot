package com.townpet.welfare;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
class AdoptionControllerTest {
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
  void memberCreatesAdoptionListingAndPublicBoardReadsIt() throws Exception {
    Cookie member = login();
    MvcResult created =
        mockMvc
            .perform(
                post("/api/v1/adoptions")
                    .cookie(member)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"title\":\"Momo needs a home\",\"description\":\"Gentle and vaccinated\",\"species\":\"DOG\",\"breed\":\"Mixed\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("OPEN"))
            .andExpect(jsonPath("$.species").value("DOG"))
            .andReturn();
    String id =
        new com.fasterxml.jackson.databind.ObjectMapper()
            .readTree(created.getResponse().getContentAsString())
            .path("id")
            .asText();

    mockMvc
        .perform(get("/api/boards/adoption/posts"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(id))
        .andExpect(jsonPath("$[0].title").value("Momo needs a home"));
  }

  private Cookie login() throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/auth/sessions")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"email\":\"demo-member-1@townpet.local\",\"password\":\"townpet-demo-123!\"}"))
            .andExpect(status().isCreated())
            .andReturn();
    return Objects.requireNonNull(result.getResponse().getCookie("SESSION"));
  }
}
