package com.townpet.identity;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.util.Objects;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** Test-only helper that exercises moderator MFA enrollment through HTTP. */
public final class MfaTestSupport {
  private MfaTestSupport() {}

  public static Cookie completeEnrollment(MockMvc mockMvc, Cookie session) throws Exception {
    String response =
        mockMvc
            .perform(post("/api/v1/auth/mfa/enrollment").cookie(session).with(csrf()))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String secret = new ObjectMapper().readTree(response).path("secret").asText();
    String code = new TotpService().generate(secret, Instant.now().getEpochSecond() / 30);
    mockMvc
        .perform(
            post("/api/v1/auth/mfa/enrollment/confirm")
                .cookie(session)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"" + code + "\"}"))
        .andExpect(status().isOk());
    return Objects.requireNonNull(session);
  }
}
