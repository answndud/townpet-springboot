package com.townpet.trustsafety;

import com.townpet.common.UuidV7;
import com.townpet.engagement.api.CommentModeration;
import com.townpet.engagement.api.CommentModeration.CommentView;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.lang.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin/comments")
@PreAuthorize("hasRole('MODERATOR')")
@Validated
class AdminCommentModerationController {
  private final CommentModeration comments;
  private final JdbcTemplate jdbc;

  AdminCommentModerationController(CommentModeration comments, JdbcTemplate jdbc) {
    this.comments = comments;
    this.jdbc = jdbc;
  }

  @GetMapping
  List<CommentView> list(
      @Nullable @RequestParam(required = false) UUID publicationId,
      @Nullable @RequestParam(required = false) @Pattern(regexp = "MEMBER|GUEST") String authorType,
      @Nullable @RequestParam(required = false)
          @Pattern(regexp = "ACTIVE|DELETED|MODERATOR_DELETED")
          String lifecycle,
      @Nullable @RequestParam(value = "from", required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          Instant createdAfter,
      @Nullable @RequestParam(value = "until", required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          Instant createdBefore,
      @RequestParam(defaultValue = "50") @Min(1) @Max(100) int limit) {
    if (createdAfter != null && createdBefore != null && !createdAfter.isBefore(createdBefore)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid time range");
    }
    return comments.search(
        publicationId, authorType, lifecycle, createdAfter, createdBefore, limit);
  }

  @PostMapping("/{commentId}/moderation")
  @Transactional
  CommentView act(
      @PathVariable UUID commentId,
      @Valid @RequestBody ActionRequest request,
      @AuthenticationPrincipal UserDetails principal) {
    boolean delete = "DELETE".equals(request.action());
    CommentView result =
        (delete ? comments.delete(commentId) : comments.restore(commentId))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT));
    jdbc.update(
        "INSERT INTO moderation_action (id, actor_member_id, target_type, target_id, action, reason) "
            + "VALUES (?, ?, 'COMMENT', ?, ?, ?)",
        UuidV7.randomUuid(),
        memberId(principal),
        commentId,
        delete ? "COMMENT_DELETED" : "COMMENT_RESTORED",
        request.reason().trim());
    return result;
  }

  private static UUID memberId(UserDetails principal) {
    try {
      return UUID.fromString(principal.getUsername());
    } catch (RuntimeException exception) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
    }
  }

  record ActionRequest(
      @NotBlank @Pattern(regexp = "DELETE|RESTORE") String action,
      @NotBlank @Size(max = 500) String reason) {}
}
