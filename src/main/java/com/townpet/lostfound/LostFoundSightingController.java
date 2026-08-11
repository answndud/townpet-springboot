package com.townpet.lostfound;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
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
@RequestMapping("/api/v1/lost-found")
class LostFoundSightingController {
  private final LostFoundSightingService sightings;
  private final LostFoundExactLocationService exactLocations;

  LostFoundSightingController(
      LostFoundSightingService sightings, LostFoundExactLocationService exactLocations) {
    this.sightings = sightings;
    this.exactLocations = exactLocations;
  }

  @PostMapping("/alerts/{alertId}/sightings")
  @ResponseStatus(HttpStatus.CREATED)
  SightingResponse create(
      @AuthenticationPrincipal UserDetails principal,
      @PathVariable UUID alertId,
      @Valid @RequestBody CreateSightingRequest request) {
    try {
      return toResponse(
          sightings.create(
              memberId(principal),
              alertId,
              request.seenAt(),
              request.description(),
              request.latitude(),
              request.longitude(),
              request.exactLatitude(),
              request.exactLongitude()));
    } catch (LostFoundAlertNotFoundException exception) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    } catch (LostFoundAlertStateException exception) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Alert does not accept sightings");
    }
  }

  @GetMapping("/sightings/{sightingId}")
  SightingResponse get(@PathVariable UUID sightingId) {
    return sightings
        .find(sightingId)
        .map(LostFoundSightingController::toResponse)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
  }

  @GetMapping("/sightings/{sightingId}/exact-location")
  ExactLocationResponse getExactLocation(
      @AuthenticationPrincipal UserDetails principal, @PathVariable UUID sightingId) {
    return exactLocations
        .getForAlertOwner(sightingId, memberId(principal))
        .map(
            location ->
                new ExactLocationResponse(
                    location.sightingId(), location.latitude(), location.longitude()))
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
  }

  private static UUID memberId(UserDetails principal) {
    try {
      return UUID.fromString(principal.getUsername());
    } catch (IllegalArgumentException exception) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid principal");
    }
  }

  private static SightingResponse toResponse(LostFoundSightingService.SightingView sighting) {
    return new SightingResponse(
        sighting.id(),
        sighting.alertId(),
        sighting.reporterMemberId(),
        sighting.seenAt(),
        sighting.description(),
        new ApproximateLocation(sighting.latitude(), sighting.longitude()),
        sighting.createdAt());
  }

  record CreateSightingRequest(
      @NotNull Instant seenAt,
      @NotBlank @Size(max = 2000) String description,
      @DecimalMin("-90.0") @DecimalMax("90.0") double latitude,
      @DecimalMin("-180.0") @DecimalMax("180.0") double longitude,
      @DecimalMin("-90.0") @DecimalMax("90.0") Double exactLatitude,
      @DecimalMin("-180.0") @DecimalMax("180.0") Double exactLongitude) {}

  record SightingResponse(
      UUID id,
      UUID alertId,
      UUID reporterMemberId,
      Instant seenAt,
      String description,
      ApproximateLocation approximateLocation,
      Instant createdAt) {}

  record ApproximateLocation(double latitude, double longitude) {}

  record ExactLocationResponse(UUID sightingId, double latitude, double longitude) {}
}
