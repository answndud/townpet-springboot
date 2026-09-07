package com.townpet.common.web;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MaintenanceMode {
  private final Path marker;
  private final AtomicInteger activeWrites = new AtomicInteger();

  public MaintenanceMode(
      @Value("${townpet.maintenance.file:/run/townpet/maintenance}") String marker) {
    this.marker = Path.of(marker);
  }

  public boolean enabled() {
    return Files.isRegularFile(marker);
  }

  public int activeWrites() {
    return activeWrites.get();
  }

  public boolean tryEnterWrite() {
    if (enabled()) return false;
    activeWrites.incrementAndGet();
    if (enabled()) {
      activeWrites.decrementAndGet();
      return false;
    }
    return true;
  }

  public void leaveWrite() {
    activeWrites.decrementAndGet();
  }
}
