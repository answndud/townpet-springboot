package com.townpet.identity;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/auth")
public class SessionController {
  private final AuthenticationManager authenticationManager;
  private final CredentialRepository credentials;
  private final SecurityContextRepository securityContexts =
      new HttpSessionSecurityContextRepository();

  public SessionController(
      AuthenticationManager authenticationManager, CredentialRepository credentials) {
    this.authenticationManager = authenticationManager;
    this.credentials = credentials;
  }

  @GetMapping("/csrf")
  CsrfResponse csrf(CsrfToken token, HttpServletResponse response) {
    response.addHeader(
        HttpHeaders.SET_COOKIE,
        ResponseCookie.from("XSRF-TOKEN", token.getToken())
            .path("/")
            .httpOnly(false)
            .sameSite("Lax")
            .build()
            .toString());
    return new CsrfResponse(token.getToken());
  }

  @PostMapping("/sessions")
  ResponseEntity<SessionResponse> createSession(
      @RequestBody CreateSessionRequest request,
      HttpServletRequest httpRequest,
      HttpServletResponse httpResponse) {
    Authentication authentication;
    try {
      authentication =
          authenticationManager.authenticate(
              UsernamePasswordAuthenticationToken.unauthenticated(
                  request.email(), request.password()));
    } catch (BadCredentialsException exception) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
    }

    httpRequest.getSession(true);
    httpRequest.changeSessionId();
    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(authentication);
    SecurityContextHolder.setContext(context);
    securityContexts.saveContext(context, httpRequest, httpResponse);

    UUID memberId =
        credentials
            .findByEmailIgnoreCase(request.email())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED))
            .getMemberId();
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(new SessionResponse(memberId, Instant.now().plusSeconds(1800)));
  }

  @DeleteMapping("/sessions/current")
  ResponseEntity<Void> deleteCurrentSession(HttpServletRequest request) {
    SecurityContextHolder.clearContext();
    if (request.getSession(false) != null) {
      request.getSession(false).invalidate();
    }
    return ResponseEntity.noContent().build();
  }

  record CreateSessionRequest(String email, String password) {}

  record SessionResponse(UUID memberId, Instant expiresAt) {}

  record CsrfResponse(String token) {}
}
