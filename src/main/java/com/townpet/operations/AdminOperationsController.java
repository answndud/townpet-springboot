package com.townpet.operations;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('MODERATOR')")
class AdminOperationsController {
  private final CorrectionRequestRepository corrections;
  private final JdbcTemplate jdbc;

  AdminOperationsController(CorrectionRequestRepository corrections, JdbcTemplate jdbc) {
    this.corrections = corrections;
    this.jdbc = jdbc;
  }

  @GetMapping("/corrections")
  List<CorrectionResponse> corrections() {
    return corrections.findTop100ByOrderByCreatedAtDescIdDesc().stream()
        .map(
            correction ->
                new CorrectionResponse(
                    correction.getId(),
                    correction.getMemberId(),
                    correction.getTitle(),
                    correction.getBody(),
                    correction.getStatus(),
                    correction.getCreatedAt()))
        .toList();
  }

  @GetMapping("/moderation-logs")
  List<ModerationLogResponse> moderationLogs() {
    return jdbc.query(
        "SELECT id, actor_member_id, target_member_id, target_type, target_id, action, reason, created_at "
            + "FROM moderation_action ORDER BY created_at DESC LIMIT 100",
        (rs, row) ->
            new ModerationLogResponse(
                rs.getObject("id", UUID.class),
                rs.getObject("actor_member_id", UUID.class),
                rs.getObject("target_member_id", UUID.class),
                rs.getString("target_type"),
                rs.getObject("target_id", UUID.class),
                rs.getString("action"),
                rs.getString("reason"),
                rs.getTimestamp("created_at").toInstant()));
  }

  record CorrectionResponse(
      UUID id, UUID memberId, String title, String body, String status, Instant createdAt) {}

  record ModerationLogResponse(
      UUID id,
      UUID actorMemberId,
      UUID targetMemberId,
      String targetType,
      UUID targetId,
      String action,
      String reason,
      Instant createdAt) {}
}
