package com.townpet.care;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.*;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/care/requests/{requestId}/applications")
class CareApplicationController {
  private final CareApplicationService applications;

  CareApplicationController(CareApplicationService applications) {
    this.applications = applications;
  }

  @GetMapping
  List<Response> list(
      @AuthenticationPrincipal UserDetails principal, @PathVariable UUID requestId) {
    try {
      return applications.listForRequester(memberId(principal), requestId).stream()
          .map(CareApplicationController::response)
          .toList();
    } catch (NoSuchElementException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    } catch (SecurityException e) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }
  }

  @PostMapping
  ResponseEntity<Response> apply(
      @AuthenticationPrincipal UserDetails principal,
      @PathVariable UUID requestId,
      @Valid @RequestBody ApplyRequest request) {
    try {
      return ResponseEntity.status(HttpStatus.CREATED)
          .body(response(applications.apply(memberId(principal), requestId, request.message())));
    } catch (NoSuchElementException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    } catch (IllegalArgumentException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
    } catch (IllegalStateException e) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
    }
  }

  @PatchMapping("/{applicationId}")
  Response decide(
      @AuthenticationPrincipal UserDetails principal,
      @PathVariable UUID requestId,
      @PathVariable UUID applicationId,
      @Valid @RequestBody DecisionRequest request) {
    try {
      return response(
          applications.decide(
              memberId(principal), requestId, applicationId, request.status(), request.version()));
    } catch (NoSuchElementException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    } catch (SecurityException e) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    } catch (IllegalStateException e) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
    }
  }

  private static UUID memberId(UserDetails principal) {
    try {
      return UUID.fromString(principal.getUsername());
    } catch (Exception e) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
    }
  }

  private static Response response(CareApplicationEntity a) {
    return new Response(
        a.getId(),
        a.getRequestId(),
        a.getApplicantMemberId(),
        a.getMessage(),
        a.getStatus(),
        a.getCreatedAt(),
        a.getUpdatedAt(),
        a.getVersion());
  }

  record ApplyRequest(@NotBlank @Size(max = 2000) String message) {}

  record DecisionRequest(@NotNull CareApplicationStatus status, @NotNull @Min(0) Long version) {}

  record Response(
      UUID id,
      UUID requestId,
      UUID applicantMemberId,
      String message,
      CareApplicationStatus status,
      Instant createdAt,
      Instant updatedAt,
      long version) {}
}
