package com.townpet.identity;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
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
  private final MeterRegistry metrics;

  public AccountTokenDeliveryListener(
      AccountTokenDelivery delivery, AccountTokenCipher cipher, MeterRegistry metrics) {
    this.delivery = delivery;
    this.cipher = cipher;
    this.metrics = metrics;
  }

  @ApplicationModuleListener
  void deliver(AccountTokenDeliveryRequested request) {
    Timer.Sample timer = Timer.start(metrics);
    RuntimeException lastFailure = null;
    for (int attempt = 1; attempt <= 3; attempt++) {
      try {
        delivery.deliver(
            request.purpose(), request.recipient(), cipher.decrypt(request.encryptedToken()));
        timer.stop(deliveryTimer(request.purpose(), "success"));
        metrics
            .counter(
                "townpet.account_token_delivery.attempts",
                "purpose",
                request.purpose().name(),
                "outcome",
                "success")
            .increment();
        log.info(
            "event=account_token_delivery outcome=success purpose={} attempt={}",
            request.purpose(),
            attempt);
        return;
      } catch (RuntimeException exception) {
        lastFailure = exception;
        metrics
            .counter(
                "townpet.account_token_delivery.attempts",
                "purpose",
                request.purpose().name(),
                "outcome",
                "failure")
            .increment();
        log.warn(
            "event=account_token_delivery outcome=retryable_failure purpose={} attempt={}",
            request.purpose(),
            attempt);
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
    timer.stop(deliveryTimer(request.purpose(), "exhausted"));
    log.error(
        "event=account_token_delivery outcome=exhausted purpose={}", request.purpose(), lastFailure);
    throw lastFailure;
  }

  private Timer deliveryTimer(AccountTokenPurpose purpose, String outcome) {
    return metrics.timer(
        "townpet.account_token_delivery.duration", "purpose", purpose.name(), "outcome", outcome);
  }
}
