package com.townpet.engagement;

import com.townpet.common.ClientAddress;
import com.townpet.common.MemberOrAnonymousOnly;
import com.townpet.common.RequestRateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping("/api/guest/posts/{publicationId}/comments")
class GuestCommentController {
  private final CommentService comments;
  private final RequestRateLimiter rateLimiter;
  private final ClientAddress clientAddress;
  private final boolean enabled;

  GuestCommentController(
      CommentService comments, RequestRateLimiter rateLimiter, ClientAddress clientAddress,
      @Value("${townpet.engagement.guest-comments.enabled:false}") boolean enabled) {
    this.comments = comments;
    this.rateLimiter = rateLimiter;
    this.clientAddress = clientAddress;
    this.enabled = enabled;
  }

  @PostMapping
  @MemberOrAnonymousOnly
  @ResponseStatus(HttpStatus.CREATED)
  Response create(
      @PathVariable UUID publicationId,
      @Valid @RequestBody CreateRequest request,
      HttpServletRequest httpRequest) {
    requireEnabled();
    rateLimiter.requireCapacity(
        "guest-content-ip", clientAddress.resolve(httpRequest), 120, Duration.ofHours(1));
    rateLimiter.requireCapacity(
        "guest-comment-create", clientAddress.resolve(httpRequest), 30, Duration.ofHours(1));
    rateLimiter.requireCapacity(
        "guest-comment-post",
        clientAddress.resolve(httpRequest) + ":" + publicationId,
        5,
        Duration.ofMinutes(10));
    rateLimiter.requireCapacity(
        "guest-comment-repeat",
        clientAddress.resolve(httpRequest) + ":" + publicationId + ":" + normalizeForDuplicateCheck(request.body()),
        1,
        Duration.ofMinutes(10));
    try {
      return response(
          comments.createGuest(
              request.password(),
              publicationId,
              request.parentCommentId(),
              request.body()));
    } catch (CommentPublicationNotFoundException | CommentNotFoundException exception) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    } catch (CommentReplyDepthExceededException exception) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Replies can only be one level deep");
    }
  }

  @DeleteMapping("/{commentId}")
  @MemberOrAnonymousOnly
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void delete(
      @PathVariable UUID publicationId,
      @PathVariable UUID commentId,
      @Valid @RequestBody DeleteRequest request,
      HttpServletRequest httpRequest) {
    requireEnabled();
    rateLimiter.requireCapacity(
        "guest-content-ip", clientAddress.resolve(httpRequest), 120, Duration.ofHours(1));
    try {
      rateLimiter.requireCapacity("guest-comment-password-check", clientAddress.resolve(httpRequest), 20, Duration.ofHours(1));
      comments.deleteGuest(request.password(), publicationId, commentId, request.version());
    } catch (CommentPublicationNotFoundException | CommentNotFoundException exception) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    } catch (CommentOwnershipException exception) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    } catch (CommentVersionConflictException exception) {
      throw new ResponseStatusException(HttpStatus.CONFLICT);
    }
  }

  @PatchMapping("/{commentId}")
  @MemberOrAnonymousOnly
  Response edit(
      @PathVariable UUID publicationId,
      @PathVariable UUID commentId,
      @Valid @RequestBody EditRequest request,
      HttpServletRequest httpRequest) {
    requireEnabled();
    rateLimiter.requireCapacity("guest-content-ip", clientAddress.resolve(httpRequest), 120, Duration.ofHours(1));
    rateLimiter.requireCapacity("guest-comment-password-check", clientAddress.resolve(httpRequest), 20, Duration.ofHours(1));
    try {
      return response(comments.editGuest(request.password(), publicationId, commentId, request.version(), request.body()));
    } catch (CommentPublicationNotFoundException | CommentNotFoundException exception) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    } catch (CommentOwnershipException exception) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    } catch (CommentVersionConflictException exception) {
      throw new ResponseStatusException(HttpStatus.CONFLICT);
    }
  }

  private void requireEnabled() {
    if (!enabled) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Guest comments are disabled");
  }

  private static Response response(CommentEntity comment) {
    return new Response(
        comment.getId(),
        comment.getPublicationId(),
        null,
        CommentAuthorType.GUEST,
        comment.getParentCommentId(),
        comment.getBody(),
        comment.getLifecycle(),
        comment.getCreatedAt(),
        comment.getUpdatedAt(),
        comment.getVersion());
  }

  private static String normalizeForDuplicateCheck(String body) {
    return body.strip().replaceAll("\\s+", " ").toLowerCase(java.util.Locale.ROOT);
  }

  record CreateRequest(
      @NotBlank @Size(min = 8, max = 72) String password,
      @NotBlank @Size(max = 5000) String body,
      @Nullable UUID parentCommentId) {}

  record DeleteRequest(@NotBlank @Size(min = 8, max = 72) String password, @NotNull @Min(0) Long version) {}

  record EditRequest(
      @NotBlank @Size(min = 8, max = 72) String password,
      @NotNull @Min(0) Long version,
      @NotBlank @Size(max = 5000) String body) {}

  record Response(
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
