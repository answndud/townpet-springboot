package com.townpet.engagement;

import com.townpet.common.ClientAddress;
import com.townpet.common.MemberOrAnonymousOnly;
import com.townpet.common.RequestRateLimiter;
import com.townpet.identity.GuestStepUpController;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/guest/posts/{publicationId}/comments")
class GuestCommentController {
  private final CommentService comments;
  private final RequestRateLimiter rateLimiter;
  private final ClientAddress clientAddress;

  GuestCommentController(
      CommentService comments, RequestRateLimiter rateLimiter, ClientAddress clientAddress) {
    this.comments = comments;
    this.rateLimiter = rateLimiter;
    this.clientAddress = clientAddress;
  }

  @PostMapping
  @MemberOrAnonymousOnly
  @ResponseStatus(HttpStatus.CREATED)
  Response create(
      @PathVariable UUID publicationId,
      @CookieValue(name = GuestStepUpController.GUEST_COOKIE) UUID guestId,
      @Valid @RequestBody CreateRequest request,
      HttpServletRequest httpRequest) {
    rateLimiter.requireCapacity(
        "guest-content-ip", clientAddress.resolve(httpRequest), 120, Duration.ofHours(1));
    rateLimiter.requireCapacity(
        "guest-comment-create", guestId.toString(), 30, Duration.ofHours(1));
    try {
      return response(
          comments.createGuest(
              guestId,
              request.password(),
              publicationId,
              request.parentCommentId(),
              request.body()));
    } catch (CommentPublicationNotFoundException | CommentNotFoundException exception) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }
  }

  private static Response response(CommentEntity comment) {
    return new Response(
        comment.getId(),
        comment.getPublicationId(),
        comment.getParentCommentId(),
        comment.getBody(),
        comment.getLifecycle(),
        comment.getCreatedAt(),
        comment.getUpdatedAt(),
        comment.getVersion());
  }

  record CreateRequest(
      @NotBlank @Size(min = 8, max = 72) String password,
      @NotBlank @Size(max = 5000) String body,
      @Nullable UUID parentCommentId) {}

  record Response(
      UUID id,
      UUID publicationId,
      @Nullable UUID parentCommentId,
      String body,
      CommentLifecycle lifecycle,
      Instant createdAt,
      Instant updatedAt,
      long version) {}
}
