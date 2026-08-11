package com.townpet.trustsafety;

import com.townpet.common.UuidV7;
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
@RequestMapping("/api/v1/trust-reports")
class TrustReportController {
  private final TrustReportRepository reports;

  TrustReportController(TrustReportRepository reports) {
    this.reports = reports;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  ReportResponse create(
      @AuthenticationPrincipal UserDetails p, @Valid @RequestBody CreateRequest r) {
    UUID member = memberId(p);
    if (reports
        .findByReporterMemberIdAndTargetTypeAndTargetId(member, r.targetType(), r.targetId())
        .isPresent()) throw new ResponseStatusException(HttpStatus.CONFLICT, "Already reported");
    return response(
        reports.save(
            new TrustReportEntity(
                UuidV7.randomUuid(),
                member,
                r.targetType(),
                r.targetId(),
                r.reason(),
                r.detail())));
  }

  @GetMapping
  @org.springframework.security.access.prepost.PreAuthorize("hasRole('MODERATOR')")
  List<ReportResponse> queue(@RequestParam(defaultValue = "OPEN") String status) {
    return reports.findByStatusOrderByCreatedAtAsc(status).stream()
        .map(TrustReportController::response)
        .toList();
  }

  @PatchMapping("/{id}")
  @org.springframework.security.access.prepost.PreAuthorize("hasRole('MODERATOR')")
  ReportResponse review(@PathVariable UUID id, @Valid @RequestBody ReviewRequest r) {
    TrustReportEntity report =
        reports.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    report.review(r.status());
    return response(report);
  }

  private static UUID memberId(UserDetails p) {
    try {
      return UUID.fromString(p.getUsername());
    } catch (Exception e) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
    }
  }

  private static ReportResponse response(TrustReportEntity r) {
    return new ReportResponse(
        r.getId(),
        r.getReporterMemberId(),
        r.getTargetType(),
        r.getTargetId(),
        r.getReason(),
        r.getDetail(),
        r.getStatus(),
        r.getCreatedAt());
  }

  record CreateRequest(
      @NotBlank @Size(max = 30) String targetType,
      @NotNull UUID targetId,
      @NotBlank @Size(max = 40) String reason,
      @Size(max = 1000) String detail) {}

  record ReviewRequest(@Pattern(regexp = "REVIEWED|REJECTED") String status) {}

  record ReportResponse(
      UUID id,
      UUID reporterMemberId,
      String targetType,
      UUID targetId,
      String reason,
      String detail,
      String status,
      Instant createdAt) {}
}
