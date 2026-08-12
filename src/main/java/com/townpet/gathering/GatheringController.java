package com.townpet.gathering;

import com.townpet.catalog.api.ValidAnimalCommunityCodes;
import com.townpet.common.MemberOnly;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.*;
import org.springframework.http.*;
import org.springframework.lang.Nullable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/gatherings")
class GatheringController {
  private final GatheringService service;

  GatheringController(GatheringService service) {
    this.service = service;
  }

  @GetMapping
  List<Response> list() {
    return service.list().stream().map(GatheringController::response).toList();
  }

  @GetMapping("/{id}")
  Response get(@PathVariable UUID id, @AuthenticationPrincipal @Nullable UserDetails principal) {
    return response(service.get(id, memberId(principal)));
  }

  @PostMapping
  @MemberOnly
  @ResponseStatus(HttpStatus.CREATED)
  Response create(
      @AuthenticationPrincipal UserDetails principal, @Valid @RequestBody CreateRequest r) {
    return response(
        service.create(
            required(principal),
            r.title(),
            r.description(),
            r.location(),
            r.startsAt(),
            r.capacity(),
            r.animalCommunityCodes()));
  }

  @PostMapping("/{id}/participants")
  @MemberOnly
  Response join(@AuthenticationPrincipal UserDetails p, @PathVariable UUID id) {
    return response(service.join(id, required(p)));
  }

  @DeleteMapping("/{id}/participants/me")
  @MemberOnly
  Response leave(@AuthenticationPrincipal UserDetails p, @PathVariable UUID id) {
    return response(service.leave(id, required(p)));
  }

  @PatchMapping("/{id}/cancel")
  @MemberOnly
  Response cancel(@AuthenticationPrincipal UserDetails p, @PathVariable UUID id) {
    return response(service.cancel(id, required(p)));
  }

  private static @Nullable UUID memberId(@Nullable UserDetails p) {
    if (p == null) return null;
    try {
      return UUID.fromString(p.getUsername());
    } catch (Exception e) {
      return null;
    }
  }

  private static UUID required(UserDetails p) {
    UUID id = memberId(p);
    if (id == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
    return id;
  }

  private static Response response(GatheringService.GatheringView g) {
    return new Response(
        g.id(),
        g.hostMemberId(),
        g.title(),
        g.description(),
        g.location(),
        g.startsAt(),
        g.capacity(),
        g.participantCount(),
        g.status(),
        g.joined(),
        g.version());
  }

  record CreateRequest(
      @NotBlank @Size(max = 160) String title,
      @NotBlank @Size(max = 5000) String description,
      @NotBlank @Size(max = 200) String location,
      @NotNull Instant startsAt,
      @Min(2) @Max(100) int capacity,
      @Nullable @Size(max = 12) @ValidAnimalCommunityCodes
          Collection<@Size(max = 40) String> animalCommunityCodes) {}

  record Response(
      UUID id,
      UUID hostMemberId,
      String title,
      String description,
      String location,
      Instant startsAt,
      int capacity,
      int participantCount,
      GatheringStatus status,
      boolean joined,
      long version) {}
}
