package com.townpet.care;

import com.townpet.common.PublicSeoProvider;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class CareSeoProvider implements PublicSeoProvider {
  private final CareRequestService requests;

  CareSeoProvider(CareRequestService requests) {
    this.requests = requests;
  }

  @Override
  public String route() {
    return "care";
  }

  @Override
  public Optional<SeoPage> find(UUID id) {
    return requests
        .get(id)
        .filter(request -> request.getStatus() == CareRequestStatus.OPEN)
        .map(
            request ->
                new SeoPage(
                    request.getTitle(),
                    request.getDescription(),
                    request.getDescription(),
                    request.getCreatedAt(),
                    request.getUpdatedAt()));
  }
}
