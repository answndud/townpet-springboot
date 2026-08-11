package com.townpet.trustsafety;

import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin/reports")
@PreAuthorize("hasRole('MODERATOR')")
class AdminReportController {
  private final TrustReportRepository reports;

  AdminReportController(TrustReportRepository reports) {
    this.reports = reports;
  }

  @GetMapping
  List<TrustReportController.ReportResponse> list(
      @RequestParam(defaultValue = "OPEN") String status) {
    return reports.findByStatusOrderByCreatedAtAsc(status).stream()
        .map(AdminReportController::response)
        .toList();
  }

  @PatchMapping("/{id}")
  TrustReportController.ReportResponse review(
      @PathVariable UUID id, @RequestBody TrustReportController.ReviewRequest request) {
    TrustReportEntity report =
        reports.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    report.review(request.status());
    return response(report);
  }

  private static TrustReportController.ReportResponse response(TrustReportEntity r) {
    return new TrustReportController.ReportResponse(
        r.getId(),
        r.getReporterMemberId(),
        r.getTargetType(),
        r.getTargetId(),
        r.getReason(),
        r.getDetail(),
        r.getStatus(),
        r.getCreatedAt());
  }
}
