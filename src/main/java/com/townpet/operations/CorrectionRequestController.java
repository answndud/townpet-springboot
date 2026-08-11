package com.townpet.operations;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/corrections")
class CorrectionRequestController {
  private final CorrectionRequestRepository corrections;

  CorrectionRequestController(CorrectionRequestRepository corrections) {
    this.corrections = corrections;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  Response create(
      @AuthenticationPrincipal UserDetails principal, @Valid @RequestBody Request request) {
    UUID memberId;
    try {
      memberId = UUID.fromString(principal.getUsername());
    } catch (IllegalArgumentException exception) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
    }
    CorrectionRequestEntity correction =
        corrections.save(new CorrectionRequestEntity(memberId, request.title(), request.body()));
    return new Response(correction.getId(), correction.getStatus(), correction.getCreatedAt());
  }

  record Request(
      @NotBlank @Size(max = 120) String title, @NotBlank @Size(max = 2000) String body) {}

  record Response(UUID id, String status, Instant createdAt) {}
}
