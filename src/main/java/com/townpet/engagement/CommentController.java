package com.townpet.engagement;

import com.townpet.common.MemberOnly;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping({
  "/api/v1/publications/{publicationId}/comments",
  "/api/posts/{publicationId}/comments"
})
class CommentController {
  private final CommentService comments;

  CommentController(CommentService comments) {
    this.comments = comments;
  }

  @GetMapping
  CommentListResponse list(
      @PathVariable UUID publicationId, @AuthenticationPrincipal @Nullable UserDetails principal) {
    try {
      return new CommentListResponse(
          comments.list(publicationId, viewerMemberId(principal)).stream()
              .map(CommentController::toResponse)
              .toList());
    } catch (CommentPublicationNotFoundException | CommentNotFoundException exception) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }
  }

  @PostMapping
  @MemberOnly
  ResponseEntity<CommentResponse> create(
      @AuthenticationPrincipal UserDetails principal,
      @PathVariable UUID publicationId,
      @Valid @RequestBody CreateCommentRequest request) {
    try {
      CommentEntity comment =
          comments.create(
              memberId(principal), publicationId, request.parentCommentId(), request.body());
      return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(comment));
    } catch (CommentPublicationNotFoundException exception) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }
  }

  @DeleteMapping("/{commentId}")
  @MemberOnly
  ResponseEntity<Void> delete(
      @AuthenticationPrincipal UserDetails principal,
      @PathVariable UUID publicationId,
      @PathVariable UUID commentId,
      @Valid @RequestBody DeleteCommentRequest request) {
    try {
      comments.delete(memberId(principal), publicationId, commentId, request.version());
      return ResponseEntity.noContent().build();
    } catch (CommentNotFoundException | CommentPublicationNotFoundException exception) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    } catch (CommentOwnershipException exception) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "Only the author can delete this comment");
    } catch (CommentVersionConflictException exception) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Comment has changed");
    }
  }

  @PatchMapping("/{commentId}")
  @MemberOnly
  CommentResponse edit(
      @AuthenticationPrincipal UserDetails principal,
      @PathVariable UUID publicationId,
      @PathVariable UUID commentId,
      @Valid @RequestBody EditCommentRequest request) {
    try {
      return toResponse(
          comments.editById(memberId(principal), commentId, request.version(), request.body()));
    } catch (CommentNotFoundException | CommentPublicationNotFoundException exception) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    } catch (CommentOwnershipException exception) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    } catch (CommentVersionConflictException exception) {
      throw new ResponseStatusException(HttpStatus.CONFLICT);
    }
  }

  @Nullable
  private static UUID viewerMemberId(@Nullable UserDetails principal) {
    if (principal == null) return null;
    try {
      return UUID.fromString(principal.getUsername());
    } catch (IllegalArgumentException exception) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid principal");
    }
  }

  private static UUID memberId(UserDetails principal) {
    UUID memberId = viewerMemberId(principal);
    if (memberId == null) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
    }
    return memberId;
  }

  private static CommentResponse toResponse(CommentEntity comment) {
    return new CommentResponse(
        comment.getId(),
        comment.getPublicationId(),
        comment.getAuthorMemberId(),
        comment.getParentCommentId(),
        comment.getBody(),
        comment.getLifecycle(),
        comment.getCreatedAt(),
        comment.getUpdatedAt(),
        comment.getVersion());
  }

  record CommentListResponse(List<CommentResponse> items) {}

  record CreateCommentRequest(
      @NotBlank @Size(max = 5000) String body, @Nullable UUID parentCommentId) {}

  record DeleteCommentRequest(@NotNull @Min(0) Long version) {}

  record EditCommentRequest(
      @NotBlank @Size(max = 5000) String body, @NotNull @Min(0) Long version) {}

  record CommentResponse(
      UUID id,
      UUID publicationId,
      @Nullable UUID authorId,
      @Nullable UUID parentCommentId,
      String body,
      CommentLifecycle lifecycle,
      Instant createdAt,
      Instant updatedAt,
      long version) {}
}
