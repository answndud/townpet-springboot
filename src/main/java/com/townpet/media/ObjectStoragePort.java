package com.townpet.media;

import java.time.Instant;
import java.util.Optional;

interface ObjectStoragePort {
  String createUploadUrl(String objectKey, String contentType, long byteSize, Instant expiresAt);

  Optional<StoredObject> inspect(String objectKey);
}

record StoredObject(String contentType, long byteSize, String checksumSha256) {}
