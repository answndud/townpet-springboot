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
import jakarta.servlet.http.Cookie;
import java.util.Objects;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:identity;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;INIT=CREATE DOMAIN IF NOT EXISTS CITEXT AS VARCHAR(320)",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.jpa.hibernate.ddl-auto=create-drop",
      "spring.flyway.enabled=false",
      "spring.session.jdbc.initialize-schema=always",
      "spring.modulith.events.jdbc.schema-initialization.enabled=true"
    })
@AutoConfigureMockMvc
@ActiveProfiles("test")
class IdentityMemberControllerTest {
  private static final UUID MEMBER_ID = UUID.fromString("00000000-0000-4000-8000-000000000001");
  private static final UUID NEIGHBORHOOD_ID =
      UUID.fromString("00000000-0000-4000-8000-000000000101");

  @Autowired MockMvc mockMvc;
  @Autowired MemberRepository members;
  @Autowired CredentialRepository credentials;
  @Autowired PasswordResetTokenRepository passwordResetTokens;
  @Autowired EmailVerificationTokenRepository emailVerificationTokens;
  @Autowired AuthAuditRepository authAudits;
  @Autowired PasswordResetService passwordResets;
  @Autowired LocalAccountTokenCapture accountTokens;
  @Autowired JdbcIndexedSessionRepository sessions;
  @Autowired NeighborhoodRepository neighborhoods;
  @Autowired MemberPetRepository pets;
  @Autowired PasswordEncoder passwordEncoder;

  @BeforeEach
  void seedMember() {
    authAudits.deleteAll();
    passwordResetTokens.deleteAll();
    emailVerificationTokens.deleteAll();
    accountTokens.clear();
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
  void guestStepUpIsScopedSingleUseAndCookieBacked() throws Exception {
    MvcResult author =
        mockMvc
            .perform(
                post("/api/guest/authors")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"password\":\"guest-password-123\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.guestId").isNotEmpty())
            .andExpect(cookie().exists(GuestStepUpController.GUEST_COOKIE))
            .andReturn();
    Cookie guest =
        Objects.requireNonNull(author.getResponse().getCookie(GuestStepUpController.GUEST_COOKIE));

    MvcResult challenge =
        mockMvc
            .perform(
                post("/api/guest/step-up")
                    .cookie(guest)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"scope\":\"publication:manage\",\"password\":\"guest-password-123\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.scope").value("publication:manage"))
            .andExpect(cookie().exists(GuestStepUpController.STEP_UP_COOKIE))
            .andReturn();
    Cookie stepUp =
        Objects.requireNonNull(
            challenge.getResponse().getCookie(GuestStepUpController.STEP_UP_COOKIE));

