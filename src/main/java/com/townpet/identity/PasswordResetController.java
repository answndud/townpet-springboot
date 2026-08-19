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
@RequestMapping("/api/v1/auth/password-resets")
public class PasswordResetController {
  private final PasswordResetService passwordResets;
  private final RequestRateLimiter rateLimiter;
  private final ClientAddress clientAddress;

  public PasswordResetController(
      PasswordResetService passwordResets, RequestRateLimiter rateLimiter, ClientAddress clientAddress) {
    this.passwordResets = passwordResets;
    this.rateLimiter = rateLimiter;
    this.clientAddress = clientAddress;
  }

  @PostMapping
  ResponseEntity<Void> request(
      @Valid @RequestBody ResetRequest request, HttpServletRequest httpRequest) {
    rateLimiter.requireCapacity(
        "password-reset-ip", clientAddress.resolve(httpRequest), 30, Duration.ofMinutes(1));
    rateLimiter.requireCapacity("password-reset-global", "all", 300, Duration.ofMinutes(1));
    passwordResets.request(request.email());
    return ResponseEntity.accepted().build();
  }

  @PostMapping("/confirmations")
  ResponseEntity<Void> confirm(
      @Valid @RequestBody ResetConfirmation request, HttpServletRequest httpRequest) {
    passwordResets.confirm(request.token(), request.newPassword());
    if (httpRequest.getSession(false) != null) {
      httpRequest.getSession(false).invalidate();
    }
    return ResponseEntity.noContent().build();
  }

  record ResetRequest(@NotBlank @Email @Size(max = 320) String email) {}

  record ResetConfirmation(
      @NotBlank @Size(min = 32, max = 128) String token,
      @NotBlank @Size(min = 10, max = 72) String newPassword) {}
}
