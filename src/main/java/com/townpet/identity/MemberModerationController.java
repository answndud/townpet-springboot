package com.townpet.identity;

import com.townpet.publication.api.PublicationModeration;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin/moderation/users")
@PreAuthorize("hasRole('MODERATOR')")
class MemberModerationController {
  private final CredentialRepository credentials;
  private final JdbcTemplate jdbc;
  private final PublicationModeration publications;

  MemberModerationController(
      CredentialRepository credentials, JdbcTemplate jdbc, PublicationModeration publications) {
    this.credentials = credentials;
    this.jdbc = jdbc;
    this.publications = publications;
  }

  @PostMapping({"/hide-content", "/restore-content"})
  @Transactional
  Response content(
      @RequestBody @Valid MemberRequest request,
      @AuthenticationPrincipal UserDetails principal,
      jakarta.servlet.http.HttpServletRequest http) {
    CredentialEntity credential =
        credentials
            .findByMemberId(request.memberId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    String action =
        http.getRequestURI().endsWith("restore-content") ? "RESTORE_CONTENT" : "HIDE_CONTENT";
    int affected =
        publications.setAuthorContentVisibility(
            request.memberId(), action.equals("RESTORE_CONTENT"));
    recordAction(
        memberId(principal), request.memberId(), request.memberId(), action, request.reason());
    return new Response(credential.getMemberId(), action, affected);
  }

  @PostMapping("/sanction")
  @Transactional
  Response sanction(
      @RequestBody @Valid MemberRequest request, @AuthenticationPrincipal UserDetails principal) {
    CredentialEntity credential =
        credentials
            .findByMemberId(request.memberId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    credential.setEnabled(false);
    recordAction(
        memberId(principal), request.memberId(), request.memberId(), "SANCTION", request.reason());
    return new Response(credential.getMemberId(), "SANCTION", 0);
  }

  private static UUID memberId(UserDetails principal) {
    try {
      return UUID.fromString(principal.getUsername());
    } catch (IllegalArgumentException exception) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
    }
  }

  private void recordAction(
      UUID actor, UUID targetMember, UUID targetId, String action, String reason) {
    jdbc.update(
        "INSERT INTO moderation_action (id, actor_member_id, target_member_id, target_type, target_id, action, reason) VALUES (?, ?, ?, 'MEMBER', ?, ?, ?)",
        UUID.randomUUID(),
        actor,
        targetMember,
        targetId,
        action,
        reason);
  }

  record MemberRequest(@NotNull UUID memberId, String reason) {}

  record Response(UUID memberId, String action, int affectedPublications) {}
}
