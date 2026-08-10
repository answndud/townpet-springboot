package com.townpet.identity;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.townpet.catalog.NeighborhoodEntity;
import com.townpet.catalog.NeighborhoodRepository;
import com.townpet.member.MemberEntity;
import com.townpet.member.MemberPetRepository;
import com.townpet.member.MemberRepository;
import java.util.Objects;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:identity;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.jpa.hibernate.ddl-auto=create-drop",
      "spring.flyway.enabled=false",
      "spring.session.jdbc.initialize-schema=always",
      "spring.modulith.events.jdbc.schema-initialization.enabled=true"
    })
@AutoConfigureMockMvc
class IdentityMemberControllerTest {
  private static final UUID MEMBER_ID = UUID.fromString("00000000-0000-4000-8000-000000000001");
  private static final UUID NEIGHBORHOOD_ID =
      UUID.fromString("00000000-0000-4000-8000-000000000101");

  @Autowired MockMvc mockMvc;
  @Autowired MemberRepository members;
  @Autowired CredentialRepository credentials;
  @Autowired NeighborhoodRepository neighborhoods;
  @Autowired MemberPetRepository pets;
  @Autowired PasswordEncoder passwordEncoder;

  @BeforeEach
  void seedMember() {
    credentials.deleteAll();
    pets.deleteAll();
    members.deleteAll();
    neighborhoods.deleteAll();
    neighborhoods.save(new NeighborhoodEntity(NEIGHBORHOOD_ID, "seoul-mapogu", "서울 마포구"));
    members.save(new MemberEntity(MEMBER_ID, "mango-user"));
    credentials.save(
        new CredentialEntity(
            MEMBER_ID, "mango@example.com", passwordEncoder.encode("password123!")));
  }

  @Test
  void loginPersistsSessionAndReturnsCurrentMember() throws Exception {
    MvcResult login =
        mockMvc
            .perform(
                post("/api/v1/auth/sessions")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"mango@example.com\",\"password\":\"password123!\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.memberId").value(MEMBER_ID.toString()))
            .andReturn();

    org.springframework.mock.web.MockHttpSession session =
        (org.springframework.mock.web.MockHttpSession)
            Objects.requireNonNull(login.getRequest().getSession(false));

    mockMvc
        .perform(get("/api/v1/members/me").session(session))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(MEMBER_ID.toString()))
        .andExpect(jsonPath("$.nickname").value("mango-user"));
  }

  @Test
  void onboardingReplacesOwnedPetsAndReturnsThem() throws Exception {
    MvcResult login =
        mockMvc
            .perform(
                post("/api/v1/auth/sessions")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"mango@example.com\",\"password\":\"password123!\"}"))
            .andExpect(status().isCreated())
            .andReturn();
    org.springframework.mock.web.MockHttpSession session =
        (org.springframework.mock.web.MockHttpSession)
            Objects.requireNonNull(login.getRequest().getSession(false));

    mockMvc
        .perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(
                    "/api/v1/members/me/onboarding")
                .session(session)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"bio\":\"산책을 좋아해요\",\"neighborhoodId\":\""
                        + NEIGHBORHOOD_ID
                        + "\",\"pets\":[{\"name\":\"Mango\",\"species\":\"DOG\"}]}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.pets[0].name").value("Mango"))
        .andExpect(jsonPath("$.pets[0].species").value("DOG"));

    mockMvc
        .perform(get("/api/v1/members/me").session(session))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.pets.length()").value(1));
  }

  @Test
  void stateChangingRequestWithoutCsrfIsRejected() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"mango@example.com\",\"password\":\"password123!\"}"))
        .andExpect(status().isForbidden());
  }

  @Test
  void logoutInvalidatesCurrentSession() throws Exception {
    MvcResult login =
        mockMvc
            .perform(
                post("/api/v1/auth/sessions")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"mango@example.com\",\"password\":\"password123!\"}"))
            .andExpect(status().isCreated())
            .andReturn();
    org.springframework.mock.web.MockHttpSession session =
        (org.springframework.mock.web.MockHttpSession)
            Objects.requireNonNull(login.getRequest().getSession(false));

    mockMvc
        .perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete(
                    "/api/v1/auth/sessions/current")
                .session(session)
                .with(csrf()))
        .andExpect(status().isNoContent());
    mockMvc
        .perform(get("/api/v1/members/me").session(session))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void unauthenticatedMemberAccessIsRejectedAndCatalogIsPublic() throws Exception {
    mockMvc.perform(get("/api/v1/members/me")).andExpect(status().isUnauthorized());
    mockMvc.perform(get("/api/v1/catalog/neighborhoods")).andExpect(status().isOk());
  }

  @Test
  void csrfTokenIsExposedOnlyAsNonHttpOnlyCookie() throws Exception {
    mockMvc
        .perform(get("/api/v1/auth/csrf"))
        .andExpect(status().isOk())
        .andExpect(cookie().exists("XSRF-TOKEN"));
  }
}
