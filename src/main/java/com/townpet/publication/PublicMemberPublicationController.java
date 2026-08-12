package com.townpet.publication;

import com.townpet.member.api.MemberDirectory;
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
@RequestMapping("/api/v1/members/{memberId}/publications")
class PublicMemberPublicationController {
  private final PublicationRepository publications;
  private final MemberDirectory members;

  PublicMemberPublicationController(PublicationRepository publications, MemberDirectory members) {
    this.publications = publications;
    this.members = members;
  }

  @GetMapping
  List<Response> list(@PathVariable UUID memberId) {
    if (members.findPublicationContext(memberId).isEmpty()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }
    if (!members.isPublicPosts(memberId)) return List.of();
    return publications
        .findTop100ByAuthorMemberIdAndLifecycleOrderByCreatedAtDescIdDesc(
            memberId, PublicationLifecycle.ACTIVE)
        .stream()
        .map(PublicMemberPublicationController::response)
        .toList();
  }

  private static Response response(PublicationEntity publication) {
    return new Response(
        publication.getId(),
        publication.getTitle(),
        publication.getBody(),
        publication.getScope(),
        publication.getCreatedAt(),
        publication.getUpdatedAt());
  }

  record Response(
      UUID id,
      String title,
      String body,
      PublicationScope scope,
      Instant createdAt,
      Instant updatedAt) {}
}
