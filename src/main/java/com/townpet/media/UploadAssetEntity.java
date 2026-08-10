package com.townpet.media;

import com.townpet.common.UuidV7;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import org.springframework.lang.Nullable;

@Entity
@Table(name = "upload_asset")
class UploadAssetEntity {
  @Id private UUID id;

  @Column(nullable = false)
  private UUID ownerMemberId;

  @Column(nullable = false, length = 255)
  private String objectKey;

  @Column(nullable = false, length = 64)
  private String checksumSha256;

  @Column(nullable = false, length = 120)
  private String contentType;

  @Column(nullable = false)
  private long byteSize;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private MediaAssetStatus status;

  @Nullable private UUID publicationId;

  @Column(nullable = false)
  private Instant expiresAt;

  @Column(nullable = false)
  private Instant createdAt;

  @Column(nullable = false)
  private Instant updatedAt;

  @Version private long version;

  protected UploadAssetEntity() {}

  UploadAssetEntity(
      UUID ownerMemberId,
      String objectKey,
      String checksumSha256,
      String contentType,
      long byteSize,
      Instant expiresAt) {
    this.id = UuidV7.randomUuid();
    this.ownerMemberId = ownerMemberId;
    this.objectKey = objectKey;
    this.checksumSha256 = checksumSha256;
    this.contentType = contentType;
    this.byteSize = byteSize;
    this.status = MediaAssetStatus.UPLOADING;
    this.expiresAt = expiresAt;
    this.createdAt = Instant.now();
    this.updatedAt = createdAt;
  }

  void finalizeUpload(String checksumSha256, Instant changedAt) {
    if (status != MediaAssetStatus.UPLOADING || !this.checksumSha256.equals(checksumSha256)) {
      throw new MediaAssetStateException();
    }
    status = MediaAssetStatus.READY;
    updatedAt = changedAt;
  }

  void attach(UUID publicationId, Instant changedAt) {
    if (status != MediaAssetStatus.READY) throw new MediaAssetStateException();
    this.publicationId = publicationId;
    status = MediaAssetStatus.ATTACHED;
    updatedAt = changedAt;
  }

  UUID getId() {
    return id;
  }

  UUID getOwnerMemberId() {
    return ownerMemberId;
  }

  String getObjectKey() {
    return objectKey;
  }

  String getChecksumSha256() {
    return checksumSha256;
  }

  String getContentType() {
    return contentType;
  }

  long getByteSize() {
    return byteSize;
  }

  MediaAssetStatus getStatus() {
    return status;
  }

  @Nullable
  UUID getPublicationId() {
    return publicationId;
  }

  Instant getExpiresAt() {
    return expiresAt;
  }

  Instant getCreatedAt() {
    return createdAt;
  }

  Instant getUpdatedAt() {
    return updatedAt;
  }

  long getVersion() {
    return version;
  }
}
