package com.townpet.identity;

import com.townpet.operations.ModerationActionEntity;
import com.townpet.operations.ModerationActionRepository;
import com.townpet.publication.api.PublicationModeration;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import org.springframework.http.HttpStatus;
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
  private final ModerationActionRepository actions;
  private final PublicationModeration publications;

  MemberModerationController(
      CredentialRepository credentials,
      ModerationActionRepository actions,
      PublicationModeration publications) {
    this.credentials = credentials;
    this.actions = actions;
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
    actions.save(
        new ModerationActionEntity(
            memberId(principal),
            request.memberId(),
            "MEMBER",
            request.memberId(),
            action,
            request.reason()));
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
    actions.save(
        new ModerationActionEntity(
            memberId(principal),
            request.memberId(),
            "MEMBER",
            request.memberId(),
            "SANCTION",
            request.reason()));
    return new Response(credential.getMemberId(), "SANCTION", 0);
  }

  private static UUID memberId(UserDetails principal) {
    try {
      return UUID.fromString(principal.getUsername());
    } catch (IllegalArgumentException exception) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
    }
  }

  record MemberRequest(@NotNull UUID memberId, String reason) {}

  record Response(UUID memberId, String action, int affectedPublications) {}
}
