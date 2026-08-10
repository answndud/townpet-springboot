package com.townpet.identity;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/email-verifications")
public class EmailVerificationController {
  private final EmailVerificationService verifications;

  public EmailVerificationController(EmailVerificationService verifications) {
    this.verifications = verifications;
  }

  @PostMapping
  ResponseEntity<Void> request(@Valid @RequestBody VerificationRequest request) {
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
