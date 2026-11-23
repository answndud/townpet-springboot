package com.townpet.engagement;

import com.townpet.common.CurrentMember;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
      @PathVariable UUID publicationId,
      @CurrentMember(required = false) @Nullable UUID currentMemberId,
      @RequestParam(required = false) @Nullable String cursor,
      @RequestParam(defaultValue = "200") int limit) {
    CommentService.CommentPage page = comments.list(publicationId, currentMemberId, cursor, limit);
    return new CommentListResponse(
        page.items().stream().map(CommentController::toResponse).toList(), page.nextCursor());
  }

  @PostMapping
  @MemberOnly
  ResponseEntity<CommentResponse> create(
      @CurrentMember UUID currentMemberId,
      @PathVariable UUID publicationId,
      @Valid @RequestBody CreateCommentRequest request) {
    CommentEntity comment =
        comments.create(
            currentMemberId, publicationId, request.parentCommentId(), request.body());
    return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(comment));
  }

  @DeleteMapping("/{commentId}")
  @MemberOnly
  ResponseEntity<Void> delete(
      @CurrentMember UUID currentMemberId,
      @PathVariable UUID publicationId,
      @PathVariable UUID commentId,
      @Valid @RequestBody DeleteCommentRequest request) {
    comments.delete(currentMemberId, publicationId, commentId, request.version());
    return ResponseEntity.noContent().build();
  }

  @PatchMapping("/{commentId}")
  @MemberOnly
  CommentResponse edit(
      @CurrentMember UUID currentMemberId,
      @PathVariable UUID publicationId,
      @PathVariable UUID commentId,
      @Valid @RequestBody EditCommentRequest request) {
    return toResponse(
        comments.edit(
            currentMemberId, publicationId, commentId, request.version(), request.body()));
  }

  private static CommentResponse toResponse(CommentEntity comment) {
    return new CommentResponse(
        comment.getId(),
        comment.getPublicationId(),
        comment.getAuthorMemberId(),
        comment.getGuestAuthorId() == null ? CommentAuthorType.MEMBER : CommentAuthorType.GUEST,
        comment.getParentCommentId(),
        comment.getBody(),
        comment.getLifecycle(),
        comment.getCreatedAt(),
        comment.getUpdatedAt(),
        comment.getVersion());
  }

  record CommentListResponse(List<CommentResponse> items, @Nullable String nextCursor) {}

  record CreateCommentRequest(
      @NotBlank @Size(max = 5000) String body, @Nullable UUID parentCommentId) {}

  record DeleteCommentRequest(@NotNull @Min(0) Long version) {}

  record EditCommentRequest(
      @NotBlank @Size(max = 5000) String body, @NotNull @Min(0) Long version) {}

  record CommentResponse(
      UUID id,
      UUID publicationId,
      @Nullable UUID authorId,
      CommentAuthorType authorType,
      @Nullable UUID parentCommentId,
      String body,
      CommentLifecycle lifecycle,
      Instant createdAt,
      Instant updatedAt,
      long version) {}
}
