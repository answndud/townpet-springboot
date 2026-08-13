package com.townpet.identity;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
@Profile("!local & !test & !e2e")
@ConditionalOnProperty(name = "townpet.email.enabled", havingValue = "false", matchIfMissing = true)
final class UnavailableAccountTokenDelivery implements AccountTokenDelivery {
  @Override
  public void deliver(AccountTokenPurpose purpose, String recipient, String rawToken) {
    throw new ResponseStatusException(
        HttpStatus.SERVICE_UNAVAILABLE, "Account token delivery is not configured");
  }
}
