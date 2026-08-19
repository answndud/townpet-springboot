package com.townpet.identity;

import com.townpet.common.ClientAddress;
import com.townpet.common.RequestRateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/email-verifications")
public class EmailVerificationController {
  private final EmailVerificationService verifications;
  private final RequestRateLimiter rateLimiter;

  public EmailVerificationController(
      EmailVerificationService verifications, RequestRateLimiter rateLimiter) {
    this.verifications = verifications;
    this.rateLimiter = rateLimiter;
  }

  @PostMapping
  ResponseEntity<Void> request(
      @Valid @RequestBody VerificationRequest request, HttpServletRequest httpRequest) {
    rateLimiter.requireCapacity(
        "email-verification-ip", ClientAddress.resolve(httpRequest), 30, Duration.ofMinutes(1));
    rateLimiter.requireCapacity("email-verification-global", "all", 300, Duration.ofMinutes(1));
    verifications.request(request.email());
    return ResponseEntity.accepted().build();
  }

  @PostMapping("/confirmations")
  ResponseEntity<Void> confirm(@Valid @RequestBody VerificationConfirmation request) {
    verifications.confirm(request.token());
    return ResponseEntity.noContent().build();
  }

  record VerificationRequest(@NotBlank @Email @Size(max = 320) String email) {}

  record VerificationConfirmation(@NotBlank @Size(min = 32, max = 128) String token) {}
}
