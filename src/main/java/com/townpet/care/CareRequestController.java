package com.townpet.care;

import com.townpet.common.CurrentMember;
import com.townpet.common.MemberOnly;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.*;
import org.springframework.http.*;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/care/requests")
class CareRequestController {
  private final CareRequestService requests;

  CareRequestController(CareRequestService requests) {
    this.requests = requests;
  }

  @GetMapping
  List<Response> open() {
    return requests.open().stream().map(CareRequestController::response).toList();
  }

  @GetMapping("/{id}")
  Response get(@PathVariable UUID id) {
    return requests
        .get(id)
        .map(CareRequestController::response)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
  }

  @PostMapping
  @MemberOnly
  ResponseEntity<Response> create(
      @CurrentMember UUID currentMemberId, @Valid @RequestBody CreateRequest request) {
    Response response =
        response(
            requests
                .create(
                    currentMemberId,
                    request.title(),
                    request.description(),
                    request.location(),
                    request.startsAt(),
                    request.endsAt(),
                    request.rewardHint())
                .request());
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @PatchMapping("/{id}/cancel")
  @MemberOnly
  Response cancel(
      @CurrentMember UUID currentMemberId,
      @PathVariable UUID id,
      @Valid @RequestBody VersionRequest request) {
    requests.cancel(currentMemberId, id, request.version());
    return get(id);
  }

  private static Response response(CareRequestEntity r) {
    return new Response(
        r.getId(),
        r.getRequesterMemberId(),
        r.getTitle(),
        r.getDescription(),
        r.getLocation(),
        r.getStartsAt(),
        r.getEndsAt(),
        r.getRewardHint(),
        r.getStatus(),
        r.getCreatedAt(),
        r.getUpdatedAt(),
        r.getVersion());
  }

  record CreateRequest(
      @NotBlank @Size(max = 120) String title,
      @NotBlank @Size(max = 5000) String description,
      @NotBlank @Size(max = 200) String location,
      @NotNull Instant startsAt,
      @NotNull Instant endsAt,
      @Size(max = 200) String rewardHint) {}

  record VersionRequest(@NotNull @Min(0) Long version) {}

  record Response(
      UUID id,
      UUID requesterMemberId,
      String title,
      String description,
      String location,
      Instant startsAt,
      Instant endsAt,
      @Nullable String rewardHint,
      CareRequestStatus status,
      Instant createdAt,
      Instant updatedAt,
      long version) {}
}
