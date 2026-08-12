package com.townpet.publication;

import com.townpet.common.MemberOrAnonymousOnly;
import com.townpet.identity.GuestStepUpController;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/guest/posts")
class GuestPublicationController {
  private final PublicationService publications;

  GuestPublicationController(PublicationService publications) {
    this.publications = publications;
  }

  @PostMapping
  @MemberOrAnonymousOnly
  @ResponseStatus(HttpStatus.CREATED)
  Response create(
      @CookieValue(name = GuestStepUpController.GUEST_COOKIE) UUID guestId,
      @Valid @RequestBody CreateRequest request) {
    return response(
        publications.createGuest(guestId, request.password(), request.title(), request.body()));
  }

  @PatchMapping("/{publicationId}")
  @MemberOrAnonymousOnly
  Response edit(
      @PathVariable UUID publicationId,
      @CookieValue(name = GuestStepUpController.GUEST_COOKIE) UUID guestId,
      @Valid @RequestBody EditRequest request) {
    try {
      return response(
          publications.editGuest(
              guestId,
              request.password(),
              publicationId,
              request.version(),
              request.title(),
              request.body()));
    } catch (PublicationNotFoundException exception) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    } catch (PublicationOwnershipException exception) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    } catch (PublicationVersionConflictException exception) {
      throw new ResponseStatusException(HttpStatus.CONFLICT);
    }
  }

  @DeleteMapping("/{publicationId}")
  @MemberOrAnonymousOnly
  void delete(
      @PathVariable UUID publicationId,
      @CookieValue(name = GuestStepUpController.GUEST_COOKIE) UUID guestId,
      @Valid @RequestBody DeleteRequest request) {
    try {
      publications.deleteGuest(guestId, request.password(), publicationId, request.version());
    } catch (PublicationNotFoundException exception) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    } catch (PublicationOwnershipException exception) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    } catch (PublicationVersionConflictException exception) {
      throw new ResponseStatusException(HttpStatus.CONFLICT);
    }
  }

  private static Response response(PublicationEntity publication) {
    return new Response(
        publication.getId(),
        null,
        publication.getTitle(),
        publication.getBody(),
        publication.getLifecycle(),
        publication.getCreatedAt(),
        publication.getUpdatedAt(),
        publication.getVersion());
  }

  record CreateRequest(
      @NotBlank @Size(min = 8, max = 72) String password,
      @NotBlank @Size(max = 120) String title,
      @NotBlank @Size(max = 20000) String body) {}

  record EditRequest(
      @NotBlank @Size(min = 8, max = 72) String password,
      @NotBlank @Size(max = 120) String title,
      @NotBlank @Size(max = 20000) String body,
      @NotNull @Min(0) Long version) {}

  record DeleteRequest(
      @NotBlank @Size(min = 8, max = 72) String password, @NotNull @Min(0) Long version) {}

  record Response(
      UUID id,
      @Nullable UUID authorId,
      String title,
      String body,
      PublicationLifecycle lifecycle,
      Instant createdAt,
      Instant updatedAt,
      long version) {}
}
