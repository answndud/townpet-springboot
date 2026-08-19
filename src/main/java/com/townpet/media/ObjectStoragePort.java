package com.townpet.media;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.springframework.lang.Nullable;

interface ObjectStoragePort {
  String createUploadUrl(String objectKey, String contentType, long byteSize, Instant expiresAt);

  default Map<String, String> createUploadFields(
      String objectKey, String contentType, long byteSize, Instant expiresAt) {
    return Map.of();
  }

  default String createReadUrl(String objectKey, Instant expiresAt) {
    return createUploadUrl(objectKey, "application/octet-stream", 0, expiresAt);
  }

  Optional<StoredObject> inspect(String objectKey);

  void store(String objectKey, String contentType, byte[] content);

  void delete(String objectKey);
}

record StoredObject(
    String contentType,
    long byteSize,
    String checksumSha256,
    String detectedContentType,
    @Nullable MediaImageDimensions.ImageDimensions imageDimensions) {
  StoredObject(
      String contentType, long byteSize, String checksumSha256, String detectedContentType) {
    this(contentType, byteSize, checksumSha256, detectedContentType, null);
  }
}
