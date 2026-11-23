package com.townpet.care;

import com.townpet.common.CurrentMember;
import com.townpet.common.MemberOnly;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/care")
class CareAssignmentController {
  private final CareAssignmentService service;

  CareAssignmentController(CareAssignmentService s) {
    service = s;
  }

  @GetMapping("/requests/{requestId}/assignment")
  @MemberOnly
  Response assignment(@CurrentMember UUID currentMemberId, @PathVariable UUID requestId) {
    try {
      return response(service.findForParticipant(currentMemberId, requestId));
    } catch (NoSuchElementException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    } catch (SecurityException e) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }
  }

  @PostMapping("/requests/{requestId}/applications/{applicationId}/accept")
  @MemberOnly
  Response accept(
      @CurrentMember UUID currentMemberId,
      @PathVariable UUID requestId,
      @PathVariable UUID applicationId,
      @Valid @RequestBody VersionRequest r) {
    try {
      return response(service.accept(currentMemberId, requestId, applicationId, r.version()));
    } catch (NoSuchElementException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    } catch (SecurityException e) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    } catch (IllegalStateException e) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
    }
  }

  @PatchMapping("/assignments/{assignmentId}/status")
  @MemberOnly
  Response status(
      @CurrentMember UUID currentMemberId,
      @PathVariable UUID assignmentId,
      @Valid @RequestBody DecisionRequest r) {
    try {
      return response(service.transition(currentMemberId, assignmentId, r.status(), r.version()));
    } catch (NoSuchElementException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    } catch (SecurityException e) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    } catch (IllegalStateException e) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
    }
  }

  @PostMapping("/assignments/{assignmentId}/feedback")
  @MemberOnly
  FeedbackResponse feedback(
      @CurrentMember UUID currentMemberId,
      @PathVariable UUID assignmentId,
      @Valid @RequestBody FeedbackRequest r) {
    try {
      CareFeedbackEntity f = service.feedback(currentMemberId, assignmentId, r.body());
      return new FeedbackResponse(
          f.getId(), f.getAssignmentId(), f.getAuthorMemberId(), f.getBody(), f.getCreatedAt());
    } catch (NoSuchElementException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    } catch (SecurityException e) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    } catch (IllegalStateException e) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
    }
  }

  private static Response response(CareAssignmentEntity a) {
    return new Response(
        a.getId(),
        a.getRequestId(),
        a.getCaregiverMemberId(),
        a.getStatus(),
        a.getCreatedAt(),
        a.getUpdatedAt(),
        a.getVersion());
  }

  record VersionRequest(@NotNull @Min(0) Long version) {}

  record DecisionRequest(@NotNull CareAssignmentStatus status, @NotNull @Min(0) Long version) {}

  record FeedbackRequest(@NotBlank @Size(max = 2000) String body) {}

  record Response(
      UUID id,
      UUID requestId,
      UUID caregiverMemberId,
      CareAssignmentStatus status,
      Instant createdAt,
      Instant updatedAt,
      long version) {}

  record FeedbackResponse(
      UUID id, UUID assignmentId, UUID authorMemberId, String body, Instant createdAt) {}
}
