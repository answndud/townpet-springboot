package com.townpet.identity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
@Profile("!local & !test & !e2e & !delivery-unavailable")
public class AccountTokenDeliveryListener {
  private static final Logger log = LoggerFactory.getLogger(AccountTokenDeliveryListener.class);
  private final AccountTokenDelivery delivery;
  private final AccountTokenCipher cipher;

  public AccountTokenDeliveryListener(AccountTokenDelivery delivery, AccountTokenCipher cipher) {
    this.delivery = delivery;
    this.cipher = cipher;
  }

  @ApplicationModuleListener
  void deliver(AccountTokenDeliveryRequested request) {
    RuntimeException lastFailure = null;
    for (int attempt = 1; attempt <= 3; attempt++) {
      try {
        delivery.deliver(
            request.purpose(), request.recipient(), cipher.decrypt(request.encryptedToken()));
        return;
      } catch (RuntimeException exception) {
        lastFailure = exception;
        log.warn("account_token_delivery_failed purpose={} attempt={}", request.purpose(), attempt);
        if (attempt < 3) {
          try {
            Thread.sleep(50L * attempt);
          } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Account token delivery interrupted", interrupted);
          }
        }
      }
    }
    log.error("account_token_delivery_exhausted purpose={}", request.purpose(), lastFailure);
    throw lastFailure;
  }
}
