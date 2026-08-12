package com.townpet.welfare;

import com.townpet.common.MemberOnly;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.*;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/volunteer")
class VolunteerController {
  private final VolunteerService service;

  VolunteerController(VolunteerService s) {
    service = s;
  }

  @GetMapping
  List<Response> list() {
    return service.list().stream().map(VolunteerController::response).toList();
  }

  @GetMapping("/{id}")
  Response get(@PathVariable UUID id) {
    return service
        .find(id)
        .map(VolunteerController::response)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
  }

  @PostMapping
  @MemberOnly
  @ResponseStatus(HttpStatus.CREATED)
  Response create(@AuthenticationPrincipal UserDetails p, @Valid @RequestBody CreateRequest r) {
    try {
      return response(
          service.create(
              member(p),
              r.title(),
              r.description(),
              r.organization(),
              r.location(),
              r.startsAt(),
              r.capacity()));
    } catch (Exception e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
    }
  }

  @PostMapping("/{id}/applications")
  @MemberOnly
  @ResponseStatus(HttpStatus.CREATED)
  void apply(
      @AuthenticationPrincipal UserDetails p,
      @PathVariable UUID id,
      @Valid @RequestBody ApplyRequest r) {
    try {
      service.apply(member(p), id, r.message());
    } catch (NoSuchElementException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    } catch (IllegalStateException e) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
    }
  }

  private static UUID member(UserDetails p) {
    try {
      return UUID.fromString(p.getUsername());
    } catch (Exception e) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
    }
  }

  private static Response response(VolunteerService.Opportunity o) {
    return new Response(
        o.id(),
        o.publisherMemberId(),
        o.title(),
        o.description(),
        o.organization(),
        o.location(),
        o.startsAt(),
        o.capacity(),
        o.status(),
        o.createdAt(),
        o.updatedAt(),
        o.version());
  }

  record CreateRequest(
      @NotBlank @Size(max = 120) String title,
      @NotBlank @Size(max = 5000) String description,
      @NotBlank @Size(max = 160) String organization,
      @NotBlank @Size(max = 200) String location,
      @NotNull Instant startsAt,
      @Min(1) @Max(100) int capacity) {}

  record ApplyRequest(@NotBlank @Size(max = 1000) String message) {}

  record Response(
      UUID id,
      UUID publisherMemberId,
      String title,
      String description,
      String organization,
      String location,
      Instant startsAt,
      int capacity,
      String status,
      Instant createdAt,
      Instant updatedAt,
      long version) {}
}
