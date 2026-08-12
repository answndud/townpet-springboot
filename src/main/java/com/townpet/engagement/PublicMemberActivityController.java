package com.townpet.engagement;

import com.townpet.member.api.MemberDirectory;
import com.townpet.publication.api.PublicationAccess;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/members/{memberId}")
class PublicMemberActivityController {
  private final CommentRepository comments;
  private final ReactionRepository reactions;
  private final MemberDirectory members;
  private final PublicationAccess publications;

  PublicMemberActivityController(
      CommentRepository comments,
      ReactionRepository reactions,
      MemberDirectory members,
      PublicationAccess publications) {
    this.comments = comments;
    this.reactions = reactions;
    this.members = members;
    this.publications = publications;
  }

  @GetMapping("/comments")
  List<CommentResponse> comments(@PathVariable UUID memberId) {
    requireMember(memberId);
    if (!members.isPublicComments(memberId)) return List.of();
    return comments
        .findByAuthorMemberIdAndLifecycleOrderByCreatedAtDesc(memberId, CommentLifecycle.ACTIVE)
        .stream()
        .filter(comment -> publications.existsActive(comment.getPublicationId()))
        .map(
            comment ->
                new CommentResponse(
                    comment.getId(),
                    comment.getPublicationId(),
                    comment.getBody(),
                    comment.getCreatedAt()))
        .toList();
  }

  @GetMapping("/reactions")
  List<ReactionResponse> reactions(@PathVariable UUID memberId) {
    requireMember(memberId);
    return reactions.findByAuthorMemberIdOrderByCreatedAtDesc(memberId).stream()
        .filter(reaction -> publications.existsActive(reaction.getPublicationId()))
        .map(
            reaction ->
                new ReactionResponse(
                    reaction.getPublicationId(),
                    reaction.getType().name(),
                    reaction.getCreatedAt()))
        .toList();
  }

  private void requireMember(UUID memberId) {
    if (members.findPublicationContext(memberId).isEmpty()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }
  }

  record CommentResponse(UUID id, UUID publicationId, String body, Instant createdAt) {}

  record ReactionResponse(UUID publicationId, String type, Instant createdAt) {}
}
