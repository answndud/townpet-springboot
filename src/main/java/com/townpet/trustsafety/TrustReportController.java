package com.townpet.trustsafety;

import com.townpet.common.MemberOnly;
import com.townpet.common.UuidV7;
import com.townpet.engagement.api.CommentModeration;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.*;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping({"/api/v1/trust-reports", "/api/reports"})
@Validated
class TrustReportController {
  private final TrustReportRepository reports;
  private final CommentModeration comments;

  TrustReportController(TrustReportRepository reports, CommentModeration comments) {
    this.reports = reports;
    this.comments = comments;
  }

  @PostMapping
  @MemberOnly
  @ResponseStatus(HttpStatus.CREATED)
  ReportResponse create(
      @AuthenticationPrincipal UserDetails p, @Valid @RequestBody CreateRequest r) {
    UUID member = memberId(p);
    if ("COMMENT".equals(r.targetType()) && !comments.isActive(r.targetId())) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }
    if (!"COMMENT".equals(r.targetType()) && !"PUBLICATION".equals(r.targetType())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported report target");
    }
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
  List<ReportResponse> queue(
      @RequestParam(defaultValue = "OPEN") @Pattern(regexp = "OPEN|REVIEWED|REJECTED")
          String status) {
    return reports.findTop100ByStatusOrderByCreatedAtAscIdAsc(status).stream()
        .map(TrustReportController::response)
        .toList();
  }

  @PatchMapping("/{id}")
  @org.springframework.security.access.prepost.PreAuthorize("hasRole('MODERATOR')")
  @org.springframework.transaction.annotation.Transactional
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

  record ReviewRequest(@NotBlank @Pattern(regexp = "REVIEWED|REJECTED") String status) {}

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
