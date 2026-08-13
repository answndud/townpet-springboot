package com.townpet.media;

import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.http.Method;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("production")
final class MinioObjectStorage implements ObjectStoragePort {
  private final MinioClient client;
  private final MinioClient presignClient;
  private final String bucket;
  private final int expirySeconds;

  MinioObjectStorage(MinioStorageProperties properties) {
    client =
        MinioClient.builder()
            .endpoint(properties.endpoint())
            .credentials(properties.accessKey(), properties.secretKey())
            .build();
    presignClient =
        MinioClient.builder()
            .endpoint(properties.publicEndpoint())
            .credentials(properties.accessKey(), properties.secretKey())
            .build();
    bucket = properties.bucket();
    expirySeconds = properties.presignExpirySeconds();
    try {
      if (!client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
        client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
      }
    } catch (Exception exception) {
      throw new IllegalStateException("Could not initialize MinIO bucket", exception);
    }
  }

  @Override
  public String createUploadUrl(
      String objectKey, String contentType, long byteSize, Instant expiresAt) {
    try {
      return presignClient.getPresignedObjectUrl(
          GetPresignedObjectUrlArgs.builder()
              .method(Method.PUT)
              .bucket(bucket)
              .object(objectKey)
              .expiry(
                  Math.min(
                      expirySeconds,
                      Math.max(
                          1, (int) (expiresAt.getEpochSecond() - Instant.now().getEpochSecond()))),
                  TimeUnit.SECONDS)
              .build());
    } catch (Exception exception) {
      throw new IllegalStateException("Could not create MinIO upload URL", exception);
    }
  }

  @Override
  public Optional<StoredObject> inspect(String objectKey) {
    try {
      var stat =
          client.statObject(StatObjectArgs.builder().bucket(bucket).object(objectKey).build());
      byte[] content;
      try (InputStream input =
          client.getObject(GetObjectArgs.builder().bucket(bucket).object(objectKey).build())) {
        content = input.readAllBytes();
      }
      String contentType = stat.contentType();
      return Optional.of(
          new StoredObject(
              contentType, stat.size(), sha256(content), MediaContentSniffer.detect(content)));
    } catch (io.minio.errors.ErrorResponseException exception) {
      if ("NoSuchKey".equals(exception.errorResponse().code())) return Optional.empty();
      throw new IllegalStateException("Could not inspect MinIO object", exception);
    } catch (Exception exception) {
      throw new IllegalStateException("Could not inspect MinIO object", exception);
    }
  }

  @Override
  public void store(String objectKey, String contentType, byte[] content) {
    try {
      client.putObject(
          PutObjectArgs.builder().bucket(bucket).object(objectKey).contentType(contentType).stream(
                  new java.io.ByteArrayInputStream(content), content.length, -1)
              .build());
    } catch (Exception exception) {
      throw new IllegalStateException("Could not store MinIO object", exception);
    }
  }

  @Override
  public void delete(String objectKey) {
    try {
      client.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(objectKey).build());
    } catch (Exception exception) {
      throw new IllegalStateException("Could not delete MinIO object", exception);
    }
  }

  private static String sha256(byte[] content) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 is required", exception);
    }
  }
}

@org.springframework.boot.context.properties.ConfigurationProperties(prefix = "townpet.media.minio")
record MinioStorageProperties(
    String endpoint,
    String publicEndpoint,
    String accessKey,
    String secretKey,
    String bucket,
    int presignExpirySeconds) {}
