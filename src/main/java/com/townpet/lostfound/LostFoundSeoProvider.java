package com.townpet.lostfound;

import com.townpet.common.PublicSeoProvider;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class LostFoundSeoProvider implements PublicSeoProvider {
  private final LostFoundAlertService alerts;

  LostFoundSeoProvider(LostFoundAlertService alerts) {
    this.alerts = alerts;
  }

  @Override
  public String route() {
    return "lost-found";
  }

  @Override
  public Optional<SeoPage> find(UUID id) {
    return alerts
        .find(id)
        .filter(alert -> alert.status() == LostFoundAlertStatus.ACTIVE)
        .map(
            alert ->
                new SeoPage(
                    alert.title(),
                    alert.description(),
                    alert.description(),
                    alert.createdAt(),
                    alert.updatedAt()));
  }
}
