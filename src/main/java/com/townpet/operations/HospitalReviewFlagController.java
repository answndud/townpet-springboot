package com.townpet.operations;

import com.townpet.common.MemberOnly;
import com.townpet.common.UuidV7;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/hospital-reviews")
class HospitalReviewFlagController {
  private final JdbcTemplate jdbc;

  HospitalReviewFlagController(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @PostMapping("/{reviewId}/flags")
  @MemberOnly
  @ResponseStatus(HttpStatus.CREATED)
  FlagResponse flag(
      @PathVariable UUID reviewId,
      @AuthenticationPrincipal UserDetails principal,
      @Valid @RequestBody FlagRequest request) {
    Integer reviewCount =
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM hospital_review WHERE id = ?", Integer.class, reviewId);
    if (reviewCount == null || reviewCount == 0) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }
    Integer open =
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM moderator_case WHERE case_type = 'HOSPITAL_REVIEW' AND target_id = ? AND status = 'OPEN'",
            Integer.class,
            reviewId);
    if (open != null && open > 0)
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Already flagged");
    UUID id = UuidV7.randomUuid();
    jdbc.update(
        "INSERT INTO moderator_case (id, case_type, target_type, target_id, subject, detail) VALUES (?, 'HOSPITAL_REVIEW', 'HOSPITAL_REVIEW', ?, ?, ?)",
        id,
        reviewId,
        request.reason(),
        request.detail());
    return new FlagResponse(id, reviewId, "OPEN");
  }

  record FlagRequest(@NotBlank @Size(max = 200) String reason, @Size(max = 4000) String detail) {}

  record FlagResponse(UUID id, UUID reviewId, String status) {}
}
