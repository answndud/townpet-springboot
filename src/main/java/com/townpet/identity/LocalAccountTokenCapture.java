package com.townpet.identity;

import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({"local", "test"})
final class LocalAccountTokenCapture implements AccountTokenDelivery {
  private final ConcurrentMap<CaptureKey, String> tokens = new ConcurrentHashMap<>();

  @Override
  public void deliver(AccountTokenPurpose purpose, String recipient, String rawToken) {
    tokens.put(new CaptureKey(purpose, normalize(recipient)), rawToken);
  }

  Optional<String> find(AccountTokenPurpose purpose, String recipient) {
    return Optional.ofNullable(tokens.get(new CaptureKey(purpose, normalize(recipient))));
  }

  void clear() {
    tokens.clear();
  }

  private static String normalize(String recipient) {
    return recipient.trim().toLowerCase(Locale.ROOT);
  }

  private record CaptureKey(AccountTokenPurpose purpose, String recipient) {}
}
