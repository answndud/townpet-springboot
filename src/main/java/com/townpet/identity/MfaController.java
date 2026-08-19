package com.townpet.identity;

import com.townpet.common.ClientAddress;
import com.townpet.common.RequestRateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/mfa")
class MfaController {
  private final MfaService mfa;
  private final RequestRateLimiter rateLimiter;
  private final ClientAddress clientAddress;

  MfaController(MfaService mfa, RequestRateLimiter rateLimiter, ClientAddress clientAddress) {
    this.mfa = mfa;
    this.rateLimiter = rateLimiter;
    this.clientAddress = clientAddress;
  }

  @PostMapping("/enrollment")
  EnrollmentResponse enrollment(
      @AuthenticationPrincipal UserDetails principal, HttpServletRequest httpRequest) {
    UUID memberId = memberId(principal);
    limit(httpRequest, memberId, "mfa-enrollment-start", 5);
    MfaService.Enrollment enrollment = mfa.startEnrollment(memberId);
    return new EnrollmentResponse(
        enrollment.secret(), enrollment.otpauthUri(), enrollment.expiresAt());
  }

  @PostMapping("/enrollment/confirm")
  ResponseEntity<RecoveryResponse> confirm(
      @AuthenticationPrincipal UserDetails principal,
      @Valid @RequestBody CodeRequest request,
      HttpServletRequest httpRequest) {
    UUID memberId = memberId(principal);
    limit(httpRequest, memberId, "mfa-enrollment-confirm", 10);
    List<String> codes = mfa.confirmEnrollment(memberId, request.code());
    httpRequest.getSession(true).setAttribute(MfaService.SESSION_VERIFIED_ATTRIBUTE, true);
    return ResponseEntity.ok(new RecoveryResponse(codes));
  }

  @PostMapping("/verify")
  ResponseEntity<Void> verify(
      @AuthenticationPrincipal UserDetails principal,
      @Valid @RequestBody CodeRequest request,
      HttpServletRequest httpRequest) {
    UUID memberId = memberId(principal);
    limit(httpRequest, memberId, "mfa-verify", 10);
    mfa.verify(memberId, request.code());
    httpRequest.getSession(true).setAttribute(MfaService.SESSION_VERIFIED_ATTRIBUTE, true);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/recovery")
  ResponseEntity<Void> recovery(
      @AuthenticationPrincipal UserDetails principal,
      @Valid @RequestBody RecoveryRequest request,
      HttpServletRequest httpRequest) {
    UUID memberId = memberId(principal);
    limit(httpRequest, memberId, "mfa-recovery", 5);
    mfa.useRecoveryCode(memberId, request.code());
    httpRequest.getSession(true).setAttribute(MfaService.SESSION_VERIFIED_ATTRIBUTE, true);
    return ResponseEntity.noContent().build();
  }

  private void limit(HttpServletRequest request, UUID memberId, String bucket, int max) {
    rateLimiter.requireCapacity(
        bucket + "-ip", clientAddress.resolve(request), max, Duration.ofMinutes(1));
    rateLimiter.requireCapacity(
        bucket + "-member", memberId.toString(), max, Duration.ofMinutes(1));
  }

  private static UUID memberId(UserDetails principal) {
    try {
      return UUID.fromString(principal.getUsername());
    } catch (RuntimeException exception) {
      throw new IllegalStateException("Invalid authenticated member id", exception);
    }
  }

  record CodeRequest(@NotBlank @Pattern(regexp = "^\\d{6}$") String code) {}

  record RecoveryRequest(@NotBlank @Pattern(regexp = "^[A-Za-z0-9]{16}$") String code) {}

  record EnrollmentResponse(String secret, String otpauthUri, java.time.Instant expiresAt) {}

  record RecoveryResponse(List<String> recoveryCodes) {}
}
