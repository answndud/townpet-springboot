package com.townpet.operations;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.lang.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('MODERATOR')")
class AdminModeratorCaseController {
  private final JdbcTemplate jdbc;

  AdminModeratorCaseController(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @GetMapping({"/care-feedbacks", "/hospital-review-flags", "/moderation/direct"})
  List<CaseResponse> list(jakarta.servlet.http.HttpServletRequest request) {
    String caseType = caseType(request.getRequestURI());
    return jdbc.query(
        "SELECT id, case_type, target_type, target_id, subject, detail, status, created_at, resolved_at "
            + "FROM moderator_case WHERE case_type = ? ORDER BY created_at DESC LIMIT 100",
        (rs, row) ->
            new CaseResponse(
                rs.getObject("id", UUID.class),
                rs.getString("case_type"),
                rs.getString("target_type"),
                rs.getObject("target_id", UUID.class),
                rs.getString("subject"),
                rs.getString("detail"),
                rs.getString("status"),
                rs.getTimestamp("created_at").toInstant(),
                resolvedAt(rs)),
        caseType);
  }

  @PatchMapping({"/care-feedbacks/{id}", "/hospital-review-flags/{id}", "/moderation/direct/{id}"})
  @Transactional
  CaseResponse review(
      @PathVariable UUID id,
      @Valid @RequestBody ReviewRequest request,
      @AuthenticationPrincipal UserDetails principal) {
    if (!Set.of("REVIEWED", "DISMISSED").contains(request.status())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported case status");
    }
    int updated =
        jdbc.update(
            "UPDATE moderator_case SET status = ?, resolved_at = CURRENT_TIMESTAMP, resolved_by = ? "
                + "WHERE id = ? AND status = 'OPEN'",
            request.status(),
            memberId(principal),
            id);
    if (updated == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    return find(id);
  }

  @PostMapping("/moderation/direct")
  @Transactional
  CaseResponse direct(
      @Valid @RequestBody DirectRequest request, @AuthenticationPrincipal UserDetails principal) {
    UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO moderator_case (id, case_type, target_type, target_id, subject, detail) VALUES (?, 'DIRECT_MODERATION', ?, ?, ?, ?)",
        id,
        request.targetType(),
        request.targetId(),
        request.action(),
        request.reason());
    jdbc.update(
        "INSERT INTO moderation_action (id, actor_member_id, target_type, target_id, action, reason) VALUES (?, ?, ?, ?, ?, ?)",
        UUID.randomUUID(),
        memberId(principal),
        request.targetType(),
        request.targetId(),
        request.action(),
        request.reason());
    return find(id);
  }

  private CaseResponse find(UUID id) {
    return jdbc.queryForObject(
        "SELECT id, case_type, target_type, target_id, subject, detail, status, created_at, resolved_at FROM moderator_case WHERE id = ?",
        (rs, row) ->
            new CaseResponse(
                rs.getObject("id", UUID.class),
                rs.getString("case_type"),
                rs.getString("target_type"),
                rs.getObject("target_id", UUID.class),
                rs.getString("subject"),
                rs.getString("detail"),
                rs.getString("status"),
                rs.getTimestamp("created_at").toInstant(),
                resolvedAt(rs)),
        id);
  }

  private static String caseType(String path) {
    if (path.endsWith("/care-feedbacks")) return "CARE_FEEDBACK";
    if (path.endsWith("/hospital-review-flags")) return "HOSPITAL_REVIEW";
    if (path.endsWith("/moderation/direct")) return "DIRECT_MODERATION";
    throw new ResponseStatusException(HttpStatus.NOT_FOUND);
  }

  private static @Nullable Instant resolvedAt(java.sql.ResultSet rs) throws java.sql.SQLException {
    java.sql.Timestamp timestamp = rs.getTimestamp("resolved_at");
    return timestamp == null ? null : timestamp.toInstant();
  }

  private static UUID memberId(UserDetails principal) {
    try {
      return UUID.fromString(principal.getUsername());
    } catch (RuntimeException exception) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
    }
  }

  record ReviewRequest(
      @NotBlank @jakarta.validation.constraints.Pattern(regexp = "REVIEWED|DISMISSED")
          String status) {}

  record DirectRequest(
      @NotBlank @Size(max = 80) String targetType,
      @NotNull UUID targetId,
      @NotBlank @Size(max = 80) String action,
      @Size(max = 2000) String reason) {}

  record CaseResponse(
      UUID id,
      String caseType,
      String targetType,
      UUID targetId,
      String subject,
      String detail,
      String status,
      Instant createdAt,
      @Nullable Instant resolvedAt) {}
}
