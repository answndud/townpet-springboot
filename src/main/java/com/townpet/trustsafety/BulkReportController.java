package com.townpet.trustsafety;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports/bulk")
@PreAuthorize("hasRole('MODERATOR')")
class BulkReportController {
  private final TrustReportRepository reports;

  BulkReportController(TrustReportRepository reports) {
    this.reports = reports;
  }

  @PatchMapping
  @Transactional
  Response update(@RequestBody @Valid Request request) {
    reports.findAllById(request.ids()).forEach(report -> report.review(request.status()));
    return new Response(request.ids().size(), request.status());
  }

  record Request(
      @NotEmpty List<UUID> ids, @Pattern(regexp = "OPEN|REVIEWED|REJECTED") String status) {}

  record Response(int updated, String status) {}
}
