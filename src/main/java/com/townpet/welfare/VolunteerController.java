package com.townpet.welfare;

import com.townpet.common.CurrentMember;
import com.townpet.common.MemberOnly;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.*;
import org.springframework.http.*;
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
  Response create(@CurrentMember UUID currentMemberId, @Valid @RequestBody CreateRequest r) {
    return response(
        service.create(
            currentMemberId,
            r.title(),
            r.description(),
            r.organization(),
            r.location(),
            r.startsAt(),
            r.capacity()));
  }

  @PostMapping("/{id}/applications")
  @MemberOnly
  @ResponseStatus(HttpStatus.CREATED)
  void apply(
      @CurrentMember UUID currentMemberId,
      @PathVariable UUID id,
      @Valid @RequestBody ApplyRequest r) {
    try {
      service.apply(currentMemberId, id, r.message());
    } catch (NoSuchElementException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    } catch (IllegalStateException e) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
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
