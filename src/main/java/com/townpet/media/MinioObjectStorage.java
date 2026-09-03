package com.townpet.media;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PostPolicy;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.http.Method;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("production")
@Primary
final class MinioObjectStorage implements ObjectStoragePort, HealthIndicator {
  private final MinioClient client;
  private final MinioClient presignClient;
  private final String publicEndpoint;
  private final String bucket;
  private final int expirySeconds;
  private final MeterRegistry metrics;

  MinioObjectStorage(MinioStorageProperties properties, MeterRegistry metrics) {
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
    publicEndpoint = properties.publicEndpoint().replaceAll("/$", "");
    bucket = properties.bucket();
    expirySeconds = properties.presignExpirySeconds();
    this.metrics = metrics;
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
    return propertiesEndpoint() + "/" + bucket;
  }

  private String propertiesEndpoint() {
    return publicEndpoint;
  }

  @Override
  public Map<String, String> createUploadFields(
      String objectKey, String contentType, long byteSize, Instant expiresAt) {
    Timer.Sample sample = Timer.start(metrics);
    try {
      PostPolicy policy =
          new PostPolicy(
              bucket,
              ZonedDateTime.ofInstant(
                  expiresAt.isBefore(Instant.now().plusSeconds(expirySeconds))
                      ? expiresAt
                      : Instant.now().plusSeconds(expirySeconds),
                  ZoneOffset.UTC));
      policy.addEqualsCondition("key", objectKey);
      policy.addEqualsCondition("Content-Type", contentType);
      policy.addContentLengthRangeCondition(byteSize, byteSize);
      Map<String, String> fields = client.getPresignedPostFormData(policy);
      sample.stop(operationTimer("create_upload_fields", "success"));
      return fields;
    } catch (Exception exception) {
      sample.stop(operationTimer("create_upload_fields", "failure"));
      throw new IllegalStateException("Could not create MinIO upload form", exception);
    }
  }

  @Override
  public String createReadUrl(String objectKey, Instant expiresAt) {
    Timer.Sample sample = Timer.start(metrics);
    try {
      String url = presignClient.getPresignedObjectUrl(
          GetPresignedObjectUrlArgs.builder()
              .method(Method.GET)
              .bucket(bucket)
              .object(objectKey)
              .expiry(
                  Math.min(
                      expirySeconds,
                      Math.max(
                          1, (int) (expiresAt.getEpochSecond() - Instant.now().getEpochSecond()))),
                  TimeUnit.SECONDS)
              .build());
      sample.stop(operationTimer("create_read_url", "success"));
      return url;
    } catch (Exception exception) {
      sample.stop(operationTimer("create_read_url", "failure"));
      throw new IllegalStateException("Could not create MinIO read URL", exception);
    }
  }

  @Override
  public Optional<StoredObject> inspect(String objectKey) {
    Timer.Sample sample = Timer.start(metrics);
    try {
      var stat =
          client.statObject(StatObjectArgs.builder().bucket(bucket).object(objectKey).build());
      byte[] content;
      try (InputStream input =
          client.getObject(GetObjectArgs.builder().bucket(bucket).object(objectKey).build())) {
        content = input.readAllBytes();
      }
      String contentType = stat.contentType();
      Optional<StoredObject> result = Optional.of(
          new StoredObject(
              contentType,
              stat.size(),
              sha256(content),
              MediaContentSniffer.detect(content),
              MediaImageDimensions.inspect(contentType, content).orElse(null)));
      sample.stop(operationTimer("inspect", "success"));
      return result;
    } catch (io.minio.errors.ErrorResponseException exception) {
      if ("NoSuchKey".equals(exception.errorResponse().code())) {
        sample.stop(operationTimer("inspect", "not_found"));
        return Optional.empty();
      }
      sample.stop(operationTimer("inspect", "failure"));
      throw new IllegalStateException("Could not inspect MinIO object", exception);
    } catch (Exception exception) {
      sample.stop(operationTimer("inspect", "failure"));
      throw new IllegalStateException("Could not inspect MinIO object", exception);
    }
  }

  @Override
  public void store(String objectKey, String contentType, byte[] content) {
    Timer.Sample sample = Timer.start(metrics);
    try {
      client.putObject(
          PutObjectArgs.builder().bucket(bucket).object(objectKey).contentType(contentType).stream(
                  new java.io.ByteArrayInputStream(content), content.length, -1)
              .build());
      sample.stop(operationTimer("store", "success"));
    } catch (Exception exception) {
      sample.stop(operationTimer("store", "failure"));
      throw new IllegalStateException("Could not store MinIO object", exception);
    }
  }

  @Override
  public void delete(String objectKey) {
    Timer.Sample sample = Timer.start(metrics);
    try {
      client.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(objectKey).build());
      sample.stop(operationTimer("delete", "success"));
    } catch (Exception exception) {
      sample.stop(operationTimer("delete", "failure"));
      throw new IllegalStateException("Could not delete MinIO object", exception);
    }
  }

  @Override
  public Health health() {
    try {
      client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
      return Health.up().withDetail("dependency", "minio").build();
    } catch (Exception exception) {
      return Health.down(exception).withDetail("dependency", "minio").build();
    }
  }

  private Timer operationTimer(String operation, String outcome) {
    return metrics.timer(
        "townpet.minio.operation.duration", "operation", operation, "outcome", outcome);
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
