package com.townpet.care;

import com.townpet.common.CurrentMember;
import com.townpet.common.MemberOnly;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/care/requests/{requestId}/applications")
class CareApplicationController {
  private final CareApplicationService applications;

  CareApplicationController(CareApplicationService applications) {
    this.applications = applications;
  }

  @GetMapping
  @MemberOnly
  List<Response> list(
      @CurrentMember UUID currentMemberId, @PathVariable UUID requestId) {
    return applications.listForRequester(currentMemberId, requestId).stream()
        .map(CareApplicationController::response)
        .toList();
  }

  @PostMapping
  @MemberOnly
  ResponseEntity<Response> apply(
      @CurrentMember UUID currentMemberId,
      @PathVariable UUID requestId,
      @Valid @RequestBody ApplyRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(response(applications.apply(currentMemberId, requestId, request.message())));
  }

  @PatchMapping("/{applicationId}")
  @MemberOnly
  Response decide(
      @CurrentMember UUID currentMemberId,
      @PathVariable UUID requestId,
      @PathVariable UUID applicationId,
      @Valid @RequestBody DecisionRequest request) {
    return response(
        applications.decide(
            currentMemberId, requestId, applicationId, request.status(), request.version()));
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
