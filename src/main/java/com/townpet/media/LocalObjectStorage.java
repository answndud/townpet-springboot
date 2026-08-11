package com.townpet.media;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({"test", "e2e"})
class LocalObjectStorage implements ObjectStoragePort {
  private final Map<String, StoredObject> objects = new ConcurrentHashMap<>();

  @Override
  public String createUploadUrl(
      String objectKey, String contentType, long byteSize, Instant expiresAt) {
    return "http://local-object-storage.test/" + objectKey;
  }

  @Override
  public Optional<StoredObject> inspect(String objectKey) {
    return Optional.ofNullable(objects.get(objectKey));
  }

  void put(String objectKey, String contentType, byte[] content) {
    objects.put(objectKey, new StoredObject(contentType, content.length, sha256(content)));
  }

  private static String sha256(byte[] content) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 is required", exception);
    }
  }
}
