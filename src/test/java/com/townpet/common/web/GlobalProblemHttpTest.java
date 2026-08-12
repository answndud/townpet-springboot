package com.townpet.common.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:problem-http;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;INIT=CREATE DOMAIN IF NOT EXISTS CITEXT AS VARCHAR(320)",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.jpa.hibernate.ddl-auto=create-drop",
      "spring.flyway.enabled=false",
      "spring.session.jdbc.initialize-schema=always",
      "spring.modulith.events.jdbc.schema-initialization.enabled=true"
    })
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GlobalProblemHttpTest {
  @Autowired MockMvc mockMvc;

  @Test
  void methodValidationUsesProblemContract() throws Exception {
    mockMvc
        .perform(get("/api/v1/feed").param("limit", "0"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
        .andExpect(jsonPath("$.traceId").isNotEmpty())
        .andExpect(header().exists(RequestTraceFilter.HEADER));
  }

  @Test
  void requestBodyValidationUsesProblemContract() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/sessions")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"title":"", "body":"", "scope":"GLOBAL"}
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
        .andExpect(jsonPath("$.traceId").isNotEmpty())
        .andExpect(jsonPath("$.fieldErrors").isArray())
        .andExpect(header().exists(RequestTraceFilter.HEADER));
  }

  @Test
  void missingPublicResourceUsesProblemContract() throws Exception {
    mockMvc
        .perform(get("/api/v1/catalog/neighborhoods/does-not-exist"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
        .andExpect(header().exists(RequestTraceFilter.HEADER));
  }

  @Test
  void securityFilterAuthenticationFailureUsesProblemContract() throws Exception {
    mockMvc
        .perform(get("/api/v1/members/me"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
        .andExpect(jsonPath("$.traceId").isNotEmpty())
        .andExpect(header().exists(RequestTraceFilter.HEADER));
  }

  @Test
  void securityFilterAuthorizationFailureUsesProblemContract() throws Exception {
    mockMvc
        .perform(get("/api/admin/policies").with(user("member").roles("MEMBER")))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("FORBIDDEN"))
        .andExpect(jsonPath("$.traceId").isNotEmpty())
        .andExpect(header().exists(RequestTraceFilter.HEADER));
  }
}
