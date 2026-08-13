package com.townpet.identity;

import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Profile("local | test | e2e | delivery-unavailable")
final class LocalAccountTokenDeliveryListener {
  private final AccountTokenDelivery delivery;
  private final AccountTokenCipher cipher;

  LocalAccountTokenDeliveryListener(AccountTokenDelivery delivery, AccountTokenCipher cipher) {
    this.delivery = delivery;
    this.cipher = cipher;
  }

  @EventListener
  void deliver(AccountTokenDeliveryRequested request) {
    delivery.deliver(
        request.purpose(), request.recipient(), cipher.decrypt(request.encryptedToken()));
  }
}
