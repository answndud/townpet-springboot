package com.townpet.media;

import java.time.Instant;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!test & !e2e")
class UnavailableObjectStorage implements ObjectStoragePort {
  @Override
  public String createUploadUrl(
      String objectKey, String contentType, long byteSize, Instant expiresAt) {
    return "";
  }

  @Override
  public Optional<StoredObject> inspect(String objectKey) {
    return Optional.empty();
  }

  @Override
  public void store(String objectKey, String contentType, byte[] content) {
    throw new IllegalStateException("Object storage is not configured");
  }

  @Override
  public void delete(String objectKey) {}
}
