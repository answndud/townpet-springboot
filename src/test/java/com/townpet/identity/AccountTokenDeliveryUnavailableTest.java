package com.townpet.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.townpet.member.MemberEntity;
import com.townpet.member.MemberRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:identity-delivery;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;INIT=CREATE DOMAIN IF NOT EXISTS CITEXT AS VARCHAR(320)",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.jpa.hibernate.ddl-auto=create-drop",
      "spring.flyway.enabled=false",
      "spring.session.jdbc.initialize-schema=always",
      "spring.modulith.events.jdbc.schema-initialization.enabled=true"
    })
@AutoConfigureMockMvc
@ActiveProfiles("delivery-unavailable")
class AccountTokenDeliveryUnavailableTest {
  private static final UUID MEMBER_ID = UUID.fromString("00000000-0000-4000-8000-000000000091");

  @Autowired MockMvc mockMvc;
  @Autowired MemberRepository members;
  @Autowired CredentialRepository credentials;
  @Autowired PasswordResetTokenRepository passwordResetTokens;
  @Autowired EmailVerificationTokenRepository emailVerificationTokens;
  @Autowired PasswordEncoder passwordEncoder;

  @BeforeEach
  void seedMember() {
    passwordResetTokens.deleteAll();
    emailVerificationTokens.deleteAll();
    credentials.deleteAll();
    members.deleteAll();
    members.save(new MemberEntity(MEMBER_ID, "delivery-user"));
    credentials.save(
        new CredentialEntity(
            MEMBER_ID, "delivery@example.com", passwordEncoder.encode("password123!")));
  }

  @Test
  void unavailableDeliveryRollsBackAccountTokens() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/password-resets")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"delivery@example.com\"}"))
        .andExpect(status().isServiceUnavailable());
    mockMvc
        .perform(
            post("/api/v1/auth/email-verifications")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"delivery@example.com\"}"))
        .andExpect(status().isServiceUnavailable());

    assertThat(passwordResetTokens.count()).isZero();
    assertThat(emailVerificationTokens.count()).isZero();
  }
}
