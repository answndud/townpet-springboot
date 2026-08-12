package com.townpet.lostfound;

import com.townpet.common.MemberOnly;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/lost-found/alerts")
class LostFoundAlertController {
  private final LostFoundAlertService alerts;

  LostFoundAlertController(LostFoundAlertService alerts) {
    this.alerts = alerts;
  }

  @PostMapping
  @MemberOnly
  @ResponseStatus(HttpStatus.CREATED)
  AlertResponse create(
      @AuthenticationPrincipal UserDetails principal,
      @Valid @RequestBody CreateAlertRequest request) {
    return toResponse(
        alerts.create(
            memberId(principal),
            request.kind(),
            request.title(),
            request.description(),
            request.lastSeenAt(),
            request.latitude(),
            request.longitude()));
  }

  @GetMapping
  List<AlertResponse> list(
      @org.springframework.web.bind.annotation.RequestParam(required = false)
          LostFoundAlertKind kind,
      @org.springframework.web.bind.annotation.RequestParam(defaultValue = "20") int limit,
      @org.springframework.web.bind.annotation.RequestParam(required = false) Double latitude,
      @org.springframework.web.bind.annotation.RequestParam(required = false) Double longitude,
      @org.springframework.web.bind.annotation.RequestParam(required = false)
          Integer radiusMeters) {
    if (limit < 1 || limit > 50) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "limit must be between 1 and 50");
    }
    boolean anyRadius = latitude != null || longitude != null || radiusMeters != null;
    boolean completeRadius = latitude != null && longitude != null && radiusMeters != null;
    if (anyRadius && !completeRadius) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "radius filter requires all parameters");
    }
    if (completeRadius
        && (latitude < -90
            || latitude > 90
            || longitude < -180
            || longitude > 180
            || radiusMeters < 1
            || radiusMeters > 100_000)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid radius filter");
    }
    return alerts
        .listActive(
            Optional.ofNullable(kind),
            limit,
            Optional.ofNullable(latitude),
            Optional.ofNullable(longitude),
            Optional.ofNullable(radiusMeters))
        .stream()
        .map(LostFoundAlertController::toResponse)
        .toList();
  }

  @GetMapping("/{alertId}")
  AlertResponse get(@PathVariable UUID alertId) {
    return alerts
        .find(alertId)
        .map(LostFoundAlertController::toResponse)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
  }

  @org.springframework.web.bind.annotation.PatchMapping("/{alertId}/status")
  @MemberOnly
  AlertResponse changeStatus(
      @AuthenticationPrincipal UserDetails principal,
      @PathVariable UUID alertId,
      @Valid @RequestBody ChangeStatusRequest request) {
    try {
      return toResponse(
          alerts.changeStatus(
              memberId(principal),
              alertId,
              request.status(),
              request.resolutionOutcome(),
              request.closeReason(),
              request.reopenReason()));
    } catch (LostFoundAlertNotFoundException exception) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    } catch (LostFoundAlertOwnershipException exception) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    } catch (LostFoundAlertStateException exception) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Invalid alert status transition");
    }
  }

  private static UUID memberId(UserDetails principal) {
    try {
      return UUID.fromString(principal.getUsername());
    } catch (IllegalArgumentException exception) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid principal");
    }
  }

  private static AlertResponse toResponse(LostFoundAlertService.AlertView alert) {
    return new AlertResponse(
        alert.id(),
        alert.reporterMemberId(),
        alert.kind(),
        alert.status(),
        alert.title(),
        alert.description(),
        alert.lastSeenAt(),
        new ApproximateLocation(alert.latitude(), alert.longitude()),
        alert.resolutionOutcome(),
        alert.closeReason(),
        alert.createdAt(),
        alert.updatedAt(),
        alert.version());
  }

  record CreateAlertRequest(
      @NotNull LostFoundAlertKind kind,
      @NotBlank @Size(max = 120) String title,
      @NotBlank @Size(max = 5000) String description,
      @NotNull Instant lastSeenAt,
      @DecimalMin("-90.0") @DecimalMax("90.0") double latitude,
      @DecimalMin("-180.0") @DecimalMax("180.0") double longitude) {}

  record ChangeStatusRequest(
      @NotNull LostFoundAlertStatus status,
      @Size(max = 500) String resolutionOutcome,
      @Size(max = 500) String closeReason,
      @Size(max = 500) String reopenReason) {}

  record AlertResponse(
      UUID id,
      UUID reporterMemberId,
      LostFoundAlertKind kind,
      LostFoundAlertStatus status,
      String title,
      String description,
      Instant lastSeenAt,
      ApproximateLocation approximateLocation,
      String resolutionOutcome,
      String closeReason,
      Instant createdAt,
      Instant updatedAt,
      long version) {}

  record ApproximateLocation(double latitude, double longitude) {}
}
