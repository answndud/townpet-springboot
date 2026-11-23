package com.townpet.engagement;

import com.townpet.common.CurrentMember;
import com.townpet.common.MemberOnly;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
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
  @MemberOnly
  CommentController.CommentResponse edit(
      @CurrentMember UUID currentMemberId,
      @PathVariable UUID id,
      @Valid @RequestBody CommentController.EditCommentRequest request) {
    try {
      return response(
          comments.editById(currentMemberId, id, request.version(), request.body()));
    } catch (CommentNotFoundException | CommentPublicationNotFoundException exception) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    } catch (CommentOwnershipException exception) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    } catch (CommentVersionConflictException exception) {
      throw new ResponseStatusException(HttpStatus.CONFLICT);
    }
  }

  @DeleteMapping("/{id}")
  @MemberOnly
  void delete(
      @CurrentMember UUID currentMemberId,
      @PathVariable UUID id,
      @Valid @RequestBody CommentController.DeleteCommentRequest request) {
    try {
      comments.deleteById(currentMemberId, id, request.version());
    } catch (CommentNotFoundException | CommentPublicationNotFoundException exception) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    } catch (CommentOwnershipException exception) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    } catch (CommentVersionConflictException exception) {
      throw new ResponseStatusException(HttpStatus.CONFLICT);
    }
  }

  private static CommentController.CommentResponse response(CommentEntity comment) {
    return new CommentController.CommentResponse(
        comment.getId(),
        comment.getPublicationId(),
        comment.getAuthorMemberId(),
        CommentAuthorType.MEMBER,
        comment.getParentCommentId(),
        comment.getBody(),
        comment.getLifecycle(),
        comment.getCreatedAt(),
        comment.getUpdatedAt(),
        comment.getVersion());
  }
}
