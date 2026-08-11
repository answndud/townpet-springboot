package com.townpet.identity;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/auth-audits")
@PreAuthorize("hasRole('MODERATOR')")
class AuthAuditAdminController {
  private final AuthAuditRepository audits;

  AuthAuditAdminController(AuthAuditRepository audits) {
    this.audits = audits;
  }

  @GetMapping
  List<Response> list() {
    return audits.findAll().stream()
        .map(audit -> new Response(audit.getMemberId(), audit.getAction(), audit.getCreatedAt()))
        .toList();
  }

  @GetMapping("/export")
  ResponseEntity<byte[]> export() {
    StringBuilder csv = new StringBuilder("member_id,action,created_at\n");
    for (Response item : list())
      csv.append(item.memberId())
          .append(',')
          .append(item.action())
          .append(',')
          .append(item.createdAt())
          .append('\n');
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType("text/csv"))
        .body(csv.toString().getBytes(StandardCharsets.UTF_8));
  }

  record Response(UUID memberId, String action, java.time.Instant createdAt) {}
}
