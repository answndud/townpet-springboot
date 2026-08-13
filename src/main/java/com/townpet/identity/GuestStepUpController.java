package com.townpet.identity;

import com.townpet.common.ClientAddress;
import com.townpet.common.MemberOrAnonymousOnly;
import com.townpet.common.RequestRateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/guest")
public class GuestStepUpController {
  public static final String GUEST_COOKIE = "TOWNPET_GUEST_ID";
  static final String STEP_UP_COOKIE = "TOWNPET_STEP_UP";
  private final GuestStepUpService steps;
  private final boolean secureCookies;
  private final RequestRateLimiter rateLimiter;

  GuestStepUpController(
      GuestStepUpService steps,
      @Value("${townpet.security.secure-cookies:false}") boolean secureCookies,
      RequestRateLimiter rateLimiter) {
    this.steps = steps;
    this.secureCookies = secureCookies;
    this.rateLimiter = rateLimiter;
  }

  @PostMapping("/authors")
  @MemberOrAnonymousOnly
  @ResponseStatus(HttpStatus.CREATED)
  GuestResponse createAuthor(
      @Valid @RequestBody CreateGuestRequest request, HttpServletResponse response) {
    rateLimiter.requireCapacity("guest-create", "anonymous", 30, Duration.ofMinutes(1));
    GuestAuthorEntity guest = steps.createGuest(request.password());
    response.addHeader(
        "Set-Cookie",
        ResponseCookie.from(GUEST_COOKIE, guest.getPublicId().toString())
            .httpOnly(true)
            .sameSite("Lax")
            .secure(secureCookies)
            .path("/")
            .maxAge(Duration.ofDays(30))
            .build()
            .toString());
    return new GuestResponse(guest.getPublicId());
  }

  @PostMapping("/step-up")
  @MemberOrAnonymousOnly
  StepUpResponse issue(
      @Valid @RequestBody StepUpRequest request,
      @Nullable @CookieValue(name = GUEST_COOKIE, required = false) String cookieGuestId,
      HttpServletResponse response,
      HttpServletRequest httpRequest) {
    rateLimiter.requireCapacity(
        "guest-step-up", ClientAddress.resolve(httpRequest), 30, Duration.ofMinutes(1));
    UUID guestId = request.guestId() == null ? parseGuestId(cookieGuestId) : request.guestId();
    GuestStepUpService.Challenge challenge =
        steps.issue(guestId, request.scope(), request.password());
    response.addHeader(
        "Set-Cookie",
        ResponseCookie.from(STEP_UP_COOKIE, challenge.rawToken())
            .httpOnly(true)
            .sameSite("Lax")
            .secure(secureCookies)
            .path("/")
            .maxAge(Duration.ofMinutes(5))
            .build()
            .toString());
    return new StepUpResponse(challenge.scope(), challenge.expiresAt());
  }

  @PostMapping("/step-up/consume")
  @MemberOrAnonymousOnly
  ConsumedResponse consume(
      @Valid @RequestBody ConsumeRequest request,
      @CookieValue(name = STEP_UP_COOKIE) String token,
      HttpServletResponse response) {
    String scope = steps.consume(token, request.scope());
    response.addHeader(
        "Set-Cookie",
        ResponseCookie.from(STEP_UP_COOKIE, "")
            .httpOnly(true)
            .sameSite("Lax")
            .secure(secureCookies)
            .path("/")
            .maxAge(Duration.ZERO)
            .build()
            .toString());
    return new ConsumedResponse(scope, Instant.now());
  }

  private static UUID parseGuestId(@Nullable String value) {
    try {
      return UUID.fromString(value);
    } catch (RuntimeException exception) {
      throw new org.springframework.web.server.ResponseStatusException(
          HttpStatus.UNAUTHORIZED, "Guest identity is required");
    }
  }

  record CreateGuestRequest(@NotBlank @Size(min = 8, max = 72) String password) {}

  record StepUpRequest(
      @Nullable UUID guestId,
      @NotBlank @Size(max = 80) String scope,
      @NotBlank @Size(min = 8, max = 72) String password) {}

  record ConsumeRequest(@NotBlank @Size(max = 80) String scope) {}

  record GuestResponse(UUID guestId) {}

  record StepUpResponse(String scope, Instant expiresAt) {}

  record ConsumedResponse(String scope, Instant consumedAt) {}
}