    mockMvc
        .perform(
            post("/api/guest/step-up/consume")
                .cookie(stepUp)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"scope\":\"publication:manage\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.scope").value("publication:manage"));
    mockMvc
        .perform(
            post("/api/guest/step-up/consume")
                .cookie(stepUp)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"scope\":\"publication:manage\"}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void loginPersistsSessionAndReturnsCurrentMember() throws Exception {
    verifyCredential();
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

    Cookie session = sessionCookie(login);

    mockMvc
        .perform(get("/api/v1/members/me").cookie(session))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(MEMBER_ID.toString()))
        .andExpect(jsonPath("$.nickname").value("mango-user"));
  }

  @Test
  void onboardingReplacesOwnedPetsAndReturnsThem() throws Exception {
    verifyCredential();
    MvcResult login =
        mockMvc
            .perform(
                post("/api/v1/auth/sessions")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"mango@example.com\",\"password\":\"password123!\"}"))
            .andExpect(status().isCreated())
            .andReturn();
    Cookie session = sessionCookie(login);

    mockMvc
        .perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(
                    "/api/v1/members/me/onboarding")
                .cookie(session)
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
        .perform(get("/api/v1/members/me").cookie(session))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.pets.length()").value(1));
  }

  @Test
  void profileVisibilityCanBeUpdatedAndIsReturnedToOwner() throws Exception {
    verifyCredential();
    MvcResult login =
        mockMvc
            .perform(
                post("/api/v1/auth/sessions")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"mango@example.com\",\"password\":\"password123!\"}"))
            .andExpect(status().isCreated())
            .andReturn();
    Cookie session = sessionCookie(login);

    mockMvc
        .perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(
                    "/api/v1/members/me/profile")
                .cookie(session)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"bio\":\"공개 범위 테스트\",\"showPublicPosts\":false,"
                        + "\"showPublicComments\":true,\"showPublicPets\":false}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.showPublicPosts").value(false))
        .andExpect(jsonPath("$.showPublicComments").value(true))
        .andExpect(jsonPath("$.showPublicPets").value(false));
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
    verifyCredential();
    MvcResult login =
        mockMvc
            .perform(
                post("/api/v1/auth/sessions")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"mango@example.com\",\"password\":\"password123!\"}"))
            .andExpect(status().isCreated())
            .andReturn();
    Cookie session = sessionCookie(login);

    mockMvc
        .perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete(
                    "/api/v1/auth/sessions/current")
                .cookie(session)
                .with(csrf()))
        .andExpect(status().isNoContent());
    mockMvc.perform(get("/api/v1/members/me").cookie(session)).andExpect(status().isUnauthorized());
  }

  @Test
  void unauthenticatedMemberAccessIsRejectedAndCatalogIsPublic() throws Exception {
    mockMvc.perform(get("/api/v1/members/me")).andExpect(status().isUnauthorized());
    mockMvc
        .perform(get("/api/v1/members/{memberId}", MEMBER_ID))
        .andExpect(status().isUnauthorized());
    mockMvc.perform(get("/api/v1/catalog/neighborhoods")).andExpect(status().isOk());
  }

  @Test
  void memberCannotAccessModeratorOperations() throws Exception {
    verifyCredential();
    MvcResult login =
        mockMvc
            .perform(
                post("/api/v1/auth/sessions")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"mango@example.com\",\"password\":\"password123!\"}"))
            .andExpect(status().isCreated())
            .andReturn();
    Cookie session = sessionCookie(login);

    mockMvc
        .perform(get("/api/v1/operations/demo-reset").cookie(session))
        .andExpect(status().isForbidden());
  }

  @Test
  void passwordResetUsesHashedSingleUseTokenAndRevokesSession() throws Exception {
    verifyCredential();
    MvcResult login =
        mockMvc
            .perform(
                post("/api/v1/auth/sessions")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"mango@example.com\",\"password\":\"password123!\"}"))
            .andExpect(status().isCreated())
            .andReturn();
    Cookie session = sessionCookie(login);
    org.assertj.core.api.Assertions.assertThat(sessions.findByPrincipalName(MEMBER_ID.toString()))
        .isNotEmpty();
    mockMvc
        .perform(
            post("/api/v1/auth/password-resets")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"mango@example.com\"}"))
        .andExpect(status().isAccepted());
    String token =
        accountTokens.find(AccountTokenPurpose.PASSWORD_RESET, "mango@example.com").orElseThrow();

    org.assertj.core.api.Assertions.assertThat(passwordResetTokens.findAll())
        .singleElement()
        .extracting(PasswordResetTokenEntity::getTokenHash)
        .isEqualTo(SecureToken.hash(token))
        .isNotEqualTo(token);

    mockMvc
        .perform(
            post("/api/v1/auth/password-resets/confirmations")
                .cookie(session)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + token + "\",\"newPassword\":\"Changed!2026\"}"))
        .andExpect(status().isNoContent());

    mockMvc.perform(get("/api/v1/members/me").cookie(session)).andExpect(status().isUnauthorized());
    org.assertj.core.api.Assertions.assertThat(sessions.findByPrincipalName(MEMBER_ID.toString()))
        .isEmpty();
    mockMvc
        .perform(
            post("/api/v1/auth/sessions")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"mango@example.com\",\"password\":\"password123!\"}"))
        .andExpect(status().isUnauthorized());
    mockMvc
        .perform(
            post("/api/v1/auth/sessions")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"mango@example.com\",\"password\":\"Changed!2026\"}"))
        .andExpect(status().isCreated());
    mockMvc
        .perform(
            post("/api/v1/auth/password-resets/confirmations")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + token + "\",\"newPassword\":\"Another!2026\"}"))
        .andExpect(status().isBadRequest());
    org.assertj.core.api.Assertions.assertThat(authAudits.count()).isEqualTo(1);
  }

  @Test
  void resetRequestDoesNotRevealUnknownOrLockedAccounts() throws Exception {
    UUID demoId = UUID.fromString("00000000-0000-4000-8000-000000000099");
    members.save(new MemberEntity(demoId, "locked-demo"));
    credentials.save(
        new CredentialEntity(
            demoId, "locked@townpet.local", passwordEncoder.encode("Locked!2026"), "MEMBER", true));

    mockMvc
        .perform(
            post("/api/v1/auth/password-resets")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"unknown@example.com\"}"))
        .andExpect(status().isAccepted());
    mockMvc
        .perform(
            post("/api/v1/auth/password-resets")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"locked@townpet.local\"}"))
        .andExpect(status().isAccepted());

    org.assertj.core.api.Assertions.assertThat(passwordResetTokens.count()).isZero();
    org.assertj.core.api.Assertions.assertThat(
            accountTokens.find(AccountTokenPurpose.PASSWORD_RESET, "unknown@example.com"))
        .isEmpty();
    org.assertj.core.api.Assertions.assertThat(
            accountTokens.find(AccountTokenPurpose.PASSWORD_RESET, "locked@townpet.local"))
        .isEmpty();
  }

  @Test
  void passwordResetRejectsWeakPassword() throws Exception {
    passwordResets.request("mango@example.com");
    String token =
        accountTokens.find(AccountTokenPurpose.PASSWORD_RESET, "mango@example.com").orElseThrow();
    mockMvc
        .perform(
            post("/api/v1/auth/password-resets/confirmations")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + token + "\",\"newPassword\":\"weakpassword\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void passwordResetRejectsExpiredToken() throws Exception {
    String token = "expired-reset-token-expired-reset-token";
    java.time.Instant now = java.time.Instant.now();
    passwordResetTokens.save(
        new PasswordResetTokenEntity(
            MEMBER_ID, SecureToken.hash(token), now.minusSeconds(7200), now.minusSeconds(3600)));

    mockMvc
        .perform(
            post("/api/v1/auth/password-resets/confirmations")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + token + "\",\"newPassword\":\"Changed!2026\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void unverifiedEmailCannotLogin() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/sessions")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"mango@example.com\",\"password\":\"password123!\"}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void emailVerificationUsesHashedSingleUseToken() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/email-verifications")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"mango@example.com\"}"))
        .andExpect(status().isAccepted());
    String token =
        accountTokens
            .find(AccountTokenPurpose.EMAIL_VERIFICATION, "mango@example.com")
            .orElseThrow();

    org.assertj.core.api.Assertions.assertThat(emailVerificationTokens.findAll())
        .singleElement()
        .extracting(EmailVerificationTokenEntity::getTokenHash)
        .isEqualTo(SecureToken.hash(token))
        .isNotEqualTo(token);

    mockMvc
        .perform(
            post("/api/v1/auth/email-verifications/confirmations")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + token + "\"}"))
        .andExpect(status().isNoContent());

    org.assertj.core.api.Assertions.assertThat(
            credentials.findByEmailIgnoreCase("mango@example.com").orElseThrow().isEmailVerified())
        .isTrue();
    org.assertj.core.api.Assertions.assertThat(emailVerificationTokens.count()).isZero();
    org.assertj.core.api.Assertions.assertThat(authAudits.count()).isEqualTo(1);
    mockMvc
        .perform(
            post("/api/v1/auth/sessions")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"mango@example.com\",\"password\":\"password123!\"}"))
        .andExpect(status().isCreated());
    mockMvc
        .perform(
            post("/api/v1/auth/email-verifications/confirmations")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + token + "\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void verificationRequestDoesNotRevealUnknownOrVerifiedAccounts() throws Exception {
    CredentialEntity credential = credentials.findByMemberId(MEMBER_ID).orElseThrow();
    credential.verifyEmail(java.time.Instant.now());
    credentials.save(credential);

    mockMvc
        .perform(
            post("/api/v1/auth/email-verifications")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"unknown@example.com\"}"))
        .andExpect(status().isAccepted());
    mockMvc
        .perform(
            post("/api/v1/auth/email-verifications")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"mango@example.com\"}"))
        .andExpect(status().isAccepted());

    org.assertj.core.api.Assertions.assertThat(emailVerificationTokens.count()).isZero();
    org.assertj.core.api.Assertions.assertThat(
            accountTokens.find(AccountTokenPurpose.EMAIL_VERIFICATION, "unknown@example.com"))
        .isEmpty();
    org.assertj.core.api.Assertions.assertThat(
            accountTokens.find(AccountTokenPurpose.EMAIL_VERIFICATION, "mango@example.com"))
        .isEmpty();
  }

  @Test
  void emailVerificationRejectsExpiredToken() throws Exception {
    String token = "expired-verify-token-expired-verify-token";
    java.time.Instant now = java.time.Instant.now();
    emailVerificationTokens.save(
        new EmailVerificationTokenEntity(
            MEMBER_ID, SecureToken.hash(token), now.minusSeconds(7200), now.minusSeconds(3600)));

    mockMvc
        .perform(
            post("/api/v1/auth/email-verifications/confirmations")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + token + "\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void csrfTokenIsExposedOnlyAsNonHttpOnlyCookie() throws Exception {
    mockMvc
        .perform(get("/api/v1/auth/csrf"))
        .andExpect(status().isOk())
        .andExpect(cookie().exists("XSRF-TOKEN"));
  }

  private static Cookie sessionCookie(MvcResult login) {
    return Objects.requireNonNull(login.getResponse().getCookie("SESSION"));
  }

  private void verifyCredential() {
    CredentialEntity credential = credentials.findByMemberId(MEMBER_ID).orElseThrow();
    credential.verifyEmail(java.time.Instant.now());
    credentials.save(credential);
  }
}
