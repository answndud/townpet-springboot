package com.townpet.welfare;

import com.townpet.common.MemberOnly;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.*;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/hospital-reviews")
@Validated
class HospitalReviewController {
  private final HospitalReviewService service;

  HospitalReviewController(HospitalReviewService s) {
    service = s;
  }

  @GetMapping
  List<Response> list(@RequestParam(required = false) @Size(max = 120) String hospital) {
    return service.list(hospital).stream().map(HospitalReviewController::response).toList();
  }

  @GetMapping("/{id}")
  Response get(@PathVariable UUID id) {
    return service
        .find(id)
        .map(HospitalReviewController::response)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
  }

  @PostMapping
  @MemberOnly
  @ResponseStatus(HttpStatus.CREATED)
  Response create(@AuthenticationPrincipal UserDetails p, @Valid @RequestBody CreateRequest r) {
    try {
      return response(
          service.create(member(p), r.hospitalName(), r.address(), r.rating(), r.body()));
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

  private static Response response(HospitalReviewService.Review r) {
    return new Response(
        r.id(),
        r.authorMemberId(),
        r.hospitalName(),
        r.address(),
        r.rating(),
        r.body(),
        r.createdAt(),
        r.updatedAt(),
        r.version());
  }

  record CreateRequest(
      @NotBlank @Size(max = 160) String hospitalName,
      @NotBlank @Size(max = 240) String address,
      @Min(1) @Max(5) int rating,
      @NotBlank @Size(max = 5000) String body) {}

  record Response(
      UUID id,
      UUID authorMemberId,
      String hospitalName,
      String address,
      int rating,
      String body,
      Instant createdAt,
      Instant updatedAt,
      long version) {}
}
