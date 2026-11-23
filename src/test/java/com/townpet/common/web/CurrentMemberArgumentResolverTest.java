package com.townpet.common.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.townpet.common.CurrentMember;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.server.ResponseStatusException;

class CurrentMemberArgumentResolverTest {
  private static final UUID MEMBER = UUID.fromString("00000000-0000-4000-8000-000000000201");
  private final CurrentMemberArgumentResolver resolver = new CurrentMemberArgumentResolver();

  @AfterEach
  void clearContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void resolvesTheAuthenticatedMemberId() throws Exception {
    authenticate(MEMBER.toString());

    assertEquals(MEMBER, resolve("required"));
    assertEquals(MEMBER, resolve("optional"));
  }

  @Test
  void rejectsMissingMemberWhenRequiredAndReturnsNullWhenOptional() throws Exception {
    assertStatus(HttpStatus.UNAUTHORIZED, () -> resolve("required"));
    assertNull(resolve("optional"));

    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
    assertStatus(HttpStatus.UNAUTHORIZED, () -> resolve("required"));
    assertNull(resolve("optional"));
  }

  @Test
  void rejectsAPrincipalThatIsNotAMemberId() {
    authenticate("not-a-uuid");

    assertStatus(HttpStatus.UNAUTHORIZED, () -> resolve("required"));
    assertStatus(HttpStatus.UNAUTHORIZED, () -> resolve("optional"));
  }

  private void authenticate(String username) {
    User principal = new User(username, "n/a", AuthorityUtils.createAuthorityList("ROLE_MEMBER"));
    SecurityContextHolder.getContext()
        .setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(
                principal, null, principal.getAuthorities()));
  }

  private Object resolve(String method) throws Exception {
    MethodParameter parameter =
        new MethodParameter(Handlers.class.getDeclaredMethod(method, UUID.class), 0);
    return resolver.resolveArgument(parameter, null, null, null);
  }

  private static void assertStatus(HttpStatus expected, org.junit.jupiter.api.function.Executable call) {
    ResponseStatusException exception = assertThrows(ResponseStatusException.class, call);
    assertEquals(expected, HttpStatus.valueOf(exception.getStatusCode().value()));
  }

  @SuppressWarnings("unused")
  private static final class Handlers {
    void required(@CurrentMember UUID memberId) {}

    void optional(@CurrentMember(required = false) UUID memberId) {}
  }
}
