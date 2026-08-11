package com.townpet.discovery;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
class LegacyDiscoveryCompatibilityController {
  @PostMapping("/api/feed/personalization")
  FeedController.FeedResponse personalized(@RequestBody(required = false) Object ignored) {
    return new FeedController.FeedResponse(List.of(), new FeedController.PageInfo(null, false));
  }

  @GetMapping("/api/profile/audience-segments")
  AudienceResponse segments() {
    return new AudienceResponse(List.of("PUBLIC"));
  }

  @PostMapping("/api/lounges/breeds/{breedCode}/groupbuys")
  List<Object> groupBuys() {
    return List.of();
  }

  @PostMapping("/api/guest/step-up")
  ResponseEntity<Void> stepUp(@RequestBody(required = false) Object ignored) {
    return ResponseEntity.noContent().build();
  }

  record AudienceResponse(List<String> segments) {}
}
