package com.townpet.operations;

import jakarta.validation.constraints.Size;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/security/csp-report")
class CspReportController {
  @PostMapping
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void receive(@RequestBody @Size(max = 8192) String payload) {
    // CSP reports are intentionally not persisted; the deployment platform owns retention.
    if (payload.length() > 8192)
      throw new org.springframework.web.server.ResponseStatusException(
          HttpStatus.PAYLOAD_TOO_LARGE);
  }

  @GetMapping
  Map<String, String> health() {
    return Map.of("status", "accepted");
  }
}
