package com.townpet.common;

import java.util.Map;
import com.townpet.common.web.MaintenanceMode;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
class LegacyHealthController {
  private final MaintenanceMode maintenanceMode;

  LegacyHealthController(MaintenanceMode maintenanceMode) {
    this.maintenanceMode = maintenanceMode;
  }

  @GetMapping
  Map<String, Object> health() {
    return Map.of(
        "status", "UP",
        "maintenance", maintenanceMode.enabled(),
        "acceptingWrites", !maintenanceMode.enabled(),
        "activeWrites", maintenanceMode.activeWrites());
  }
}
