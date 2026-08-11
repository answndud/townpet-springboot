package com.townpet.welfare;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/adoptions")
public class AdoptionController {
  private final AdoptionService adoptions;

  AdoptionController(AdoptionService adoptions) {
    this.adoptions = adoptions;
  }

  @GetMapping
  List<Response> list(@RequestParam(defaultValue = "20") int limit) {
    if (limit < 1 || limit > 50)
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "limit must be between 1 and 50");
    return adoptions.list(limit).stream().map(AdoptionController::response).toList();
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  Response create(
      @AuthenticationPrincipal UserDetails principal, @Valid @RequestBody CreateRequest request) {
    return response(
        adoptions.create(
            memberId(principal),
            request.title(),
            request.description(),
            request.species(),
            request.breed(),
            request.neighborhoodId()));
  }

  private static UUID memberId(UserDetails principal) {
    try {
      return UUID.fromString(principal.getUsername());
    } catch (IllegalArgumentException exception) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
    }
  }

  private static Response response(AdoptionService.Listing item) {
    return new Response(
        item.id(),
        item.publisherMemberId(),
        item.neighborhoodId(),
        item.title(),
        item.description(),
        item.species(),
        item.breed(),
        item.status(),
        item.createdAt(),
        item.updatedAt(),
        item.version());
  }

  record CreateRequest(
      @NotBlank @Size(max = 120) String title,
      @NotBlank @Size(max = 5000) String description,
      @NotBlank @Size(max = 30) String species,
      @Nullable @Size(max = 80) String breed,
      @Nullable UUID neighborhoodId) {}

  public record Response(
      UUID id,
      UUID publisherMemberId,
      UUID neighborhoodId,
      String title,
      String description,
      String species,
      String breed,
      String status,
      Instant createdAt,
      Instant updatedAt,
      long version) {}
}
