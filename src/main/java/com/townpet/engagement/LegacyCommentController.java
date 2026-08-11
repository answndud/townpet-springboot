package com.townpet.engagement;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/comments")
class LegacyCommentController {
  private final CommentService comments;

  LegacyCommentController(CommentService comments) {
    this.comments = comments;
  }

  @PatchMapping("/{id}")
  CommentController.CommentResponse edit(
      @AuthenticationPrincipal UserDetails principal,
      @PathVariable UUID id,
      @Valid @RequestBody CommentController.EditCommentRequest request) {
    try {
      return response(
          comments.editById(memberId(principal), id, request.version(), request.body()));
    } catch (CommentNotFoundException | CommentPublicationNotFoundException exception) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    } catch (CommentOwnershipException exception) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    } catch (CommentVersionConflictException exception) {
      throw new ResponseStatusException(HttpStatus.CONFLICT);
    }
  }

  @DeleteMapping("/{id}")
  void delete(
      @AuthenticationPrincipal UserDetails principal,
      @PathVariable UUID id,
      @Valid @RequestBody CommentController.DeleteCommentRequest request) {
    try {
      comments.deleteById(memberId(principal), id, request.version());
    } catch (CommentNotFoundException | CommentPublicationNotFoundException exception) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    } catch (CommentOwnershipException exception) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    } catch (CommentVersionConflictException exception) {
      throw new ResponseStatusException(HttpStatus.CONFLICT);
    }
  }

  private static UUID memberId(UserDetails principal) {
    try {
      return UUID.fromString(principal.getUsername());
    } catch (IllegalArgumentException exception) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
    }
  }

  private static CommentController.CommentResponse response(CommentEntity comment) {
    return new CommentController.CommentResponse(
        comment.getId(),
        comment.getPublicationId(),
        comment.getAuthorMemberId(),
        comment.getBody(),
        comment.getLifecycle(),
        comment.getCreatedAt(),
        comment.getUpdatedAt(),
        comment.getVersion());
  }
}
