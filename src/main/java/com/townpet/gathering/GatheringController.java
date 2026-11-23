package com.townpet.gathering;

import com.townpet.common.CurrentMember;
import com.townpet.common.MemberOnly;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.*;
import org.springframework.http.*;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.*;

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
  Response get(@PathVariable UUID id, @CurrentMember(required = false) @Nullable UUID currentMemberId) {
    return response(service.get(id, currentMemberId));
  }

  @PostMapping
  @MemberOnly
  @ResponseStatus(HttpStatus.CREATED)
  Response create(
      @CurrentMember UUID currentMemberId, @Valid @RequestBody CreateRequest r) {
    return response(
        service.create(
            currentMemberId,
            r.title(),
            r.description(),
            r.location(),
            r.startsAt(),
            r.capacity()));
  }

  @PostMapping("/{id}/participants")
  @MemberOnly
  Response join(@CurrentMember UUID currentMemberId, @PathVariable UUID id) {
    return response(service.join(id, currentMemberId));
  }

  @DeleteMapping("/{id}/participants/me")
  @MemberOnly
  Response leave(@CurrentMember UUID currentMemberId, @PathVariable UUID id) {
    return response(service.leave(id, currentMemberId));
  }

  @PatchMapping("/{id}/cancel")
  @MemberOnly
  Response cancel(@CurrentMember UUID currentMemberId, @PathVariable UUID id) {
    return response(service.cancel(id, currentMemberId));
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
      @Min(2) @Max(100) int capacity) {}

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
